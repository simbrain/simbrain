"""Sets up and runs the Simbrain 4 performance benchmarks. Run it through the `bench` wrapper next to it, which runs it
in this directory's uv environment, so PyTorch and Brian2 come from uv.lock.

  bench setup --java21 JDK --java25 JDK   build Simbrain 4, check out and build Simbrain 3, download the MNIST data
  bench run --machine NAME [--suites ...] headless suites (run_benchmarks.py)
  bench headed --machine NAME             suites with the GUI open; needs a display (run_headed.py)
  bench torch-threads --machine NAME      PyTorch at several thread counts (torch_threads.py); run after training
  bench summarize [FILES...]              medians from the result files

Everything generated goes under build/benchmarks in the repository (override with --work): the Simbrain 3 worktree,
the data, the Simbrain 4 classpath and the results.
"""
import argparse, datetime, hashlib, json, os, platform, shutil, subprocess, sys, urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
# Simbrain 3 as benchmarked: the Simbrain3 branch at v3.07+15
SIMBRAIN3_COMMIT = "e274a943b"
SIMBRAIN3_BRANCH = "Simbrain3"
DATA_URL = "https://downloads.simbrain.net/simbraindata/"
# MD5 of the files the published results were measured on
DATA_FILES = {
    "tiny_mnist_train_inputs.csv": "4986372cdad007de723b08568a6f89c1",
    "tiny_mnist_train_labels.csv": "8503fe6355be1a029ffbf1dad4d74993",
    "tiny_mnist_test_inputs.csv": "f3832db0d1070d711d51c0abbedb6539",
    "tiny_mnist_test_labels.csv": "c7c97eecbd72c32536e4158280fd3374",
}


def say(message):
    print(f"[bench] {message}", flush=True)


def sh(cmd, cwd=None, env=None, capture=False):
    say(" ".join(str(c) for c in cmd))
    result = subprocess.run([str(c) for c in cmd], cwd=cwd, env={**os.environ, **(env or {})}, text=True,
                            capture_output=capture, stdin=subprocess.DEVNULL)
    if result.returncode != 0:
        if capture:
            sys.stderr.write(result.stdout + result.stderr)
        sys.exit(f"[bench] failed ({result.returncode}): {' '.join(str(c) for c in cmd)}")
    return result.stdout if capture else None


def java_binary(path):
    """Accepts a JDK home or a java executable and returns the executable."""
    p = Path(path).expanduser()
    for candidate in [p, p / "bin" / "java", p / "Contents" / "Home" / "bin" / "java"]:
        if candidate.is_file() and os.access(candidate, os.X_OK):
            return candidate.resolve()
    sys.exit(f"[bench] no java executable at {path}")


def java_major(java):
    out = subprocess.run([str(java), "-version"], capture_output=True, text=True).stderr
    version = out.split('"')[1] if '"' in out else out.split()[2]
    return int(version.split(".")[0])


def config_path(work):
    return work / "config.json"


def load_config(work):
    if not config_path(work).exists():
        sys.exit(f"[bench] {work} is not set up; run `bench setup` first")
    return json.loads(config_path(work).read_text())


def simbrain_cache_dir():
    """Where Simbrain 4 caches downloads (FileUtils.getSystemCacheDirectory)."""
    home = Path.home()
    if platform.system() == "Windows":
        return Path(os.environ.get("LOCALAPPDATA", home / "AppData" / "Local")) / "Simbrain"
    if platform.system() == "Darwin":
        return home / "Library" / "Caches" / "Simbrain"
    return home / ".cache" / "Simbrain"


def setup_simbrain4(java21, work):
    # Gradle runs on Java 21; the init script adds a task that writes the test runtime classpath to a file. No Gradle
    # daemon is left running, since an idle one holds memory through the benchmarks; the Kotlin daemon it starts exits
    # with it.
    java_home = java21.parent.parent
    classpath = work / "s4cp.txt"
    sh([REPO / "gradlew", "-q", "--no-daemon", "--init-script", HERE / "cp.init.gradle", "printTestClasspath"], cwd=REPO,
       env={"JAVA_HOME": str(java_home), "CP_OUT": str(classpath)})
    say(f"Simbrain 4 classpath written to {classpath}")


def setup_simbrain3(java25, work, commit):
    tree = work / "simbrain3"
    if not tree.exists():
        known = subprocess.run(["git", "cat-file", "-e", f"{commit}^{{commit}}"], cwd=REPO,
                               stderr=subprocess.DEVNULL).returncode == 0
        if not known:
            sh(["git", "fetch", "origin", SIMBRAIN3_BRANCH], cwd=REPO)
        sh(["git", "worktree", "add", "--detach", tree, commit], cwd=REPO)
    # The harnesses are new files in Simbrain 3's tree; nothing of Simbrain 3 itself is changed
    target = tree / "src" / "org" / "simbrain" / "benchmarks"
    target.mkdir(parents=True, exist_ok=True)
    for harness in sorted((HERE / "simbrain3").glob("*.java")):
        shutil.copy2(harness, target / harness.name)
    # Equivalent of Simbrain 3's `ant compile`: compile every source and copy the resources beside the classes
    javac = java25.parent / "javac"
    sources = sorted(str(p.relative_to(tree)) for p in (tree / "src").rglob("*.java"))
    (work / "simbrain3-sources.txt").write_text("\n".join(sources) + "\n")
    bin_dir = tree / "bin"
    bin_dir.mkdir(exist_ok=True)
    sh([javac, "-nowarn", "-encoding", "UTF-8", "-cp", "lib/*", "-d", "bin", f"@{work / 'simbrain3-sources.txt'}"],
       cwd=tree, capture=True)
    for resource in (tree / "src").rglob("*"):
        if resource.is_file() and resource.suffix != ".java":
            destination = bin_dir / resource.relative_to(tree / "src")
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(resource, destination)
    say(f"Simbrain 3 ({commit}) built in {tree}")


def setup_data(work):
    data = work / "data"
    data.mkdir(parents=True, exist_ok=True)
    cache = simbrain_cache_dir()
    cache.mkdir(parents=True, exist_ok=True)
    for name, md5 in DATA_FILES.items():
        target = data / name
        if not target.exists():
            say(f"downloading {DATA_URL + name}")
            # The server refuses Python's default user agent
            request = urllib.request.Request(DATA_URL + name, headers={"User-Agent": "simbrain-benchmarks"})
            with urllib.request.urlopen(request) as response:
                target.write_bytes(response.read())
        if hashlib.md5(target.read_bytes()).hexdigest() != md5:
            say(f"warning: {name} differs from the file the published results used")
        # The CNN and MNIST demo harnesses load the data the way the simulations do, through Simbrain's download
        # cache, which would otherwise try to show a download dialog in a headless run
        if not (cache / name).exists():
            shutil.copy2(target, cache / name)
    say(f"data in {data}, Simbrain cache {cache}")


def setup(args):
    work = args.work
    work.mkdir(parents=True, exist_ok=True)
    java21, java25 = java_binary(args.java21), java_binary(args.java25)
    if java_major(java21) != 21:
        sys.exit(f"[bench] --java21 is Java {java_major(java21)}; Simbrain 4 builds with Java 21")
    if java_major(java25) < 21:
        sys.exit(f"[bench] --java25 is Java {java_major(java25)}; the Simbrain 3 runs used Java 25")
    config = {"java21": str(java21), "java25": str(java25), "simbrain3_commit": args.simbrain3_commit}
    if not args.skip_simbrain4:
        setup_simbrain4(java21, work)
    if not args.skip_simbrain3:
        setup_simbrain3(java25, work, args.simbrain3_commit)
    setup_data(work)
    config_path(work).write_text(json.dumps(config, indent=2) + "\n")
    say(f"set up in {work}")


def output_file(args, kind):
    """The --out file, or a dated file in the results folder; its folder is created."""
    out = (args.out or args.work / "results" / f"{args.machine}_{kind}_{datetime.date.today().isoformat()}.jsonl").resolve()
    out.parent.mkdir(parents=True, exist_ok=True)
    return out


def run(args):
    config = load_config(args.work)
    out = output_file(args, "headless")
    sh([sys.executable, HERE / "run_benchmarks.py", "--machine", args.machine, "--out", out, "--suites", args.suites,
        "--java21", config["java21"], "--java25", config["java25"], "--s4cp", args.work / "s4cp.txt",
        "--s4repo", REPO, "--s3dir", args.work / "simbrain3", "--python", sys.executable, "--bench", HERE,
        "--data", args.work / "data", "--heap", args.heap, *(["--s4-only"] if args.s4_only else [])])
    say(f"results in {out}")


def headed(args):
    config = load_config(args.work)
    out = output_file(args, "headed")
    sh([sys.executable, HERE / "run_headed.py", "--machine", args.machine, "--out", out, "--suites", args.suites,
        "--repeats", str(args.repeats), "--heap", args.heap, "--java21", config["java21"], "--java25", config["java25"],
        "--s4cp", args.work / "s4cp.txt", "--s3dir", args.work / "simbrain3"])
    say(f"results in {out}")


def torch_threads(args):
    load_config(args.work)
    out = output_file(args, "torch_threads")
    # The tiny language model runs train on token ids the training suite writes beside its results
    ids = list(out.parent.glob("tinylm_ids_*.txt"))
    if len(ids) < 2:
        sys.exit("[bench] no tiny LM token ids in the results folder; run `bench run --suites training` first")
    cores = os.cpu_count()
    threads = args.threads or ",".join(str(t) for t in sorted({1, 2, 4, 8, 16, cores} & set(range(1, cores + 1))))
    sh([sys.executable, HERE / "torch_threads.py", "--machine", args.machine, "--out", out, "--python", sys.executable,
        "--bench", HERE, "--data", args.work / "data", "--threads", threads, "--repeats", str(args.repeats)])
    say(f"results in {out}")


def summarize(args):
    files = [Path(f) for f in args.files] or list((args.work / "results").glob("*.jsonl"))
    # Ordered by when each file's runs started, which file times do not keep through a git checkout
    files.sort(key=lambda f: min((json.loads(l).get("timestamp", "") for l in f.read_text().splitlines() if l.strip()),
                                 default=""))
    kinds = {"headed": [], "torch_threads": [], "headless": []}
    for f in files:
        suites = {json.loads(l).get("suite") for l in f.read_text().splitlines() if l.strip()}
        kinds["headed" if "headed" in suites else "torch_threads" if "torch_threads" in suites else "headless"].append(f)
    # Later files supersede earlier ones for the same machine, suite and simulator, so files go in time order
    for kind, script in [("headless", "summarize.py"), ("headed", "summarize_headed.py"),
                         ("torch_threads", "summarize_torch_threads.py")]:
        if kinds[kind]:
            print(f"\n##### {kind}: {', '.join(f.name for f in kinds[kind])}", flush=True)
            subprocess.run([sys.executable, str(HERE / script), *map(str, kinds[kind])])


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--work", type=Path, default=REPO / "build" / "benchmarks",
                        help="folder for everything generated (default: build/benchmarks)")
    commands = parser.add_subparsers(dest="command", required=True)

    p = commands.add_parser("setup", help="build both Simbrains and fetch the data")
    p.add_argument("--java21", required=True, help="Java 21 (JDK home or java executable), for Simbrain 4")
    p.add_argument("--java25", required=True, help="Java 25 (JDK home or java executable), for Simbrain 3")
    p.add_argument("--simbrain3-commit", default=SIMBRAIN3_COMMIT)
    p.add_argument("--skip-simbrain3", action="store_true")
    p.add_argument("--skip-simbrain4", action="store_true")
    p.set_defaults(func=setup)

    p = commands.add_parser("run", help="headless suites")
    p.add_argument("--machine", required=True, help="a name for this machine, recorded in every result")
    p.add_argument("--suites", default="headline,threads,training,inference,sweep",
                   help="comma-separated: headline, sweep, threads, training, inference, arrays, smoke")
    p.add_argument("--heap", default="10g", help="Java heap; 10g ran the 8000-neuron sweep on a 16 GB laptop")
    p.add_argument("--s4-only", action="store_true", help="only the Simbrain 4 runs")
    p.add_argument("--out", type=Path)
    p.set_defaults(func=run)

    p = commands.add_parser("headed", help="suites with the GUI open")
    p.add_argument("--machine", required=True)
    p.add_argument("--suites", default="s4,mnist,s3", help="comma-separated: s4, mnist, s3, large")
    p.add_argument("--heap", default="8g", help="Java heap; the large suite needs about 24g for free neurons")
    p.add_argument("--repeats", type=int, default=3)
    p.add_argument("--out", type=Path)
    p.set_defaults(func=headed)

    p = commands.add_parser("torch-threads", help="PyTorch at several thread counts")
    p.add_argument("--machine", required=True)
    p.add_argument("--threads", help="comma-separated thread counts (default: 1,2,4,8,16 and all cores, as available)")
    p.add_argument("--repeats", type=int, default=3)
    p.add_argument("--out", type=Path)
    p.set_defaults(func=torch_threads)

    p = commands.add_parser("summarize", help="medians from result files")
    p.add_argument("files", nargs="*", help="result files (default: everything in the results folder)")
    p.set_defaults(func=summarize)

    args = parser.parse_args()
    args.work = args.work.resolve()
    args.func(args)


if __name__ == "__main__":
    main()
