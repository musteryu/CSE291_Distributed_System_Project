package PingPong;
import rmi.*;

public class PingPongServer {
    protected static Skeleton<PingPongInterface> skeleton = null;
    protected static PingPong server = null;
    public static void main(String[] args){
        server = new PingPong();
        skeleton = new Skeleton<>(PingPongInterface.class, server, PingPongUtil.getAddress());

    }
}
