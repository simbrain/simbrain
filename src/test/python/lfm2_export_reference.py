"""Exports LFM2.5-230M reference activations for the Kotlin parity test (Lfm2ParityTest).

Loads the model in float32 (same bf16 widening the Kotlin loader does), runs fixed prompts,
and writes per-layer hidden states plus logits as raw little-endian f32 binaries with a JSON
manifest, to ~/.cache/simbrain/lfm2-parity/.

Usage: python3 src/test/python/lfm2_export_reference.py
"""

import json
from pathlib import Path

import numpy as np
import torch
from huggingface_hub import hf_hub_download
from tokenizers import Tokenizer
from transformers import AutoModelForCausalLM

MODEL_ID = "LiquidAI/LFM2.5-230M"
OUT_DIR = Path.home() / ".cache" / "simbrain" / "lfm2-parity"

PROMPTS = [
    "The quick brown fox jumps over the lazy dog.",
    "Neural networks learn representations, and probes read them out.",
]


def main():
    torch.manual_seed(0)
    torch.set_num_threads(1)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    tokenizer = Tokenizer.from_file(hf_hub_download(MODEL_ID, "tokenizer.json"))
    model = AutoModelForCausalLM.from_pretrained(MODEL_ID, torch_dtype=torch.float32)
    model.eval()

    theta = getattr(model.config, "rope_theta", None)
    if theta is None:
        theta = model.config.rope_parameters["rope_theta"]
    assert theta == 1_000_000.0, f"unexpected rope theta {theta}"
    assert model.config.norm_eps == 1e-5

    manifest = {"model": MODEL_ID, "rope_theta": theta, "prompts": []}
    for pi, prompt in enumerate(PROMPTS):
        ids = torch.tensor([tokenizer.encode(prompt).ids])
        with torch.no_grad():
            out = model(ids, output_hidden_states=True, use_cache=False)
        seq = ids.shape[1]
        entry = {
            "prompt": prompt,
            "token_ids": ids[0].tolist(),
            "num_hidden_states": len(out.hidden_states),
            "hidden_size": out.hidden_states[0].shape[-1],
            "vocab_size": out.logits.shape[-1],
            "files": {},
        }
        for li, hs in enumerate(out.hidden_states):
            name = f"p{pi}_hidden{li}.bin"
            hs[0].to(torch.float32).numpy().astype("<f4").tofile(OUT_DIR / name)
            entry["files"][f"hidden{li}"] = name
        logits_name = f"p{pi}_logits.bin"
        out.logits[0].to(torch.float32).numpy().astype("<f4").tofile(OUT_DIR / logits_name)
        entry["files"]["logits"] = logits_name
        manifest["prompts"].append(entry)
        print(f"prompt {pi}: {seq} tokens, {len(out.hidden_states)} hidden states")

    (OUT_DIR / "manifest.json").write_text(json.dumps(manifest, indent=2))
    print(f"wrote {OUT_DIR}")


if __name__ == "__main__":
    main()
