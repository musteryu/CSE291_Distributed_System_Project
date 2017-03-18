#!/bin/bash
N=${1:-5}
echo "\nstop docker hadoop containers\n"

sudo docker stop -f hadoop-master &> /dev/null
sudo docker rm -f hadoop-master &> /dev/null
i=1
while [ $i -lt $N ]
do
    sudo docker stop -f hadoop-slave$i &> /dev/null
    sudo docker rm -f hadoop-slave$i &> /dev/null
    i=$(( $i + 1 ))
done
