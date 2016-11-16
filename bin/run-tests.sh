#!/bin/bash

echo "========================================================================="
echo "=             MXC Allocator test script                                 ="
echo "========================================================================="
echo ""

echo "Enter the number of DAGs to generate: "

read nbdags

while [[ $nbdags -lt 0 ]]; do
  echo "Number of DAGs needs to be postive. Please insert a valid number."
  read nbdags
done

java -jar ls_alloc.jar -i ../tests/ex1.test
