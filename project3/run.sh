#!/bin/bash
N=${1:-5}

echo "build docker hadoop image"

sudo docker rm -f hadoop-master &> /dev/null
i=1
while [ $i -lt $N ]
do 
    sudo docker rm -f hadoop-slave$i &> /dev/null
    i=$(( $i + 1 ))
done
sudo docker rmi project3/hadoopimage
sudo docker build -t project3/hadoopimage .
sudo docker network rm hadoop
sudo docker network create --driver=bridge hadoop

echo ""


# start hadoop master container
echo "start hadoop-master container..."
sudo docker run -itd \
                --net=hadoop \
                -p 50070:50070 \
                -p 8088:8088 \
                --name hadoop-master \
                --hostname hadoop-master \
                project3/hadoopimage &> /dev/null


# start hadoop slave container
i=1
while [ $i -lt $N ]
do
	echo "start hadoop-slave$i container..."
	sudo docker run -itd \
	                --net=hadoop \
	                --name hadoop-slave$i \
	                --hostname hadoop-slave$i \
	                project3/hadoopimage &> /dev/null
	i=$(( $i + 1 ))
done 

# get into hadoop master container
sudo docker exec -it hadoop-master bash
