FROM ubuntu:14.04

# Install Java.
RUN \
  apt-get update && \
  apt-get install -y openjdk-7-jdk && \
  rm -rf /var/lib/apt/lists/*

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

RUN mkdir /src
RUN mkdir /src/rmi

ADD rmi /src/rmi


ADD PingPongClient.java /src

ADD PingPong.java /src

ADD PingPongInterface.java /src
