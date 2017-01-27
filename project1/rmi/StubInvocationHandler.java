package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException; // Do we need to throw this?
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

public class StubInvocationHandler implements java.lang.reflect.InvocationHandler
{
    private Class c;
    private InetSocketAddress address;

    public StubInvocationHandler(Class c, InetSocketAddress address)
    {
        this.c = c;
        this.address = address;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        String methodName = method.getName();
        //System.out.println("[StubInvocationHandler.java]: " + methodName);
        switch (methodName) {
            case "hashCode":
                return this.hashCode();
            case "toString":
                return this.toString();
            case "equals":
                if (args == null || args[0] == null){
                    return false;
                }
                if (args.length == 1 && Proxy.isProxyClass(args[0].getClass())) {
                    if (!method.getReturnType().getName().equals("boolean")) {
                        StubInvocationHandler otherStub = (StubInvocationHandler) Proxy.getInvocationHandler(args[0]);
                        return this.getAddress().equals(otherStub.getAddress()) &&
                            this.getInterface().equals(otherStub.getInterface());
                    }
                }
                return false;
        }

        Transmittable response;

        try {
            response = remoteInvoke(method, args);
        } catch (Exception e) {
            throw new RMIException(e.getCause());
        }

        if (response == null) {
            return null;
        } else if (response.checkException()) {
            throw (Exception) response.retrieve();
        } else {
            return response.retrieve();
        }

    }


    private Transmittable remoteInvoke(Method method, Object[] args) throws RMIException {
        try {

            Socket connection = new Socket();
            connection.connect(address, 1000);

            ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
            oos.flush();
            oos.writeObject(method.getName());
            oos.writeObject(method.getReturnType().getName());
            oos.writeObject(method.getParameterTypes());
            oos.writeObject(args);

            ObjectInputStream oins = new ObjectInputStream(connection.getInputStream());
            Transmittable response = (Transmittable) oins.readObject();
            connection.close();
            return response;
        } catch (Exception e) {
            throw new RMIException(e.getCause());
        }

    }

    public int hashCode() {
        return this.address.hashCode() + this.c.hashCode();
    }

    public String toString() {
        String string =  "Stub for RMI " + this.getInterface().toString()         +":\n"+
                         "Remote Address: " + this.getAddress().toString()       +"\n"+
                         "Hostname: " + this.getAddress().getHostName()          +"\n"+
                         "Port: " + String.valueOf(this.getAddress().getPort())       ;
        return string;
    }

    public Class getInterface() {
        return this.c;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }
}
