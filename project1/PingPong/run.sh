cp -r ../rmi client/rmi
cp -r ../rmi server/rmi

cd server 
echo "-------------------------------------"
echo "Initializing server....."
echo "-------------------------------------"
docker build -t server .

docker run -itd  --name server server /bin/bash -c " cd src; javac *.java; java PingPongServer "

address=$(docker inspect --format '{{ .NetworkSettings.IPAddress}}' server)

cd ../client
echo "-------------------------------------"
echo "Initializing client....."
echo "-------------------------------------"
docker build -t client .

docker run -itd --name client  client /bin/bash -c "cd src; javac *.java; java PingPongClient $address 7000 1"

echo "-------------------------------------"
docker logs -f client
echo "-------------------------------------"
