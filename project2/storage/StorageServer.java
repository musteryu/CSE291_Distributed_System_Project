package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    private enum State {RUNNING,FREE,DEAD};

    private File root = null;
    private int clientPort = 0;
    private int commandPort = 0;
    private State state = State.FREE;
    private Skeleton<Storage> clientSkeleton = null;
    private Skeleton<Command> commandSkeleton = null;

    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {
        if(root == null) {
            throw new NullPointerException("Root is null.");
        }
        this.root = root.getAbsoluteFile();
        this.clientPort = client_port;
        this.commandPort = command_port;
    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root,0,0);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        switch(this.state) {
            case RUNNING:
                throw new RMIException("Storage server already running.");
            case DEAD:
                throw new RMIException("Storage server is dead.");
        }

        if (this.clientPort == 0) {
            clientSkeleton = new Skeleton<>(Storage.class, this);
        } else {
            clientSkeleton = new Skeleton<>(Storage.class, this, new InetSocketAddress(this.clientPort));
        }

        if (commandPort == 0) {
            commandSkeleton = new Skeleton<>(Command.class, this);
        } else {
            commandSkeleton = new Skeleton<>(Command.class, this, new InetSocketAddress(commandPort));
        }
        clientSkeleton.start();
        commandSkeleton.start();

        Storage clientStub = Stub.create(Storage.class, clientSkeleton, hostname);
        Command commandStub = Stub.create(Command.class, commandSkeleton, hostname);

        Path[] paths = Path.list(root);
        naming_server.register(clientStub, commandStub, paths);

        this.state = State.RUNNING;
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        try {
            clientSkeleton.stop();
            commandSkeleton.stop();
            stopped(null);
        } catch (Throwable cause) {
            stopped(cause);
        }
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File local = file.toFile(root);
        if (!local.exists() || local.isDirectory()) {
            throw new FileNotFoundException("File does not exist or is a directory.");
        }
        return local.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File local = file.toFile(root);
        if (!local.exists() || local.isDirectory()) {
            throw new FileNotFoundException("File does not exist or is a directory.");
        }

        if (offset < 0 || offset > Integer.MAX_VALUE || length < 0 || offset + length > local.length()) {
            throw new IndexOutOfBoundsException("File read position/length not valid.");
        }

        // Just raise an IOException if file read is not successful for any reason
        try {
            RandomAccessFile fileReader = new RandomAccessFile(local, "r");
            byte[] content = new byte[length];
            fileReader.seek(offset);
            fileReader.readFully(content, 0, length);
            return content;
        } catch(Exception e) {
            throw new IOException("File read failed.");
        }
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File local = file.toFile(root);
        if (!local.exists() || local.isDirectory()) {
            throw new FileNotFoundException("File does not exist or is a directory.");
        }

        if (!local.canWrite()) {
            throw new IOException("File cannot be written.");
        }

        if (offset < 0) {
            throw new IndexOutOfBoundsException("Offset is negative.");
        }

        try {
            RandomAccessFile fileWriter = new RandomAccessFile(local, "rw");
            fileWriter.seek(offset);
            fileWriter.write(data);
        } catch(Exception e) {
            throw new IOException("File read failed.");
        }
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
