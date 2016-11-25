#!/bin/bash

echo "========================================================================="
echo "=                   MXC Allocator test script                           ="
echo "========================================================================="
echo ""

echo "Enter the number of DAGs to generate: [ENTER] "

read nbdags

while [[ $nbdags -le 0 ]]; do
  echo "Number of DAGs needs to be postive. Please insert a valid number."
  read nbdags
done

echo ""
echo "Will generate $nbdags DAGs."

for i in `seq 1 $nbdags`
do
  java -jar generator.jar  -h 4 -l 4 -hl 2 -e 30 -cp 20  -o /home/roberto/workspace/LS_mxc/tests/ex$i.test
done

echo ""
echo "Running the allocation and writting to output files"
echo ""

for i in `seq 1 $nbdags`
do
  echo "================== Testing for file $i ========================="
  java -jar ls_alloc.jar -i ../tests/ex$i.test -o ../tests/ex$i.out
  echo ""
done
