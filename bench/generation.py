#!/usr/bin/python
import os
import numpy as np

for tasks in (10, 20, 50):
    for u in np.arange(1, 4.05, 0.05):
            g_cmd = "java -jar bin/generator.jar -mu "+str(round(u,2))+"\
                     -nd 2 -l 2 -nt "+str(tasks)+" -nf 1000 -e 20\
                     -o genned/e20/2/"+str(tasks)+"/test-"+str(round(u,2))+".xml -p 2 -j 5"

            g_cmd3 = "java -jar bin/generator.jar -mu "+str(round(u,2))+"\
                     -nd 2 -l 2 -nt "+str(tasks)+" -nf 1000 -e 40\
                     -o genned/e40/4/"+str(tasks)+"/test-"+str(round(u,2))+".xml -p 2 -j 5"

            ret = os.system(g_cmd)
            ret = os.system(g_cmd3)
    for u2 in np.arange(2, 8.1, 0.1):
            g_cmd2 = "java -jar bin/generator.jar -mu "+str(round(u2,2))+"\
                     -nd 4 -l 2 -nt "+str(tasks)+" -nf 1000 -e 20\
                     -o genned/e20/4/"+str(tasks)+"/test-"+str(round(u2,2))+".xml -p 2 -j 5"

            g_cmd4 = "java -jar bin/generator.jar -mu "+str(round(u2,2))+"\
                     -nd 4 -l 2 -nt "+str(tasks)+" -nf 1000 -e 40\
                     -o genned/e40/4/"+str(tasks)+"/test-"+str(round(u2,2))+".xml -p 2 -j 5"

            ret = os.system(g_cmd2)
            ret = os.system(g_cmd4)

