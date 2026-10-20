"""Tiny language model in PyTorch, matched to TinyLmBenchmark.kt / TinyLmModel: learned token + position embeddings,
pre-LayerNorm blocks (bias-free Q/K/V/O, causal scaled attention, ReLU MLP with biases), final LayerNorm, bias-free
unembedding, mean cross-entropy over positions, Adam 1e-3, float32, uniform(+-1/sqrt(fan_in)) init. Trains on the
token ids written by TinyLmBenchmark.kt, windows in corpus order.
Usage: tinylm_torch.py idsFile epochs threads batch contextSize embedDim numHeads hiddenDim numLayers
"""
import json, math, sys, time
import torch
from torch import nn
import torch.nn.functional as F

ids_file, epochs, threads, batch = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
ctx, dim, heads, hidden, layers = (int(a) for a in sys.argv[5:10])
torch.set_num_threads(threads)
torch.manual_seed(42)
with open(ids_file) as f:
    vocab = int(f.readline())
    ids = torch.tensor([int(t) for t in f.readline().split()])
windows = torch.stack([ids[s:s + ctx + 1] for s in range(len(ids) - ctx)])
inputs, targets = windows[:, :-1], windows[:, 1:]


def uniform_(t, fan_in):
    b = 1 / math.sqrt(fan_in)
    nn.init.uniform_(t, -b, b)


class Block(nn.Module):
    def __init__(self):
        super().__init__()
        self.ln1, self.ln2 = nn.LayerNorm(dim, eps=1e-5), nn.LayerNorm(dim, eps=1e-5)
        self.q, self.k, self.v, self.o = (nn.Linear(dim, dim, bias=False) for _ in range(4))
        self.up, self.down = nn.Linear(dim, hidden), nn.Linear(hidden, dim)
        nn.init.zeros_(self.up.bias)
        nn.init.zeros_(self.down.bias)

    def forward(self, x):
        b, t, _ = x.shape
        h = self.ln1(x)
        split = lambda y: y.view(b, t, heads, dim // heads).transpose(1, 2)
        a = F.scaled_dot_product_attention(split(self.q(h)), split(self.k(h)), split(self.v(h)), is_causal=True)
        x = x + self.o(a.transpose(1, 2).reshape(b, t, dim))
        return x + self.down(F.relu(self.up(self.ln2(x))))


class TinyLm(nn.Module):
    def __init__(self):
        super().__init__()
        self.embed = nn.Parameter(torch.empty(vocab, dim))
        self.pos = nn.Parameter(torch.empty(ctx, dim))
        uniform_(self.embed, dim)
        uniform_(self.pos, dim)
        self.blocks = nn.ModuleList(Block() for _ in range(layers))
        self.ln = nn.LayerNorm(dim, eps=1e-5)
        self.unembed = nn.Linear(dim, vocab, bias=False)

    def forward(self, tok):
        x = self.embed[tok] + self.pos[: tok.shape[1]]
        for blk in self.blocks:
            x = blk(x)
        return self.unembed(self.ln(x))


model = TinyLm()
opt = torch.optim.Adam(model.parameters(), lr=1e-3)
epoch_s, last = [], 0.0
for _ in range(epochs):
    t0, total, n = time.perf_counter(), 0.0, 0
    for s in range(0, len(inputs), batch):
        x, y = inputs[s:s + batch], targets[s:s + batch]
        opt.zero_grad()
        loss = F.cross_entropy(model(x).reshape(-1, vocab), y.reshape(-1))
        loss.backward()
        opt.step()
        total += loss.item() * len(x)
        n += len(x)
    epoch_s.append(round(time.perf_counter() - t0, 3))
    last = total / n
print(json.dumps({"framework": f"pytorch-tinylm-{threads}t-b{batch}", "vocab": vocab, "windows": len(inputs),
                  "config": [ctx, dim, heads, hidden, layers], "epochs": epochs, "epoch_s": epoch_s,
                  "final_train_loss": round(last, 4)}))
