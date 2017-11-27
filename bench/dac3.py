#!/usr/bin/python
import os
import numpy as np

min_u = 1.05
setp_u = 0.05
i = 0
j = 0

for u in np.arange(1.05, 2, 0.05):
    g_cmd = "java -jar bin/generator.jar -c 4 -cp 30 -e 40 -h "+str(u)+" -lu "+str(u)+" -hl 0.5 -l "+str(u)+" -nd 4 -nf 1000 -o btests/test"+str(u)+".xml -p 2 -j  8 -g"
    ret = os.system(g_cmd)
    i += 1

for u2 in np.arange(1.05, 2, 0.05):
    r_cmd = "java -jar bin/bench.jar -i btests/test"+str(u2)+"*.xml -o out"+str(j)+".csv -j8"
    ret = os.system(r_cmd)
    j += 1
