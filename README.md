Mixed-Criticality DAG Framework
====

[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)](LICENSE)

Mixed-criticality framework for MC-DAGs. This implementation is able to schedule one or multiple MC-DAGs in a multi-core architecture for Mixed-critcality systems. The scheduler is efficient and ensures that safe mode transitions are possible.

This framework has an implementation of the scheduling algorithm in addition to a generator of MC-DAGs. A model transformation is also possible in order to compute availability of LO criticality outputs of the MC-DAG.

## Scheduler

The scheduler uses List Scheduling to allocate tasks to cores. The HI scheduling table is computed first in order to determine **safe** activation instants for HI criticality tasks. The LO scheduling table is then computed by allowing preemption and migration of tasks when the activation instants of HI criticality tasks occur.

## Generator

The benchmarking tool generates random DAGs to be evaluated by the scheduling algorithm.

## Availability

This framework is also capable of performing a model transformation to compute availability rates for LO outputs of a MC-DAG. Models generated are probabilistic automata used by the [PRISM Model Checker](http://www.prismmodelchecker.org/).

## About

This implementation supports the following papers:
* *Directed Acyclic Graph Scheduling for Mixed-Criticality Systems - R. Medina, E. Borde and L. Pautet* - presented at [Ada Europe 2017](https://www.auto.tuwien.ac.at/~blieb/AE2017/).
* *Availability Enhancement and Analysis for Mixed-Criticality Systems on Multi-core  - R. Medina, E. Borde and L. Pautet*.
