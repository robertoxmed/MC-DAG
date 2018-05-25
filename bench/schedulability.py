#!/usr/bin/python
import os
import numpy as np

for u2 in np.arange(1, 4.05, 0.05):
    r_cmd = "java -jar bin/bench.jar -i genned/test-"+str(u2)+"*.xml -o results/out-"+str(u2)+".csv -j8"
    ret = os.system(r_cmd)
