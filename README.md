Mixed-Criticality DAG Framework
====

[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)](LICENSE)

Mixed-criticality scheduler for MC-DAGs. This implementation is able to schedule MC-DAGs for multi-core architectures with Mixed-critcality. The scheduler is efficient and ensures that safe mode transitions are possible.

This simulator has an implementation of the algorithm in addition to a generator
of MC DAGs.

## Scheduler

The scheduler uses List Scheduling to allocate tasks to cores. The HI scheduling table is computed first in order to determine **safe** activation instants for HI criticality tasks. The LO scheduling table is then computed by allowing preemption and migration of tasks when the activation instants of HI criticality tasks occur.

## Generator

The benchmarking tool generates random DAGs to be evaluated by the scheduling algorithm. The generated DAGs are schedulable by HLFET without safe mode transitions.

## Availability

This framework is also capable of performing a transformation model to compute availability rates for LO outputs of the MCDAG. Models generated are probabilistic automata for the [PRISM Model Checker](http://www.prismmodelchecker.org/).

## About

This implementation supports the following paper
* *Directed Acyclic Graph Scheduling for Mixed-Criticality Systems - R. Medina, E. Borde and L. Pautet* - presented at [Ada Europe 2017](https://www.auto.tuwien.ac.at/~blieb/AE2017/).
* *Availability Analysis for Mixed-Criticality Multi-core Systems with Task Precedence Constraints  - R. Medina, E. Borde and L. Pautet*
