package rmi;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

class StubInvocationHandler<T> implements java.lang.reflect.InvocationHandler, Serializable {
    private Class<T> c;
    private InetSocketAddress address;

    StubInvocationHandler(Class<T> c, InetSocketAddress address)
    {
        this.c = c;
        this.address = address;
        System.out.println("create invocation handler with address: " + address);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        String methodName = method.getName();
        switch (methodName) {
            case "hashCode":
                try {
                    this.c.getMethod("hashCode", method.getParameterTypes());
                } catch (Exception e) {
                    return this.hashCode();
                }
                break;
            case "toString":
                try {
                    this.c.getMethod("toString", method.getParameterTypes());
                } catch (Exception e) {
                    return this.toString();
                }
                break;
            case "equals":
                try {
                    this.c.getMethod("equals", method.getParameterTypes());
                } catch (Exception e) {
                    if (args != null && args.length == 1 && args[0] != null && Proxy.isProxyClass(args[0].getClass())) {
                        InvocationHandler thisHandler = Proxy.getInvocationHandler(proxy);
                        InvocationHandler thatHandler = Proxy.getInvocationHandler(args[0]);
                        if (thisHandler.equals(thatHandler)) {
                            return true;
                        }
                    }
                    return false;
                }
                break;
        }
        boolean isRmi = false;
        for (Class<?> ex: method.getExceptionTypes()) {
            if (ex.getName().equals(RMIException.class.getName())) {
                isRmi = true;
                break;
            }
        }
        if (!isRmi) {
            throw new Exception("invoked method doesn't belong to a remote interface");
        }

        Response response;
        try {
            response = remoteInvoke(method, args);
        } catch (Exception e) {
            throw new RMIException(e.getMessage(), e.getCause());
        }

        if (response == null) {
            return null;
        } else {
            try {
                return response.getOrThrow();
            } catch (Throwable throwable) {
                if (throwable instanceof InvocationTargetException) {
                    throw ((InvocationTargetException) throwable).getTargetException();
                } else if (throwable instanceof RMIException) {
                    throw ((RMIException) throwable);
                } else {
                    throw throwable;
                }
            }
        }
    }

    private Response remoteInvoke(Method method, Object[] args) throws RMIException {
        try {
            System.out.println("begin connect: " + address);
            Socket connection = new Socket(address.getHostName(), address.getPort());
            ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
            oos.flush();
            if (args != null) {
                System.out.println("pass in parameters: " + args[0]);
                oos.writeObject(new Request(method, args));
            } else {
                oos.writeObject(new Request(method));
            }
            // oos.flush();
            Thread.sleep(10);
            ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
            Object response = ois.readObject();
            System.out.println("Get response: " + response);
            oos.close();
            ois.close();
            return ((Response) response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RMIException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public String toString() {
        return  "Stub for RMI " + this.getInterface().toString()                    +":\n"+
                "Remote Address: " + this.getInetSocketAddress().toString()         +"\n"+
                "Hostname: " + this.getInetSocketAddress().getHostName()            +"\n"+
                "Port: " + String.valueOf(this.getInetSocketAddress().getPort());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && this.getClass().isAssignableFrom(obj.getClass())) {
            final StubInvocationHandler that = ((StubInvocationHandler) obj);
            return this.address.equals(that.address) && this.c.getName().equals(that.c.getName());
        }
        return false;
    }

    Class getInterface() {
        return this.c;
    }

    InetSocketAddress getInetSocketAddress() {
        return this.address;
    }
}
