### how to build images
To simplify your life, please build images and start the containers using the following command:
```
sh run.sh <number of cluster nodes n>
```
This might require your sudo authorization. <number of cluster nodes n> should be a number indicating
that one master and n-1 slaves are started.

### how to run wordcount example
When the hadoop-master container is started, please make sure you are in the home directory.
You can find all the files needed in the home directory.
To run wordcount example, please run:
```
sh run-wordcount.sh
```

### how to run bigramcount example
The java file is already compiled, therefore you only need to run the following shell code:
```
sh run-bigramcount.sh
```
The result is indirected to local file **output/bigram_result**. If you really hate this setup, you can
change the file **run-bigramcount.sh** :). I know you won't, right?

### how to count the result
To analyze the result of bigramcount, please run the python program:
```
python count_bigrams.py output/bigram_result
```

### about the input files
The input files are in the input directory including several sample text files and a novel *sherlock_holmes.txt*.
Change any files in this directory or add file will influence the result.

### how to stop containers
```
sh stop.sh <number of cluster nodes n>
```
**Please make sure that the parameter n is equal to the number when you are building images**
