#!/usr/bin/python
import os
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
            for dags in range (0, nbdags):
                u_hi_in_lo = cores - 1
                para = cores * 2
                gen_command = "java -jar generator.jar -h "+str(u_hi)+" -l "+str(u_lo)+" -hl "+str(u_hi_in_lo)+" -e "+str(edge_proba)+" -cp "+str(c_path)+" -c "+str(max_nb_cores)+" -p "+str(para)+" -o ../tests/ex"+str(dags)+".test"
                os.system(gen_command)

            for dags in range (0, nbdags):
                exe_command = "java -jar ls-alloc.jar -i "+ex_root+str(dags)+".test"
                ret = os.system(exe_command)
                if ret == 0:
                    success += 1
            line = "cores %d U_LO %f U_HI %f Success rate: %d\n" % (cores, u_lo, u_hi, success)
            f.write(line)
            success = 0
    print(" Cores ", cores)
    min_u_lo = cores

f.close()
