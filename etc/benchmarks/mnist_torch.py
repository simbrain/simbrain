"""Tiny MNIST in PyTorch, matched to MnistBenchmark.kt (400-150-100-10 ReLU, softmax/CE, Adam 1e-3, batch 35,
Xavier-uniform weights). Usage: mnist_torch.py dataDir epochs seed threads dtype[float32|float64] [hidden e.g. 150,100]
"""
import json, os, sys, time
import numpy as np
import torch
from torch import nn

data_dir, epochs, seed, threads, dtype = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
hidden = [int(h) for h in (sys.argv[6] if len(sys.argv) > 6 else "150,100").split(",")]
torch.set_num_threads(threads)
torch.manual_seed(seed)
dt = getattr(torch, dtype)

load = lambda n: torch.tensor(np.loadtxt(os.path.join(data_dir, n), delimiter=","), dtype=dt)
Xtr, Ytr = load("tiny_mnist_train_inputs.csv"), load("tiny_mnist_train_labels.csv").argmax(1)
Xte, Yte = load("tiny_mnist_test_inputs.csv"), load("tiny_mnist_test_labels.csv").argmax(1)

sizes = [Xtr.shape[1]] + hidden + [10]
layers = []
for i in range(len(sizes) - 1):
    lin = nn.Linear(sizes[i], sizes[i + 1])
    nn.init.xavier_uniform_(lin.weight)
    layers += [lin] + ([nn.ReLU()] if i < len(sizes) - 2 else [])
model = nn.Sequential(*layers).to(dt)
opt = torch.optim.Adam(model.parameters(), lr=1e-3)
loss_fn = nn.CrossEntropyLoss()
batch = 35

epoch_s = []
for _ in range(epochs):
    perm = torch.randperm(len(Xtr))
    t0 = time.perf_counter()
    for s in range(0, len(Xtr), batch):
        idx = perm[s:s + batch]
        opt.zero_grad()
        loss_fn(model(Xtr[idx]), Ytr[idx]).backward()
        opt.step()
    epoch_s.append(round(time.perf_counter() - t0, 3))
with torch.no_grad():
    acc = (model(Xte).argmax(1) == Yte).double().mean().item()
print(json.dumps({"framework": f"pytorch-{torch.__version__}-{threads}t-{dtype}", "arch": "-".join(map(str, sizes)),
                  "batch": batch, "epochs": epochs, "epoch_s": epoch_s, "test_acc": round(acc, 4)}))
