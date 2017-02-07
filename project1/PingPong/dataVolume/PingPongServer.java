import rmi.*;

import java.net.InetSocketAddress;

public class PingPongServer {
    protected static Skeleton<PingPongInterface> skeleton;
    protected static PingPong server = null;
    public static void main(String[] args){
        server = new PingPong();
        InetSocketAddress address = new InetSocketAddress("", 7000);
        skeleton = new Skeleton<>(PingPongInterface.class, server, address);
        try {
            skeleton.start();
        } catch (RMIException e){
            e.printStackTrace();
        }
    }
}
