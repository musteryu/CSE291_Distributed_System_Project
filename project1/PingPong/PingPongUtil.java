package PingPong;

import java.net.InetSocketAddress;


public class PingPongUtil {
    static final int PORT = 7000;

    public static InetSocketAddress getAddress(){
        return new InetSocketAddress(PORT);
    }
}
