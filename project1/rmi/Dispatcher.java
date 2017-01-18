package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by musteryu on 2017/1/17.
 */
class Dispatcher<T> implements Runnable {
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private boolean isStopped;
    private Class<T> typeClass;

    Dispatcher(Class<T> c, ServerSocket serverSocket, int poolSize) {
        this.serverSocket = serverSocket;
        this.pool = Executors.newFixedThreadPool(poolSize);
        this.isStopped = false;
        this.typeClass = c;
    }

    public void run() {
        try {
            while (!isStopped()) {
                Socket socket = this.serverSocket.accept();
                this.pool.execute(new SocketHandler(socket));
            }
        } catch (IllegalAccessException | InstantiationException | IOException ie) {
            ie.printStackTrace();
        }
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

     synchronized void stop() {
        if (!isStopped) {
            isStopped = true;
            try {
                this.serverSocket.close();
                List<Runnable> stillRunnings = this.pool.shutdownNow();
                for (Runnable each: stillRunnings) {
                    ((SocketHandler) each).drain();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SocketHandler implements Runnable {
        private Socket socket;
        private T delegation;
        private boolean drained;

        SocketHandler(Socket socket) throws InstantiationException, IllegalAccessException {
            this.socket = socket;
            this.delegation = Dispatcher.this.typeClass.newInstance();
            this.drained = false;
        }

        synchronized void drain() {
            this.drained = true;
        }

        public void run() {
            try {
                while ( !this.drained ) {
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    /* preparation */
                    String methodName = ((String) inputStream.readObject());
                    Object arg, type;
                    List<Class> types = new LinkedList<>();
                    List<Object> args = new LinkedList<>();
                    while ((type = inputStream.readObject()) != null &&
                            (arg = inputStream.readObject()) != null) {
                        types.add(Class.forName(((String) type)));
                        args.add(arg);
                    }

                    /* get invoke method */
                    Class[] typeArray = new Class[types.size()];
                    Object[] argArray = new Object[args.size()];
                    types.toArray(typeArray);
                    args.toArray(argArray);
                    Method method = this.delegation.getClass().getMethod(methodName, typeArray);

                    /* invoke method and return */
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    outputStream.writeObject(method.getReturnType().getName());
                    outputStream.writeObject(method.invoke(argArray));
                    outputStream.flush();
                }
            } catch (IOException | ClassNotFoundException | NoSuchMethodException |
                    IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
