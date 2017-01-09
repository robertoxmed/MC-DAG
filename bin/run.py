#!/usr/bin/python
import os
import numpy as np

nbdags = 100
max_nb_cores = 4
step_utilization = 0.5
min_u_lo = 4
min_u_hi = 4
edge_proba = 20
c_path = 30
success = 0

test_cores = [8]

ex_root="../tests/ex"
ouput_path = "../tests/results20-1.csv"

f = open("results20-1.csv", 'w')
f.write("Cores, U LO, U HI, Success rate\n")

for cores in test_cores:
    for u_lo in np.arange(min_u_lo, cores + 0.1 , step_utilization):
        for u_hi in np.arange(min_u_lo, cores + 0.1, step_utilization):
            dags = 0
            while (dags < nbdags):
                u_hi_in_lo = min(u_hi,u_lo)/2
                para = cores * 2
                gen_command = "java -jar generator.jar -h "+str(u_hi)+" -l "+str(u_lo)+" -hl "+str(u_hi_in_lo)+" -e "+str(edge_proba)+" -cp "+str(c_path)+" -c "+str(cores)+" -p "+str(para)+" -o ../tests/ex"+str(cores)+"-lo-"+str(u_lo)+"-hi-"+str(u_hi)+"-e-"+str(edge_proba)+"-"+str(dags)+".test"
                ret = os.system(gen_command)
                if (ret == 0):
                    dags += 1

            print("Testing cores ", cores, " U_LO ", u_lo, " U_HI ", u_hi, " U HI in LO ", u_hi_in_lo )

            for dags in range (0, nbdags):
                exe_command = "java -jar ls-alloc.jar -i "+ex_root+str(cores)+"-lo-"+str(u_lo)+"-hi-"+str(u_hi)+"-e-"+str(edge_proba)+"-"+str(dags)+".test"
                ret = os.system(exe_command)
                if ret == 0:
                    success += 1
            line = "%d, %f, %f, %d\n" % (cores, u_lo, u_hi, success)
            f.write(line)
            success = 0
            dags = 0
    print(" Cores ", cores)
    min_u_lo = cores

f.close()