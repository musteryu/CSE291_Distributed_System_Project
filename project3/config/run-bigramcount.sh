#!/bin/bash
$HADOOP_HOME/sbin/start-dfs.sh
$HADOOP_HOME/sbin/start-yarn.sh
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

# direct the output to a file
hdfs dfs -cat output/* > ./output/bigram_result

# process the bigram counts
python count_bigrams.py output/bigram_result
