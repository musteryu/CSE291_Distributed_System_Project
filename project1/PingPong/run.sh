rm -rf dataVolume/rmi
cp -r ../rmi dataVolume/rmi
rm -rf $(JAVAFILES:.java=.class) *.zip $(DOCDIR) $(ALLDOCDIR)

cd dataVolume
docker build -t data-volume-image .
docker create -v /src --name data-volume-container data-volume-image /bin/true

cd ../server 
echo "-------------------------------------"
echo "Initializing server....."
echo "-------------------------------------"
docker build -t server .

docker run -itd  --name server --volumes-from data-volume-container server /bin/bash -c " cd src; javac *.java; java PingPongServer "

address=$(docker inspect --format '{{ .NetworkSettings.IPAddress}}' server)

cd ../client
echo "-------------------------------------"
echo "Initializing client....."
echo "-------------------------------------"
docker build -t client .

docker run -itd --name client --volumes-from data-volume-container client /bin/bash -c "cd src; java PingPongClient $address 7000 1"

echo "-------------------------------------"
docker logs -f client
echo "-------------------------------------"
