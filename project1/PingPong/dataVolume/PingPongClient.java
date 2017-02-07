import rmi.*;

import java.net.InetSocketAddress;

public class PingPongClient {
    protected static PingPongInterface stub = null;
    public static void main(String[] args){
        if(args.length != 3) {
            System.err.println("Usage : java PingPongClient <hostname> <port number> <id number>");
            System.exit(1);
        }
        String hostName = args[0];
        int port = Integer.valueOf(args[1]);
        int idNumber = Integer.valueOf(args[2]);
        try {
            Thread.sleep(1000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        InetSocketAddress ad = new InetSocketAddress(hostName,port);
        stub = PingPongServerFactory.makePingPongServer(ad);
        int successCount = 0;

        try {
            for(int i = 0 ; i < 4 ; i++){
                String response = stub.ping(idNumber);
                int res = Integer.valueOf(response.substring(4));
                if(idNumber == res){
                    successCount++;
                }
            }
            System.out.println("4 Tests Completed, " + String.valueOf(successCount) + " Tests Passed");

        } catch (RMIException e) {
            e.printStackTrace();
        }
    }
}
