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
  java -jar generator.jar  -w 32 -e 30 -h 10 -hp 30 -o /home/roberto/workspace/LS_mxc/tests/ex$i.test -d /home/roberto/workspace/LS_mxc/tests/ex$i.dzn
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
