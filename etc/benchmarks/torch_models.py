"""What the Tiny MNIST PyTorch replicas share: loading the data, the multilayer perceptron matched to MnistBenchmark.kt,
the CNN matched to CnnBenchmark.kt, and one epoch of mini-batch training. Each model is built in the same order as
before the scripts shared it, so a seed gives the same initial weights."""
import os
import numpy as np
import torch
from torch import nn

MLP_BATCH = 35
CNN_BATCH = 32


def load_tiny_mnist(data_dir, dtype, images=False):
    """Training and test inputs and class labels; with images, the inputs as 1 x 20 x 20 images."""
    load = lambda n: torch.tensor(np.loadtxt(os.path.join(data_dir, n), delimiter=","), dtype=dtype)
    Xtr, Ytr = load("tiny_mnist_train_inputs.csv"), load("tiny_mnist_train_labels.csv").argmax(1)
    Xte, Yte = load("tiny_mnist_test_inputs.csv"), load("tiny_mnist_test_labels.csv").argmax(1)
    if images:
        Xtr, Xte = Xtr.reshape(-1, 1, 20, 20), Xte.reshape(-1, 1, 20, 20)
    return Xtr, Ytr, Xte, Yte


def mlp(sizes, dtype):
    """Linear layers of the given sizes with ReLU between them and Xavier-uniform weights."""
    layers = []
    for i in range(len(sizes) - 1):
        lin = nn.Linear(sizes[i], sizes[i + 1])
        nn.init.xavier_uniform_(lin.weight)
        layers += [lin] + ([nn.ReLU()] if i < len(sizes) - 2 else [])
    return nn.Sequential(*layers).to(dtype)


def cnn(dtype):
    """20x20x1 -> conv3x3x5 SAME ReLU -> maxpool2 -> conv3x3x8 SAME ReLU -> maxpool2 -> flatten 200 -> dense 10, He
    init with zero biases."""
    model = nn.Sequential(
        nn.Conv2d(1, 5, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
        nn.Conv2d(5, 8, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
        nn.Flatten(), nn.Linear(200, 10),
    ).to(dtype)
    for m in model.modules():
        if isinstance(m, (nn.Conv2d, nn.Linear)):
            nn.init.kaiming_normal_(m.weight, nonlinearity="relu")
            nn.init.zeros_(m.bias)
    return model


def train_epoch(model, opt, loss_fn, X, Y, batch, perm):
    """One pass over X in the order perm, one optimizer step per batch."""
    for s in range(0, len(X), batch):
        idx = perm[s:s + batch]
        opt.zero_grad()
        loss_fn(model(X[idx]), Y[idx]).backward()
        opt.step()
