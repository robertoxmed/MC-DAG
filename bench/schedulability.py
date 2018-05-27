#!/usr/bin/python
import os
import numpy as np

for tasks in (10, 20, 50):
    # Create files for percentages
    f = open("results/e20/2/"+str(tasks)+"/out-e20-2-"+str(tasks)+"-total.csv", "w+");
    f.write("Main; U; Fed (%); Lax (%)\n")
    f.close()
    f2 = open("results/e20/4/"+str(tasks)+"/out-e20-4-"+str(tasks)+"-total.csv", "w+");
    f2.write("Main; U; Fed (%); Lax (%)\n")
    f2.close()
    f3 = open("results/e40/2/"+str(tasks)+"/out-e40-2-"+str(tasks)+"-total.csv", "w+");
    f3.write("Main; U; Fed (%); Lax (%)\n")
    f3.close()
    f4 = open("results/e40/2/"+str(tasks)+"/out-e40-4-"+str(tasks)+"-total.csv", "w+");
    f4.write("Main; U; Fed (%); Lax (%)\n")
    f4.close()

    for u in np.arange(1, 4.05, 0.05):
        r_cmd = "java -jar bin/bench.jar -i genned/e20/2/"+str(tasks)+"/test-"+str(round(u,2))+"*.xml\
                -o results/e20/2/"+str(tasks)+"/detail/out-"+str(round(u,2))+".csv \
                -ot results/e20/2/"+str(tasks)+"/out-e20-2-"+str(tasks)+"-total.csv\
                -u "+str(round(u,2))+" -c 4 -j 2"

        r_cmd2 = "java -jar bin/bench.jar -i genned/e40/2/"+str(tasks)+"/test-"+str(round(u,2))+"*.xml\
                -o results/e40/2/"+str(tasks)+"/detail/out-"+str(round(u,2))+".csv \
                -ot results/e40/2/"+str(tasks)+"/out-e40-2-"+str(tasks)+"-total.csv\
                -u "+str(round(u,2))+" -c 4 -j 2"

        ret = os.system(r_cmd)
        ret = os.system(r_cmd)

    for u2 in np.arange(2, 8.1, 0.1):
        r_cmd3 = "java -jar bin/bench.jar \
                -i genned/e20/4/"+str(tasks)+"/test-"+str(round(u2,2))+"*.xml\
                -o results/e20/4/"+str(tasks)+"/detail/out-"+str(round(u2,2))+".csv \
                -ot results/e20/4/"+str(tasks)+"/out-e20-4-"+str(tasks)+"-total.csv\
                -u "+str(round(u2,2))+" -c 8 -j 2"

        r_cmd4 = "java -jar bin/bench.jar \
                -i genned/e40/4/"+str(tasks)+"/test-"+str(round(u2,2))+"*.xml\
                -o results/e40/4/"+str(tasks)+"/detail/out-"+str(round(u2,2))+".csv \
                -ot results/e40/4/"+str(tasks)+"/out-e40-4-"+str(tasks)+"-total.csv\
                -u "+str(round(u2,2))+" -c 8 -j 2"

        ret = os.system(r_cmd3)
        ret = os.system(r_cmd4)
