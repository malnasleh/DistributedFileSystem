package Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class contains the implementation of BackendServiceServer.
 * Main function includes read/write of files.
 */
public class BackendServiceImpl extends UnicastRemoteObject implements BackendService {
    private final Map<String, ReentrantReadWriteLock> fileLocks = new HashMap<>();
    private final Map<String, Instant> fileTimeStamps = new HashMap<>();
    private final Registry registry;
    public boolean isLeader = false;
    private ArrayList<BackendService> allServers;

    protected BackendServiceImpl(Registry registry) throws RemoteException {
        super();
        this.registry = registry;
    }

    /**
     * Reads the file from FileStorage. The critical section also has a lock so that only one process can read or write from the same file.
     * @param fileName The file the server is attempting to read from.
     * @return FileData if the file exists, null otherwise.
     */
    @Override
    public FileData read(String fileName) {
        try {
            System.out.println("Entered read method");
            if (fileName == null || fileName.trim().isEmpty()) {
                return null;
            }
            Path path = Paths.get("FileStorage/" + fileName);
            if (Files.exists(path)) {
                acquireReadLock(fileName);
                FileData readData = new FileData(fileName, Files.readAllBytes(path), FileData.StatusCodeEnum.SUCCESS);
                releaseReadLock(fileName);
                return readData;
            } else {
                return new FileData(null, null, FileData.StatusCodeEnum.OTHER);
            }
        } catch (IOException e) {
            System.out.println("There was an error while reading the file");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the file to FileStorage. Only writes to a file if the new timestamp is later than the current file in FileStorage. The critical section also has a lock so that only one process can read and write to the same file.
     * @param fileName The file the server is attempting to write to.
     * @param data The byte array data of the file.
     * @param timeStamp The UTC time stamp the file is written.
     */
    @Override
    public FileData write(String fileName, byte[] data, Instant timeStamp) {
        allServers = getAllServers();
        System.out.println("Entered write method");
        Path directoryPath = Paths.get("FileStorage");
        Path filePath = directoryPath.resolve(fileName); //adds file name to directory path
        try {
            if (Files.notExists(directoryPath)) {
                Files.createDirectory(directoryPath);
            }
            acquireWriteLock(fileName);
            Instant latestWrite = fileTimeStamps.computeIfAbsent(fileName, k -> timeStamp);
            if (timeStamp.isBefore(latestWrite)) {
                releaseWriteLock(fileName);
                return new FileData(null, null, FileData.StatusCodeEnum.OVERWRITTEN);
            } else {
                fileTimeStamps.put(fileName, timeStamp);
            }
            Files.write(filePath, data);
            releaseWriteLock(fileName);
            if (isLeader) {
                broadcastWrite(fileName, data, timeStamp);
            }
            return new FileData(null, null, FileData.StatusCodeEnum.SUCCESS);
        } catch (IOException e) {
            System.out.println("There was an error while writing the file");
            System.out.println(e.getMessage());
            return new FileData(null, null, FileData.StatusCodeEnum.OTHER);
        }
    }

    /**
     * Get the list of servers.
     * @return The list of servers.
     */
    @Override
    public ArrayList<BackendService> getAllServers() {
        ArrayList<BackendService> servers = new ArrayList<>();
        try {
            for (String name : registry.list()) {
                if (name.contains("server") || name.contains("Server")) {
                    try {
                        servers.add((BackendService) registry.lookup(name));
                    } catch (RemoteException | NotBoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return servers;
    }

    /**
     * Broadcast the write to all other available replicas.
     * @param fileName The file name that has been written to.
     * @param data Byte array of the file.
     * @param timeStamp UTC time stamp of the file.
     */
    @Override
    public void broadcastWrite(String fileName, byte[] data, Instant timeStamp) {
        for (BackendService srvr : allServers) {
            try {
                srvr.write(fileName, data, timeStamp);
                System.out.println("Sent to another server");
            } catch (RemoteException e) {
                System.err.println("Error writing to replica server " + srvr + e.getMessage());
            }
        }
    }

    /**
     * Setter method for leader election.
     */
    @Override
    public void becomeLeader() {
        isLeader = true;
    }

    /**
     * When a new server joins, primary server calls this method to write all files to the newly joined server.
     * @param newServer A newly joined or previous dead server.
     * @throws RemoteException
     */
    @Override
    public void readAll(BackendService newServer) throws RemoteException {
        File[] allFiles = new File("./FileStorage").listFiles();
        if (allFiles == null) {
            return;
        }
        for (File file : allFiles) {
            if (file.isFile() && !file.isHidden()) {
                FileData data = read(file.getName());
                newServer.write(file.getName(), data.fileData(), Instant.now());
            }
        }
    }

    /**
     * Acquire the read lock.
     * @param fileName The file that is being read to.
     */
    public void acquireReadLock(String fileName) {
        ReentrantReadWriteLock lock = getOrCreateLock(fileName);
        lock.readLock().lock();
    }

    /**
     * Release the read lock.
     * @param fileName The file that is being read to.
     */
    public void releaseReadLock(String fileName) {
        ReentrantReadWriteLock lock = fileLocks.get(fileName);
        if (lock != null) {
            lock.readLock().unlock();
        }
    }

    /**
     * Acquire the write lock.
     * @param fileName The file that is being written to.
     */
    public void acquireWriteLock(String fileName) {
        ReentrantReadWriteLock lock = getOrCreateLock(fileName);
        lock.writeLock().lock();
    }

    /**
     * Release the write lock.
     * @param fileName The file that is being written to.
     */
    public void releaseWriteLock(String fileName) {
        ReentrantReadWriteLock lock = fileLocks.get(fileName);
        if (lock != null) {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create a lock for the specified file. The file can have one reader or one writer at a time to ensure consistency.
     * @param fileName The file name of the file being read or written to.
     * @return The lock object.
     */
    private synchronized ReentrantReadWriteLock getOrCreateLock(String fileName) {
        return fileLocks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
    }

}
