package rmi;

import rmi.RMIUtil;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ServerSocket serverSocket;
    private SocketAddress address;
    private ExecutorService pool;
    private T impl;
    private Listener listener;
    private boolean isStarted;
    private static final int THREAD_NUM = 10;

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
        RMIUtil.checkInterface(c);
        RMIUtil.checkNotNull(c, server);
        this.impl = server;
        this.pool = Executors.newFixedThreadPool(THREAD_NUM);
        this.isStarted = false;
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
        this.address = address;
        this.pool = Executors.newFixedThreadPool(THREAD_NUM);
        this.isStarted = false;
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
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(address);
            listener = new Listener(serverSocket);
            new Thread(listener).start();
            isStarted = true;
        } catch (IOException e) {
            throw new rmi.RMIException(e.getMessage(), e.getCause());
        }
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
        listener.closeListener();
        isStarted = false;
    }

    /**
     * Returns the address of the endpoint this socket is bound to.
     * @return server socket address, <code>null</code> when no address assigned.
     */
    public InetSocketAddress getLocalSocketAddress() {
        return ((InetSocketAddress) serverSocket.getLocalSocketAddress());
    }

    /**
     * Get local port number
     * @return the port number to which this socket is listening or
     *          -1 if the socket is not bound yet.
     */
    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Returns the local address of this server socket.
     * @return the address to which this socket is bound,
     *         or the loopback address if denied by the security manager,
     *         or {@code null} if the socket is unbound.
     */
    public InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    /**
     * Whether the listener has been started;
     * @return true when listener has been started and false otherwise.
     */
    public boolean isStarted() {
        return this.isStarted;
    }


    /**
     * Listener for incoming requests, only starts when wrapped in a thread.
     */
    private class Listener implements Runnable {
        private ServerSocket serverSocket;
        private boolean isClosed;
        private final T impl = Skeleton.this.impl;
        private final ExecutorService pool = Skeleton.this.pool;
        Listener(ServerSocket serverSocket) {
            this.isClosed = false;
            this.serverSocket = serverSocket;
        }

        Listener() {
            this(null);
        }

        @Override
        public void run() {
            try {
                while (!isClosed) {
                    Socket socket = serverSocket.accept();
                    this.pool.submit(new Worker(socket));
                }
            } catch (IOException e) {
                Skeleton.this.listen_error(e);
                e.printStackTrace();
            } finally {
                this.pool.shutdownNow();
            }
        }

        public void closeListener() {
            this.isClosed = true;
        }

        public boolean isClosed() {
            return this.isClosed;
        }
    }

    private class Worker implements Runnable {
        private Socket socket;
        private final T impl = Skeleton.this.impl;
        Worker(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ObjectOutputStream ous = null;
            try {
                ous = new ObjectOutputStream(this.socket.getOutputStream());
//                ous.flush();
                ObjectInputStream ois = new ObjectInputStream(this.socket.getInputStream());
                Object ret = invoke(ois);
                ous.writeObject(Either.left(ret));
                ous.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                try {
                    ous.writeObject(Either.right(e));
                    ous.flush();
                } catch (IOException ioe) {
                    e.printStackTrace();
                }
            } finally {
                closeSocket();
            }
        }

        private Object invoke(ObjectInputStream ois) throws
                    ClassNotFoundException,
                    IOException,
                    NoSuchMethodException,
                    InvocationTargetException,
                    IllegalAccessException {
                String methodName = ((String) ois.readObject());
                Object typeName = null, arg = null;
                List<Class<?>> types = new ArrayList<>();
                List<Object> args = new ArrayList<>();
                while ((typeName = ois.readObject()) != null &&
                        (arg = ois.readObject()) != null) {
                    types.add(Class.forName((String) typeName));
                    args.add(arg);
                }
                Class[] typeArr = new Class[types.size()];
                Object[] argArr = new Object[args.size()];
                types.toArray(typeArr);
                args.toArray(argArr);
                Method method = this.impl.getClass().getMethod(methodName, typeArr);
                return method.invoke(this.impl, argArr);
        }

        private void closeSocket() {
            try {
                this.socket.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
