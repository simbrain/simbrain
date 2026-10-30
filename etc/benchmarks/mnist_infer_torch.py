"""Tiny MNIST inference in PyTorch, matched to MnistInferenceBenchmark.kt. Builds the MLP of mnist_torch.py
(400-150-100-10) or the CNN of cnn_torch.py, trains briefly so the accuracy check means something, then times passes
over the test set in eval mode under torch.inference_mode:

- batch1: one example per call, as Simbrain runs these networks
- full: the whole test set in one call

Both include the output softmax, which the Simbrain networks compute in their output layer.

Usage: mnist_infer_torch.py <mlp|cnn> dataDir trainEpochs passes seed threads dtype[float32|float64]
"""
import json, sys, time
import torch
from torch import nn
from torch_models import CNN_BATCH, MLP_BATCH, cnn, load_tiny_mnist, mlp, train_epoch

flavor, data_dir = sys.argv[1], sys.argv[2]
train_epochs, passes, seed, threads, dtype = int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5]), int(sys.argv[6]), sys.argv[7]
assert flavor in ("mlp", "cnn")
torch.set_num_threads(threads)
torch.manual_seed(seed)
dt = getattr(torch, dtype)
WARMUP_PASSES = 3

Xtr, Ytr, Xte, Yte = load_tiny_mnist(data_dir, dt, images=flavor == "cnn")
model, batch = (mlp([400, 150, 100, 10], dt), MLP_BATCH) if flavor == "mlp" else (cnn(dt), CNN_BATCH)
opt = torch.optim.Adam(model.parameters(), lr=1e-3)
loss_fn = nn.CrossEntropyLoss()
for _ in range(train_epochs):
    train_epoch(model, opt, loss_fn, Xtr, Ytr, batch, torch.randperm(len(Xtr)))
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
    print(json.dumps({"framework": f"pytorch-{torch.__version__}-{threads}t-{dtype}", "version": torch.__version__,
                      "threads": threads, "dtype": dtype, "flavor": flavor, "mode": mode, "seed": seed,
                      "batch": 1 if mode == "batch1" else n, "examples": n, "passes": passes,
                      "pass_s": [round(r[0], 5) for r in results], "us_per_example": round(median / n * 1e6, 2),
                      "examples_per_s": round(n / median), "test_acc": round(results[-1][1] / n, 4)}))
