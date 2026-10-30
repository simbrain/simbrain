"""Tiny MNIST in PyTorch, matched to MnistBenchmark.kt (400-150-100-10 ReLU, softmax/CE, Adam 1e-3, batch 35,
Xavier-uniform weights). Usage: mnist_torch.py dataDir epochs seed threads dtype[float32|float64] [hidden e.g. 150,100]
"""
import json, sys, time
import torch
from torch import nn
from torch_models import MLP_BATCH, load_tiny_mnist, mlp, train_epoch

data_dir, epochs, seed, threads, dtype = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
hidden = [int(h) for h in (sys.argv[6] if len(sys.argv) > 6 else "150,100").split(",")]
torch.set_num_threads(threads)
torch.manual_seed(seed)
dt = getattr(torch, dtype)

Xtr, Ytr, Xte, Yte = load_tiny_mnist(data_dir, dt)
sizes = [Xtr.shape[1]] + hidden + [10]
model = mlp(sizes, dt)
opt = torch.optim.Adam(model.parameters(), lr=1e-3)
loss_fn = nn.CrossEntropyLoss()

epoch_s = []
for _ in range(epochs):
    perm = torch.randperm(len(Xtr))
    t0 = time.perf_counter()
    train_epoch(model, opt, loss_fn, Xtr, Ytr, MLP_BATCH, perm)
    epoch_s.append(round(time.perf_counter() - t0, 3))
with torch.no_grad():
    acc = (model(Xte).argmax(1) == Yte).double().mean().item()
print(json.dumps({"framework": f"pytorch-{torch.__version__}-{threads}t-{dtype}", "version": torch.__version__,
                  "threads": threads, "dtype": dtype, "arch": "-".join(map(str, sizes)), "batch": MLP_BATCH,
                  "epochs": epochs, "seed": seed, "epoch_s": epoch_s, "test_acc": round(acc, 4)}))
