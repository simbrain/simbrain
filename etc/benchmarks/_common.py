"""What the suite runners and summarizers share: running one harness process and recording its JSON lines, the meta
row that says what was measured, the parameters several suites must agree on, and reading result files with later
files superseding earlier ones."""
import json, os, platform, statistics as st, subprocess, sys, time
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent

# The Izhikevich network's time step in ms, in every simulator
DT = "0.5"
# Tiny language model sizes as contextSize, embeddingDimension, numHeads, hiddenSize, numLayers; the first is the demo's
TINY_LM_CONFIGS = [("24", "20", "4", "30", "1"), ("64", "128", "4", "512", "2")]


def sweep_scale(n, p):
    """The weight scale 100 / in-degree, which keeps firing rates comparable across network sizes."""
    return round(100.0 / ((int(n) - 1) * float(p)), 6)


def tinylm_ids_path(results_dir, config):
    """Where the training suite writes the token ids that the PyTorch tiny language model trains on."""
    return os.path.join(results_dir, f"tinylm_ids_{'_'.join(config)}.txt")


def append_row(out, row):
    with open(out, "a") as f:
        f.write(json.dumps(row) + "\n")
    print(json.dumps(row), flush=True)


def run_and_record(cmd, extra, out, machine, timeout, cwd=None, env=None, last_line_only=False):
    """Runs one harness process and appends each JSON line it prints to out, with extra, the machine, the time and the
    process's wall time added. A crash or timeout is recorded as an error row, so a sweep goes on past it."""
    started = time.time()
    try:
        proc = subprocess.run([str(c) for c in cmd], cwd=cwd, env={**os.environ, **(env or {})}, capture_output=True,
                              text=True, timeout=timeout, stdin=subprocess.DEVNULL)
        # A harness may print one result line per mode
        lines = [l for l in proc.stdout.splitlines() if l.startswith("{")]
        if last_line_only:
            lines = lines[-1:]
        rows = [_parse(l) for l in lines] or [{"error": (proc.stdout + proc.stderr)[-800:]}]
    except subprocess.TimeoutExpired:
        rows = [{"error": f"timeout after {timeout} s"}]
    finished = {"machine": machine, "wall_total_s": round(time.time() - started, 2),
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"), "loadavg": os.getloadavg()[0]}
    for row in rows:
        row.update(extra)
        row.update(finished)
        append_row(out, row)


def _parse(line):
    try:
        return json.loads(line)
    except json.JSONDecodeError:
        return {"error": line[-500:]}


def _git(directory, *args):
    result = subprocess.run(["git", *args], cwd=directory, capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else None


def record_meta(out, machine, s3dir=None, **fields):
    """Appends the meta row: the machine, the command line, and the Simbrain 4 commit (and whether tracked files had
    uncommitted changes) and, given its worktree, the Simbrain 3 commit that the runs measured."""
    row = {"suite": "meta", "platform": platform.platform(), "processor": platform.processor(), "cores": os.cpu_count(),
           "python": sys.version.split()[0], "argv": sys.argv,
           "simbrain4_commit": _git(REPO, "rev-parse", "HEAD"),
           "simbrain4_dirty": bool(_git(REPO, "status", "--porcelain", "--untracked-files=no"))}
    if s3dir:
        row["simbrain3_commit"] = _git(s3dir, "rev-parse", "HEAD")
    row.update(fields)
    row.update({"machine": machine, "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S")})
    append_row(out, row)


def load_latest(paths, measurement):
    """The rows of result files given in time order, where a later file supersedes earlier ones for the same
    measurement (and meta rows for the same machine), so a rerun replaces its old rows rather than pooling with them.
    Returns the rows and, apart, the rows recording an error (a crash or timeout)."""
    by_file = [[json.loads(l) for l in open(p) if l.strip()] for p in paths]

    def key(r):
        return ("meta", r["machine"]) if r.get("suite") == "meta" else measurement(r)

    errors = [r for rows in by_file for r in rows if "error" in r]
    latest = {}
    for index, rows in enumerate(by_file):
        for r in rows:
            if "error" not in r:
                latest[key(r)] = index
    rows = [r for index, rows in enumerate(by_file) for r in rows if "error" not in r and latest[key(r)] == index]
    return rows, errors


def med(values):
    """The median, ignoring missing values; NaN when there are none."""
    values = [v for v in values if v is not None]
    return st.median(values) if values else float("nan")


def steady(epochs):
    # first epoch includes JIT warm-up (Simbrain) or allocator warm-up (PyTorch)
    return med(epochs[1:] if len(epochs) > 1 else epochs)
