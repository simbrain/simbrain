"""Tiny MNIST inference in PyTorch, matched to MnistInferenceBenchmark.kt. Builds the MLP of mnist_torch.py
(400-150-100-10) or the CNN of cnn_torch.py, trains briefly so the accuracy check means something, then times passes
over the test set in eval mode under torch.inference_mode:

- batch1: one example per call, as Simbrain runs these networks
- full: the whole test set in one call

Both include the output softmax, which the Simbrain networks compute in their output layer.

Usage: mnist_infer_torch.py <mlp|cnn> dataDir trainEpochs passes seed threads dtype[float32|float64]
"""
import json, os, sys, time
import numpy as np
import torch
from torch import nn

flavor, data_dir = sys.argv[1], sys.argv[2]
train_epochs, passes, seed, threads, dtype = int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5]), int(sys.argv[6]), sys.argv[7]
assert flavor in ("mlp", "cnn")
torch.set_num_threads(threads)
torch.manual_seed(seed)
dt = getattr(torch, dtype)
WARMUP_PASSES = 3

load = lambda n: torch.tensor(np.loadtxt(os.path.join(data_dir, n), delimiter=","), dtype=dt)
Xtr, Ytr = load("tiny_mnist_train_inputs.csv"), load("tiny_mnist_train_labels.csv").argmax(1)
Xte, Yte = load("tiny_mnist_test_inputs.csv"), load("tiny_mnist_test_labels.csv").argmax(1)

if flavor == "mlp":
    sizes, layers = [400, 150, 100, 10], []
    for i in range(len(sizes) - 1):
        lin = nn.Linear(sizes[i], sizes[i + 1])
        nn.init.xavier_uniform_(lin.weight)
        layers += [lin] + ([nn.ReLU()] if i < len(sizes) - 2 else [])
    model, batch = nn.Sequential(*layers).to(dt), 35
else:
    Xtr, Xte = Xtr.reshape(-1, 1, 20, 20), Xte.reshape(-1, 1, 20, 20)
    model = nn.Sequential(
        nn.Conv2d(1, 5, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
        nn.Conv2d(5, 8, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
        nn.Flatten(), nn.Linear(200, 10),
    ).to(dt)
    for m in model.modules():
        if isinstance(m, (nn.Conv2d, nn.Linear)):
            nn.init.kaiming_normal_(m.weight, nonlinearity="relu")
            nn.init.zeros_(m.bias)
    batch = 32

opt = torch.optim.Adam(model.parameters(), lr=1e-3)
loss_fn = nn.CrossEntropyLoss()
for _ in range(train_epochs):
    perm = torch.randperm(len(Xtr))
    for s in range(0, len(Xtr), batch):
        idx = perm[s:s + batch]
        opt.zero_grad()
        loss_fn(model(Xtr[idx]), Ytr[idx]).backward()
        opt.step()
model.eval()

# The single-example inputs and the labels are prepared up front, as Simbrain's are, so the loop times only the
# forward pass rather than per-example tensor indexing
singles = [Xte[i:i + 1].contiguous() for i in range(len(Xte))]
labels = Yte.tolist()


def batch1_pass():
    correct = 0
    t0 = time.perf_counter()
    with torch.inference_mode():
        for i, x in enumerate(singles):
            correct += int(torch.softmax(model(x), 1).argmax(1).item() == labels[i])
    return time.perf_counter() - t0, correct


def full_pass():
    t0 = time.perf_counter()
    with torch.inference_mode():
        correct = int((torch.softmax(model(Xte), 1).argmax(1) == Yte).sum().item())
    return time.perf_counter() - t0, correct


n = len(Xte)
for mode, run_pass in [("batch1", batch1_pass), ("full", full_pass)]:
    for _ in range(WARMUP_PASSES):
        run_pass()
    results = [run_pass() for _ in range(passes)]
    median = sorted(r[0] for r in results)[passes // 2]
    print(json.dumps({"framework": f"pytorch-{torch.__version__}-{threads}t-{dtype}", "flavor": flavor, "mode": mode,
                      "batch": 1 if mode == "batch1" else n, "examples": n, "passes": passes,
                      "pass_s": [round(r[0], 5) for r in results], "us_per_example": round(median / n * 1e6, 2),
                      "examples_per_s": round(n / median), "test_acc": round(results[-1][1] / n, 4)}))
