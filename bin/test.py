#!/usr/bin/python

import numpy as np

nbdags = 10
max_nb_cores = 4
step_utilization = 0.2
min_u_lo = 1
min_u_hi = 1
edge_proba = 20
c_path = 30
success = 0

ex_root="../tests/ex"
ouput_path = "../tests/results.out"

f = open("results.out", 'w')

for cores in range(2, max_nb_cores+1, 2):
    for u_lo in np.arange(min_u_lo, cores, step_utilization):
        for u_hi in np.arange(min_u_lo, cores, step_utilization):
            print(" U LO ", u_lo, " U HI ", u_hi)
    min_u_lo = cores
