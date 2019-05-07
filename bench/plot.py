#!/usr/bin/python
import bench
import csv
import matplotlib.pyplot as plt
from matplotlib import pylab

def plot():
    
    for l in number_levels:
        for c in number_cores:
            for p in edge_percentage:
                for d in number_dags:
                    for t in number_tasks:
                        x = []
                        y = []
                        with open("results/l"+str(l)+"/c"+str(c)+"/e"+str(p)+"/"+str(d)+"/"+str(t)+"/out-l"+str(l)+"-c-"+str(c)+"-e"+str(p)+"-"+str(d)+"-"+str(t)+"-total.csv", 'r') as csvfile:
                            plots = csv.reader(csvfile, delimiter=',')
                            for row in plots:
                                x.append
def main():
    return 0