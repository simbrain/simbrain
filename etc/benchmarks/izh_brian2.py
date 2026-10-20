"""Izhikevich (2003) network in Brian2, matched to IzhikevichBenchmark.kt.

Forward Euler at dt; Gaussian noise current redrawn each step (sigma 5 exc / 2 inh);
a presynaptic spike adds w*dt to v_post (equivalent to a one-step current pulse of height w).
Usage: izh_brian2.py <runtime|cpp_standalone> N p dt_ms sim_ms seed [weight_scale]
"""
import json, sys, time, os, shutil, tempfile
import numpy as np

target, N, p, dt_ms, sim_ms, seed_value = sys.argv[1], int(sys.argv[2]), float(sys.argv[3]), float(sys.argv[4]), float(sys.argv[5]), int(sys.argv[6])
weight_scale = float(sys.argv[7]) if len(sys.argv) > 7 else 1.0

t_import = time.perf_counter()
from brian2 import *
import brian2
t_import = time.perf_counter() - t_import

if target == "cpp_standalone":
    outdir = tempfile.mkdtemp(prefix="brian2_out_")
    set_device("cpp_standalone", directory=outdir, build_on_run=False)
    prefs.devices.cpp_standalone.openmp_threads = int(os.environ.get("BRIAN_THREADS", "0"))
else:
    prefs.codegen.target = "cython"


brian2.seed(seed_value)
np.random.seed(seed_value)
defaultclock.dt = dt_ms * ms

t0 = time.perf_counter()
Ne = int(N * 0.8)
eqs = """
dv/dt = (0.04*v**2 + 5*v + 140 - u + I)/ms : 1
du/dt = a*(b*v - u)/ms : 1
I : 1
a : 1
b : 1
c : 1
d : 1
sigma : 1
"""
G = NeuronGroup(N, eqs, threshold="v >= 30", reset="v = c; u += d", method="euler")
re = np.random.rand(N)
exc = np.arange(N) < Ne
G.a = np.where(exc, 0.02, 0.02 + 0.08 * re)
G.b = np.where(exc, 0.2, 0.25 - 0.05 * re)
G.c = np.where(exc, -65 + 15 * re**2, -65.0)
G.d = np.where(exc, 8 - 6 * re**2, 2.0)
G.sigma = np.where(exc, 5.0, 2.0)
G.v = -65.0
G.u = "b*v"
G.run_regularly("I = sigma*randn()", when="start")

S = Synapses(G, G, "w : 1", on_pre="v_post += w*dt/ms")
S.connect(condition="i != j", p=p)
S.w = "weight_scale * (0.5 * int(i < Ne) - int(i >= Ne)) * rand()"
mon = SpikeMonitor(G, record=False)
net = Network(G, S, mon)
build_s = time.perf_counter() - t0

t1 = time.perf_counter()
net.run(sim_ms * ms, report=None)
if target == "cpp_standalone":
    device.build(directory=outdir, compile=True, run=True, debug=False)
run_wall = time.perf_counter() - t1

result = {
    "simulator": f"brian2-{target}",
    "version": brian2.__version__,
    "neurons": N, "p": p, "weight_scale": weight_scale, "synapses": int(len(S)), "dt_ms": dt_ms, "sim_ms": sim_ms,
    "build_s": round(build_s, 4),
    "rate_hz": round(mon.num_spikes / N / (sim_ms / 1000), 3),
}
if target == "cpp_standalone":
    # run_wall includes code generation + compilation; the binary's own timing isolates simulation
    result["codegen_compile_run_s"] = round(run_wall, 4)
    result["run_s"] = round(device._last_run_time, 4)
    shutil.rmtree(outdir, ignore_errors=True)
else:
    result["run_s"] = round(run_wall, 4)
result["rt_factor"] = round(result["run_s"] / (sim_ms / 1000), 4)
print(json.dumps(result))
