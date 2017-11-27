#!/bin/bash

#########################
# Generate random MC-DAGs
#########################
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 1.5 -lu 0.5 -hl 0.25 -l 1.5 -nd 2 -nf 500 -o btests/test1.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 1.5 -lu 0.5 -hl 0.25 -l 1.5 -nd 4 -nf 500 -o btests/test2.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 1.9 -lu 1.1 -hl 0.5 -l 1.9 -nd 2 -nf 500 -o btests/test3.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 1.9 -lu 1.1 -hl 0.5 -l 1.9 -nd 4 -nf 500 -o btests/test4.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 1.5 -lu 1.5 -hl 0.5 -l 1.5 -nd 2 -nf 500 -o btests/test5.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 1.5 -lu 1.5 -hl 0.5 -l 1.5 -nd 4 -nf 500 -o btests/test6.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 2 -lu 2 -hl 0.5 -l 2 -nd 2 -nf 500 -o btests/test7.xml -p 2 -j8 -g
java -jar bin/generator.jar -c 4 -cp 30 -e 30 -h 2 -lu 2 -hl 0.5 -l 2 -nd 4 -nf 500 -o btests/test8.xml -p 2 -j8 -g

#########################
# Run benchmarks on generated MC-DAGs
#########################
java -jar bin/bench.jar -i btests/test1*.xml -o out1.csv -j8
java -jar bin/bench.jar -i btests/test2*.xml -o out2.csv -j8
java -jar bin/bench.jar -i btests/test3*.xml -o out3.csv -j8
java -jar bin/bench.jar -i btests/test4*.xml -o out4.csv -j8
java -jar bin/bench.jar -i btests/test5*.xml -o out5.csv -j8
java -jar bin/bench.jar -i btests/test6*.xml -o out6.csv -j8
java -jar bin/bench.jar -i btests/test7*.xml -o out7.csv -j8
java -jar bin/bench.jar -i btests/test8*.xml -o out8.csv -j8
