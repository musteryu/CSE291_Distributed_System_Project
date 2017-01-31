package PingPong;
import rmi.*;
public interface PingPongInterface {
    public String ping(int idNumber) throws RMIException;
}
