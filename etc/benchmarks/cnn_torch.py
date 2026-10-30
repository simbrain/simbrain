"""Tiny MNIST CNN in PyTorch, matched to CnnBenchmark.kt / cnnMNIST: 20x20x1 -> conv3x3x5 SAME ReLU -> maxpool2
-> conv3x3x8 SAME ReLU -> maxpool2 -> flatten 200 -> dense 10, softmax CE, Adam 1e-3, batch 32, He init.
Usage: cnn_torch.py dataDir epochs seed threads dtype
"""
import json, sys, time
import torch
from torch import nn
from torch_models import CNN_BATCH, cnn, load_tiny_mnist, train_epoch

data_dir, epochs, seed, threads, dtype = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
torch.set_num_threads(threads)
torch.manual_seed(seed)
dt = getattr(torch, dtype)

Xtr, Ytr, Xte, Yte = load_tiny_mnist(data_dir, dt, images=True)
model = cnn(dt)
opt = torch.optim.Adam(model.parameters(), lr=1e-3)
loss_fn = nn.CrossEntropyLoss()

epoch_s = []
for _ in range(epochs):
    perm = torch.randperm(len(Xtr))
    t0 = time.perf_counter()
    train_epoch(model, opt, loss_fn, Xtr, Ytr, CNN_BATCH, perm)
    epoch_s.append(round(time.perf_counter() - t0, 3))
with torch.no_grad():
    acc = (model(Xte).argmax(1) == Yte).double().mean().item()
print(json.dumps({"framework": f"pytorch-cnn-{threads}t-{dtype}", "version": torch.__version__, "threads": threads,
                  "dtype": dtype, "batch": CNN_BATCH, "epochs": epochs, "seed": seed, "epoch_s": epoch_s,
                  "test_acc": round(acc, 4)}))
