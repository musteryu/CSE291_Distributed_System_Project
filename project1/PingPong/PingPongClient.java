package PingPong;
import rmi.*;

import java.net.InetSocketAddress;

public class PingPongClient {
    protected static PingPong stub = null;
    public static void main(String[] args){
        InetSocketAddress address =
                new InetSocketAddress(7000);
        stub = Stub.create(PingPong.class,address);

        String response;
        for (int i = 0; i < 4; i++) {
            response = stub.ping(88888);
            if (response.startsWith("Pong")){
                continue;
            } else {
                throw new Error();
            }
        }
    }
}
