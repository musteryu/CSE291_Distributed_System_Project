import rmi.RMIException;

public class PingPong implements PingPongInterface{
    public String ping(int idNumber) throws RMIException {
        return "pong" + idNumber;
    }
}



