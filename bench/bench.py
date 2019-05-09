#!/usr/bin/python
import numpy
import os
import optparse
import sys
import shutil
import textwrap
import smtplib
import time
import matplotlib.pyplot as plt
import csv
import zipfile
import tempfile
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email import encoders
from _ast import With

# Global setup for generation and benchmarks
global number_levels
global number_tasks
global number_dags
global number_cores
global edge_percentage

number_levels = [4]
number_tasks = [20, 30, 50]
number_dags = [2]
number_cores = [4]
edge_percentage = [20]
number_jobs = 8
number_files = "100"

def create_setup():
    # Create the directory tree for generation
    if not os.path.exists("genned"):
        os.makedirs("genned")
        
    for l in number_levels:
        if not os.path.exists("genned/l"+str(l)):
            os.makedirs("genned/l"+str(l))

        for c in number_cores:
            if not os.path.exists("genned/l"+str(l)+"/c"+str(c)):
                os.makedirs("genned/l"+str(l)+"/c"+str(c))

            for p in edge_percentage:
                if not os.path.exists("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)):
                    os.makedirs("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p))

                for d in number_dags:
                    if not os.path.exists("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)):
                        os.makedirs("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d))

                    for t in number_tasks:
                        if not os.path.exists("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)):
                            os.makedirs("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t))

    # Create the directory tree for benchmarking
    if not os.path.exists("results"):
        os.makedirs("results")

    for l in number_levels:
        if not os.path.exists("results/l"+str(l)):
            os.makedirs("results/l"+str(l))
        
        for c in number_cores:
            if not os.path.exists("results/l"+str(l)+"/c"+str(c)):
                os.makedirs("results/l"+str(l)+"/c"+str(c))

            for p in edge_percentage:
                if not os.path.exists("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)):
                    os.makedirs("results/l"+str(l)+"/c"+str(c)+"/e"+str(p))

                for d in number_dags:
                    if not os.path.exists("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)):
                        os.makedirs("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d))

                    for t in number_tasks:
                        if not os.path.exists("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)):
                            os.makedirs("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t))
                        if not os.path.exists("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/detail"):
                            os.makedirs("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/detail")

    print("MC-DAG script > Finished setup!")

def clean_generated():
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        # Create the folder string
                        folder = str("genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t))
                        for file in os.listdir(folder):
                            file_path = os.path.join(folder, file)
                            try:
                                if os.path.isfile(file_path):
                                    os.unlink(file_path)
                            except Exception as e:
                                print(e)

    print("MC-DAG script > All generated files have been cleaned!\n")

def generate():
    
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        # Vary utilization
                        low_bound = c /4
                        step = c * 0.025
                        upper_bound = c + step

                        for u in numpy.arange(low_bound, upper_bound, step):
                            cmd = "java -jar bin/generator.jar -mu "+str(round(u,2))+"\
                                   -nd "+str(d)+" -l "+str(l)+" -nt "+str(t)+" -nf "+number_files+" -e "+str(p)+"\
                                   -o genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/test-"+str(round(u,2))+".xml\
                                   -p 1 -j "+str(number_jobs)

                            ret = os.system(cmd)
                            if ret != 0:
                                print("MC-DAG script > ERROR unexpected behavior for the generation. Exiting...")
                                return -1

def benchmark():
    
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        # Create the result file
#                         f = open("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-l"+str(l)+"-c-"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv", "w+")
#                         if l == 2:
#                             f.write("Main; U; Fed (%); PFed; AFed; AvgFed; Lax (%); PLax; ALax; AvgLax; Edf (%); PEdf; AEdf; AvgEdfHybrid(%); PHybrid; AHybird; AvgHybrid\n")
#                         else:
#                             f.write("Main; U; Lax (%); PLax; ALax; AvgLax; Edf (%); PEdf; AEdf; AvgEdf; Hybrid(%); Ezl; AEzl; AvgEzl\n")
#                         f.close()
                        # Vary utilization
                        low_bound = c /4
                        step = c * 0.025
                        upper_bound = c + step

                        for u in numpy.arange(low_bound, upper_bound, step):
                            cmd = "java -jar bin/benchmark.jar  -i genned/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/test-"+str(round(u,2))+"*.xml\
                                   -o results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/detail/out-"+str(round(u,2))+".csv \
                                   -ot results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-l"+str(l)+"-c-"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv\
                                   -u "+str(round(u,2))+" -c "+str(c)+" -l "+str(l)+" -j "+str(number_jobs)
                            ret = os.system(cmd)
                            if ret != 0:
                                print("ERROR unexpected behavior for the benchmarking. Exiting...")
                                return -1

def plot():
     
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        x = []
                        llf = []
                        edf = []
                        ezl = []
                        
                        with open("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-l"+str(l)+"-c-"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv", 'r') as csvfile:
                            plots = csv.reader(csvfile, delimiter=',')
                            for row in plots:
                                x.append(float(row[0])/4)
                                llf.append(float(row[1]))
                                edf.append(float(row[5]))
                                ezl.append(float(row[9]))
                        plt.figure()            
                        plt.plot(x,llf, label='LLF')
                        plt.plot(x,edf, label='EDF')
                        plt.plot(x,ezl, label='EZL')
                        plt.xlabel('U norm')
                        plt.ylabel('Acceptance rate')
                        plt.title('Results  levels '+str(l)+' tasks '+str(t))
                        plt.legend()
                        plt.savefig("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/graph-l"+str(l)+"-c"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+".png")

def main():
    usage_str = "%prog [options]"
    description_str = "Benchmark script"
    epilog_str = "Examples"

    parser = optparse.OptionParser(usage = usage_str,
                                   description = description_str,
                                   epilog = epilog_str,
                                   add_help_option = False,
                                   version = "%prog version 0.1")

    parser.add_option("-h", "--help", action = "store_true", dest = "help",
                      default = False, help = "Show this message and exit")

    parser.add_option("-s", "--setup", action = "store_true", dest = "setup",
                      default = False, help = "Setup folder for generation/benchmarks")

    parser.add_option("-g", "--generate", action = "store_true", dest = "generate",
                      default = False, help = "Launch generation")

    parser.add_option('-b', "--benchmark", action = "store_true", dest = "benchmark",
                      default = False, help = "Launch benchmarking")

    parser.add_option("-c", "--clean", action = "store_false", dest = "cleanup",
                      default = False, help = "Cleanup generated files")

    (options, args) = parser.parse_args()
    start = time.time()

    if options.help:
        print_help(parser)
        return 0

    if options.setup:
        create_setup()
        return 0

    if options.cleanup:
        clean_generated()
        return 0

    if options.generate:
        generate()

    if options.benchmark:
        #benchmark()
        plot()
        end = time.time()
        send_email(start,end)

    return 0

def print_help(parser):
    parser.print_help()

def send_email(t_start,t_end):
    i = 0
    with open("config.txt") as f:
        for line in f:
            if i == 0:
                FROM = line.rstrip('\n')
            elif i == 1:
                password = line.rstrip('\n')
            elif i == 2:
                TO = line.rstrip('\n')
            i += 1
    nb_files = len(number_levels) * len(number_cores) * len(edge_percentage) * len(number_dags) * len(number_tasks) * int(number_files)
    SUBJECT = "Results for benchmarks ready"
    TEXT = "Statistics for the benchmarks:\n\
              - Elapsed time: "+str(t_end - t_start)+"\n\
              - Number of files: "+str(nb_files)

    msg = MIMEMultipart()
    msg['From'] = "MC-DAG script"
    msg['To'] = TO
    msg['Subject'] = SUBJECT

    msg.attach(MIMEText(TEXT, 'plain'))

    # Attach graphs' PNGs
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        attachment = open("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/graph-l"+str(l)+"-c"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+".png", "rb")
                        part = MIMEBase('application', 'octet-stream')
                        part.set_payload((attachment).read())
                        encoders.encode_base64(part)
                        part.add_header('Content-Disposition', "attachment; filename=results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/graph-l"+str(l)+"-c"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+".png")
                        msg.attach(part)
                        
    # Create a zip with results
    zf = tempfile.TemporaryFile(prefix='results', suffix='.zip')
    zip = zipfile.ZipFile(zf, 'w')
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        zip.write("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-l"+str(l)+"-c-"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv")
    zip.close()
    zf.seek(0)
    part = MIMEBase('application', 'zip')
    part.set_payload(zf.read())
    encoders.encode_base64(part)
    part.add_header('Content-Disposition', "attachment; filename=results.zip")
    msg.attach(part)
    

    server = smtplib.SMTP('smtp.gmail.com:587')
    server.ehlo()
    server.starttls()
    server.login(FROM, password)
    text = msg.as_string()
    server.sendmail(FROM, TO, text)
    server.close()

if __name__ == "__main__":
    sys.exit(main())
