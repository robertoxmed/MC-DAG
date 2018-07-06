#!/usr/bin/python
import numpy
import os
import optparse
import sys
import shutil
import textwrap
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email import encoders

# Global setup for generation and benchmarks
number_tasks = [10, 20, 50]
number_dags = [2, 4]
number_cores = [4, 8]
edge_percentage = [20, 40]
number_jobs = 5
number_files = "10"

def create_setup():
    # Create the directory tree for generation
    if not os.path.exists("genned"):
        os.makedirs("genned")
    
    for p in edge_percentage:
        if not os.path.exists("genned/e"+str(p)):
            os.makedirs("genned/e"+str(p))
            
        for d in number_dags:
            if not os.path.exists("genned/e"+str(p)+"/"+str(d)):
                os.makedirs("genned/e"+str(p)+"/"+str(d))
                
            for t in number_tasks:
                if not os.path.exists("genned/e"+str(p)+"/"+str(d)+"/"+str(t)):
                    os.makedirs("genned/e"+str(p)+"/"+str(d)+"/"+str(t))
    
    # Create the directory tree for benchmarking
    if not os.path.exists("results"):
        os.makedirs("results")
    
    for p in edge_percentage:
        if not os.path.exists("results/e"+str(p)):
            os.makedirs("results/e"+str(p))
            
        for d in number_dags:
            if not os.path.exists("results/e"+str(p)+"/"+str(d)):
                os.makedirs("results/e"+str(p)+"/"+str(d))
                
            for t in number_tasks:
                if not os.path.exists("results/e"+str(p)+"/"+str(d)+"/"+str(t)):
                    os.makedirs("results/e"+str(p)+"/"+str(d)+"/"+str(t))
                if not os.path.exists("results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/detail"):
                    os.makedirs("results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/detail")
                    
    print("bench> Finished setup!")
    
def clean_generated():
    for p in edge_percentage:            
        for d in number_dags:
            for t in number_tasks:
                # Create the folder string
                folder = str("genned/e"+str(p)+"/"+str(d)+"/"+str(t))
                for file in os.listdir(folder):
                    file_path = os.path.join(folder, file)
                    try:
                        if os.path.isfile(file_path):
                            os.unlink(file_path)
                    except Exception as e:
                        print(e)
                        
    print("All generated files have been cleaned!\n")

def generate():
    
    for p in edge_percentage:            
        for d in number_dags:
            for t in number_tasks:
                # Vary utilization
                for u in numpy.arange(1, 4.1, 0.1):
                    cmd = "java -jar bin/generator.jar -mu "+str(round(u,2))+"\
                           -nd "+str(d)+" -l 2 -nt "+str(t)+" -nf "+number_files+" -e "+str(p)+"\
                           -o genned/e"+str(p)+"/"+str(d)+"/"+str(t)+"/test-"+str(round(u,2))+".xml\
                           -p 2 -j "+str(number_jobs)
                     
                    ret = os.system(cmd)
                    if ret != 0:
                        print("ERROR unexpected behavior for the generation. Exiting...")
                        return -1

def benchmark():
    
    for p in edge_percentage:            
        for d in number_dags:
            for t in number_tasks:
                # Create the result file
                f = open("results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv", "w+")
                f.write("Main; U; Fed (%); PFed; AFed; Lax (%); PLax; ALax\n")
                f.close()
                # Vary utilization
                for u in numpy.arange(1, 4.1, 0.1):
                    cmd = "java -jar bin/benchmark.jar  -i genned/e"+str(p)+"/"+str(d)+"/"+str(t)+"/test-"+str(round(u,2))+"*.xml\
                           -o results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/detail/out-"+str(round(u,2))+".csv \
                           -ot results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv\
                           -u "+str(round(u,2))+" -c 4 -j "+str(number_jobs)
                    ret = os.system(cmd)
                    if ret != 0:
                        print("ERROR unexpected behavior for the benchmarking. Exiting...")
                        return -1

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
        benchmark()
        send_email()
    
    return 0

def print_help(parser):
    parser.print_help()
    
def send_email():
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
    SUBJECT = "Affirmative Dave, I read you"
    TEXT = "I'm sorry Dave, I'm affraid I can't do that"

    msg = MIMEMultipart()
    msg['From'] = "HAL 9000"
    msg['To'] = TO
    msg['Subject'] = SUBJECT
    
    msg.attach(MIMEText(TEXT, 'plain'))
    
    ATTACHMENTS = []
    
    for p in edge_percentage:            
        for d in number_dags:
            for t in number_tasks:
                attachment = open("results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv", "rb")
                part = MIMEBase('application', 'octet-stream')
                part.set_payload((attachment).read())
                encoders.encode_base64(part)
                part.add_header('Content-Disposition', "attachment; filename= results/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv")
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