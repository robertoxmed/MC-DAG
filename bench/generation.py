#!/usr/bin/python
import os
import numpy as np

for u in np.arange(1, 4.05, 0.05):
    g_cmd = "java -jar bin/generator.jar -mu "+str(u)+" -nd 2 -l 2 -nt 20 -nf 1000 -e 20 -o genned/test-"+str(u)+".xml -p 2 -j 8 -g"
    ret = os.system(g_cmd)
