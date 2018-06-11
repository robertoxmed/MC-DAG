Mixed-Criticality DAG Framework
====

[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)](LICENSE)

This repository holds the Mixed-Criticality Directed Acyclic Graph (MC-DAG) framework. The tool is composed of three different components: a scheduler module for MC-DAGs, an availability estimator and a MC-DAG random generator.

## Scheduler

The scheduler uses list-based algorithms to allocate tasks to cores. Static scheduling **tables** for all the criticality modes of a system are computed by this tool.

The scheduling tables for the high-criticality modes are computed first in order to determine **safe** activation instants for high-criticality tasks. The low-criticality scheduling table is then computed by allowing preemption and migration of tasks when the activation instants of HI criticality tasks occur.

## Random MC-DAG Generator

This framework includes a random generator of MC-DAGs. This generator aims to produce unbiased MC-DAGs to perform benchmarks on scheduling policies.

The unbiased generation relies on methods to create random topologies on DAGs, and uniform distributions of utilization to tasks.

## Availability

This framework is also capable of performing model transformations to compute availability rates for low-critilicaty outputs of a MC-DAG. Models generated are probabilistic automata used by the [PRISM Model Checker](http://www.prismmodelchecker.org/).

## About

This implementation supports the following papers:
* *Directed Acyclic Graph Scheduling for Mixed-Criticality Systems - R. Medina, E. Borde and L. Pautet* - presented at [Ada Europe 2017](https://www.auto.tuwien.ac.at/~blieb/AE2017/).
* *Availability Enhancement and Analysis for Mixed-Criticality Systems on Multi-core - R. Medina, E. Borde and L. Pautet* - presented at [DATE 2018](https://www.date-conference.com/)
* *Scheduling Multi-Periodic Mixed-Criticality DAGs on Multi-core Architectures - R. Medina, E. Borde and L. Pautet*.
