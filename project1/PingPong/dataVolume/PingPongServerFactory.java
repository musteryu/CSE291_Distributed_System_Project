import rmi.Stub;

import java.net.InetSocketAddress;

public class PingPongServerFactory {
    public static PingPongInterface makePingPongServer(InetSocketAddress ad){
        return Stub.create(PingPongInterface.class, ad);
    }
}
