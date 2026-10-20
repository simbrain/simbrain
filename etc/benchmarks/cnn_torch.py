"""Tiny MNIST CNN in PyTorch, matched to CnnBenchmark.kt / cnnMNIST: 20x20x1 -> conv3x3x5 SAME ReLU -> maxpool2
-> conv3x3x8 SAME ReLU -> maxpool2 -> flatten 200 -> dense 10, softmax CE, Adam 1e-3, batch 32, He init.
Usage: cnn_torch.py dataDir epochs seed threads dtype
"""
import json, os, sys, time
import numpy as np
import torch
from torch import nn

data_dir, epochs, seed, threads, dtype = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
torch.set_num_threads(threads)
torch.manual_seed(seed)
dt = getattr(torch, dtype)

load = lambda n: torch.tensor(np.loadtxt(os.path.join(data_dir, n), delimiter=","), dtype=dt)
Xtr = load("tiny_mnist_train_inputs.csv").reshape(-1, 1, 20, 20)
Ytr = load("tiny_mnist_train_labels.csv").argmax(1)
Xte = load("tiny_mnist_test_inputs.csv").reshape(-1, 1, 20, 20)
Yte = load("tiny_mnist_test_labels.csv").argmax(1)

model = nn.Sequential(
    nn.Conv2d(1, 5, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
    nn.Conv2d(5, 8, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
    nn.Flatten(), nn.Linear(200, 10),
).to(dt)
for m in model.modules():
    if isinstance(m, (nn.Conv2d, nn.Linear)):
        nn.init.kaiming_normal_(m.weight, nonlinearity="relu")
        nn.init.zeros_(m.bias)
opt = torch.optim.Adam(model.parameters(), lr=1e-3)
loss_fn = nn.CrossEntropyLoss()
batch = 32

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
print(json.dumps({"framework": f"pytorch-cnn-{threads}t-{dtype}", "batch": batch, "epochs": epochs,
                  "epoch_s": epoch_s, "test_acc": round(acc, 4)}))
