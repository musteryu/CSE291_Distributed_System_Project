#!/bin/bash
echo -e "\n"

$HADOOP_HOME/sbin/start-dfs.sh

echo -e "\n"

$HADOOP_HOME/sbin/start-yarn.sh

echo -e "\n"

mkdir input
mkdir output

# create input files
cat sherlock_holmes.txt > input/sherlock_holmes.txt

# create input directory on HDFS
hadoop fs -mkdir -p input

# put input files to HDFS
hdfs dfs -put ./input/* input

# run wordcount
hadoop jar bc.jar BigramCount input output

# print the input file
echo -e "\nbigram count input:"

# print the output of wordcount
echo -e "\nbigram count output:"
hdfs dfs -cat output/* > ./output/bigram_result
