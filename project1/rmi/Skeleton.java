package rmi;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{
    private InetSocketAddress address;
    private ExecutorService pool;
    private ServerSocket serverSocket;
    private T impl;
    private Class<T> c;
    private Listener listener;
    private boolean active;
    private static final int THREAD_NUM = 20;
    private static final int DEFAULT_PORT = 5656;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private Throwable stopEx = null;

    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
        this(c, server, null);
    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
        RMIUtil.checkInterface(c);
        RMIUtil.checkNotNull(c, server);
        this.impl = server;
        this.active = false;
        this.c = c;
        this.pool = Executors.newFixedThreadPool(THREAD_NUM);
        this.address = address;
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {

    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {

    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws rmi.RMIException
    {
        if (isActive()) {
            System.out.println("> Skeleton is already running on address: " + this.address);
            return;
        }
        /* try to open socket */
        ServerSocket servsock = null;
        try {
            servsock = new ServerSocket();
        } catch (IOException ioe) {
            System.out.println("> Skeleton failed to open TCP socket");
            throw new RMIException("Skeleton failed to open TCP socket", ioe.getCause());
        }
        /* try to bind socket */
        try {
            if (this.address == null) {
                servsock.bind(new InetSocketAddress("0.0.0.0", 0));
            } else {
                servsock.bind(this.address);
            }
            this.address = new InetSocketAddress(servsock.getInetAddress(), servsock.getLocalPort());
        } catch (IOException e) {
            System.out.println("> Skeleton failed to bind address");
            throw new rmi.RMIException("Skeleton failed to bind address", e.getCause());
        }
        /* open listener thread */
        this.serverSocket = servsock;
        this.listener = new Listener(this.serverSocket, this.impl, this.pool);
        this.listener.start();
        active = true;
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {
        if (!active) return;
        active = false;
        if (listener == null) return;
        listener.close();
        System.out.println("> Joining the listener");
        Listener l = listener;
        listener = null;
        stopped(this.stopEx);
        try {
            l.join();
        } catch (InterruptedException ite) {
            ite.printStackTrace();
        }
    }

    /**
     * Returns the address of the endpoint this socket is bound to.
     * @return server socket address, <code>null</code> when no address assigned.
     */
    synchronized InetSocketAddress getSocketAddress() {
        return this.address;
    }

    /**
     * Whether the listener has been started;
     * @return true when listener has been started and false otherwise.
     */
    synchronized boolean isActive() {
        return this.active;
    }


    /**
     * Listener for incoming requests, only starts when wrapped in a thread.
     */
    private class Listener extends Thread {
        private ServerSocket serverSocket;
        private boolean active;
        private T impl = Skeleton.this.impl;
        private ExecutorService pool;

        Listener(ServerSocket serverSocket, T impl, ExecutorService pool) {
            this.active = false;
            this.serverSocket = serverSocket;
            this.pool = pool;
            this.impl = impl;
        }

        @Override
        public void run() {
            this.active = true;
            List<Future<?>> futures = new LinkedList<>();
            List<Worker> workers = new LinkedList<>();
            while (isActive()) {
                try {
                    System.out.println("> Listener starts at address: " + this.serverSocket.getLocalSocketAddress());
                    Socket socket = this.serverSocket.accept();
                    System.out.println("> Get connection from" + socket.getRemoteSocketAddress());
                    Worker worker = new Worker(socket, this.impl);
                    System.out.println("> Start a new worker: " + worker);
                    if (isActive()) {
                        System.out.println("> Submit the new worker: " + worker);
                        workers.add(worker);
                        futures.add(this.pool.submit(worker));
                    }
                } catch (SocketException se) {
                    System.out.println("> SocketException: " + se.getMessage());
                    if (serverSocket.isClosed()) {
                        this.active = false;
                    }
                } catch (IOException e) {
                    if (Skeleton.this.isActive() && Skeleton.this.listen_error(e)) continue;
                    this.active = false;
                    Skeleton.this.stopEx = e;
                    e.printStackTrace();
                }
            }
            for (Worker w: workers) {
                w.close(); // close those no ready to invoke
            }
            for (Future<?> f: futures) {
                try {
                    System.out.println("> Get result from future: " + f.get());
                } catch (Throwable throwable) {
                    System.out.println("> Error while waiting for thread results, with error <" + throwable.getMessage()
                    + "> and cause <" + throwable.getCause() + ">");
                }
            }
        }

        private void close() {
            try {
                if (!this.serverSocket.isClosed()) {
                    System.out.println("> Close serverSocket");
                    this.serverSocket.close();
                }
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        }

        public boolean isActive() {
            return this.active;
        }
    }

    private class Worker implements Runnable {
        private Socket socket;
        private T impl;
        private boolean closed;
        private boolean readyToInvoke;

        Worker(Socket socket, T impl) {
            this.socket = socket;
            this.impl = impl;
            this.closed = false;
            this.readyToInvoke = false;
        }

        @Override
        public void run() {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try {
                oos = new ObjectOutputStream(this.socket.getOutputStream());
                oos.flush();
                Thread.sleep(10);
                InputStream is = this.socket.getInputStream();
                ois = new ObjectInputStream(is);
                this.readyToInvoke = true;
                try {
                    Object ret = invoke(ois);
                    oos.writeObject(Response.result(ret));
                } catch (InvocationTargetException | RMIException e) {
                    oos.writeObject(Response.except(e));
                }
            } catch (ClassNotFoundException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | IOException
                    | SecurityException e) {
                Skeleton.this.service_error(new RMIException(e.getMessage(), e.getCause()));
            } catch (InterruptedException it) {
                it.printStackTrace();
            } finally {
                closeSocket();
                try {
                    if (oos != null) oos.close();
                    if (ois != null) ois.close();
                } catch (IOException ioe) {
                    System.out.println("> Worker failed to close socket");
                }
            }
        }

        private synchronized Object invoke(ObjectInputStream ois) throws
                ClassNotFoundException,
                NoSuchMethodException,
                IllegalAccessException,
                InvocationTargetException,
                IOException, RMIException {
            Object res = null;

            Request request = (Request) ois.readObject();
            String methodName = request.unwrapMethodName();
            System.out.println("> Method name is: " + methodName);
            Class<?>[] types = request.unwrapTypes();
            Method method = Skeleton.this.c.getMethod(methodName, types);
            method.setAccessible(true);
            System.out.println("> Get method: " + method.getName());
            System.out.print("> Get parameters: ");
            for (Object p: request.unwrapParams()) {
                System.out.print(" " + p);
            }
            System.out.println();

            boolean mvalid = false;
            for (Method m: this.impl.getClass().getMethods()) {
                if (m.getName().equals(method.getName())) {
                    mvalid = true;
                    break;
                }
            }
            if (!mvalid) {
                throw new RMIException("Method not in skeleton interface");
            }

            res = method.invoke(this.impl, request.unwrapParams());
            System.out.println("> Get invoke result: " + res);

            return res;
        }

        private void closeSocket() {
            try {
                this.socket.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        private void close() {
            this.closed = true;
            if (!readyToInvoke) {
                closeSocket();
            }
        }
    }
}
