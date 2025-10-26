# Performance baselines

The table tracks macrobenchmark medians gathered from the `:benchmark` suite. Values are milliseconds and should be refreshed whenever an optimisation changes behaviour.

| Scenario | Metric | Before (ms) | After (ms) | Notes |
| --- | --- | --- | --- | --- |
| Cold start to list | Startup median | Pending | Pending | Capture on a physical device after running `connectedCheck`. |
| Open board tab | Frame time median | Pending | Pending | Measure navigation after warm start. |
| Open timeline tab | Frame time median | Pending | Pending | Measure navigation after warm start. |
| Add quick task | Frame time median | Pending | Pending | Measure the task composer interaction. |
| Scroll populated list | Frame time median | Pending | Pending | Use the seeded 2k task dataset. |

Update the figures once macrobenchmarks complete on target hardware so regressions can be tracked release over release.
