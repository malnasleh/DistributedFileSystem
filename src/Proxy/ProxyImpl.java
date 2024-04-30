package Proxy;

import Operator.RegistryOperations;
import Server.FileData;
import Server.BackendService;

import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.*;

/**
 * This class contains the implementation of our proxy
 * the proxies sit inbetween the client and servers and route requests
 * and responses to their respective destination
 */
public class ProxyImpl extends UnicastRemoteObject implements Proxy {
    private final RegistryOperations operations;
    private final Registry registry;
    private Map.Entry<String, BackendService> server;


    /**
     * @param server     central authority server reference and name
     * @param operations reference to the operations object for the registry
     * @param registry   reference to the registry
     * @throws RemoteException required as of RMI
     *                         Constructor for proxy object
     */
    protected ProxyImpl(Map.Entry<String, BackendService> server, RegistryOperations operations, Registry registry) throws RemoteException {
        super();
        this.server = server;
        this.operations = operations;
        this.registry = registry;
    }

    /**
     * @param fileName name of file it wants to read
     * @return returns the file that ti got from the server
     * @throws RemoteException required
     *                         This method takes in a file name and finds a random server to read that file from
     *                         when it gets the file back it returns it
     */
    @Override
    public FileData forwardRead(String fileName) throws RemoteException {
        System.out.println("Entered proxy read method");
        Map.Entry<String, BackendService> readServer = getRandomServer();
        try {
            if (readServer == null) {
                for (String name : registry.list()) {
                    if (name.contains("centralAuthority")) {
                        server = operations.getCentralAuthority();
                        return this.server.getValue().read(fileName);
                    }
                }
                return new FileData(null, null, FileData.StatusCodeEnum.NO_SERVERS);
            }
            return readServer.getValue().read(fileName);
        } catch (Exception e) {
            if (readServer != null) {
                System.out.println("Unbinding " + readServer.getKey());
                this.operations.unBindRemoteObject(readServer.getKey());
            } else if (server != null) {
                System.out.println("Unbinding " + server.getKey());
                this.operations.unBindRemoteObject(server.getKey());
            } else {
                System.out.println("Couldn't find any servers to service your request.");
                return new FileData(null, null, FileData.StatusCodeEnum.NO_SERVERS);
            }
            return forwardRead(fileName);
        }
    }

    /**
     * @param fileName  name of file
     * @param data      file byte array
     * @param timeStamp timestamp of when the request was made
     * @throws RemoteException required
     *                         This method takes a file and sends it to the central authority for it to
     *                         write to the storage network
     */
    @Override
    public FileData forwardWrite(String fileName, byte[] data, Instant timeStamp) throws RemoteException {
        System.out.println("Entered proxy write method");
        try {
            return server.getValue().write(fileName, data, timeStamp);
        } catch (Exception e) {
            operations.unBindRemoteObject(server.getKey());
            Map.Entry<String, BackendService> newLeader = operations.getCentralAuthority();
            if (newLeader.getValue() == null) {
                System.out.println("There are no more servers left");
            } else {
                this.server = newLeader;
                System.out.println("Elected new Leader");
                return forwardWrite(fileName, data, timeStamp);
            }
        }
        return new FileData(null, null, FileData.StatusCodeEnum.NO_SERVERS);
    }

    private Map.Entry<String, BackendService> getRandomServer() {
        ArrayList<Map.Entry<String, BackendService>> servers;
        try {
            servers = operations.getAllServers();
            if (servers.isEmpty()) return null;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        servers.add(server);
        Collections.shuffle(servers);
        return servers.get(0);
    }
}
