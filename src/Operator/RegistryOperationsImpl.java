package Operator;

import Proxy.Proxy;
import Server.BackendService;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
/**
 * This Class abstracts the access to the registry in which the methods are called to
 * make updates and access info from the registry
 */
public class RegistryOperationsImpl extends UnicastRemoteObject implements RegistryOperations {

    private final Registry registry;

    /**
     *
     * @param registry Reference Instance of the RMI registry
     * @throws RemoteException Required as a part of RMI
     * This is the constructer for the registry operations object,
     * sets up reference to the registry
     */
    protected RegistryOperationsImpl(Registry registry) throws RemoteException {
        super();
        this.registry = registry;
    }

    /**
     *
     * @param objectToBeBound Reference to the object that is being bound to the registry
     * @param name Name of the object in the RMI regisry
     * This method takes in an new object and object name and calls the registry method
     * to bind it to the naming list and
     */
    @Override
    public void bindRemoteObject(Remote objectToBeBound, String name) {
        try {
            registry.rebind(name, objectToBeBound);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param name name of the object to be unbound
     * This method takes in the name of an object to released from the RMI registry
     * and calls the registry to remove it from its naming list
     */
    @Override
    public synchronized void unBindRemoteObject(String name) {
        try {
            registry.unbind(name);
        } catch (NotBoundException ignored) {
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @return returns an array list of all the server names and a reference to
     * Their object in a map object
     */
    @Override
    public ArrayList<Map.Entry<String, BackendService>> getAllServers() {
        ArrayList<Map.Entry<String, BackendService>> servers = new ArrayList<>();
        try {
            for (String name : registry.list()) {
                if (name.contains("server") || name.contains("Server")) {
                    try {
                        servers.add(new AbstractMap.SimpleEntry<>(name, (BackendService) registry.lookup(name)));
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
     *
     * @return returns an array list of all the proxy names and a reference to
     * Their object in a map object
     */
    @Override
    public ArrayList<Map.Entry<String, Proxy>> getAllProxies() {
        ArrayList<Map.Entry<String, Proxy>> proxies = new ArrayList<>();
        try {
            for (String name : registry.list()) {
                if (name.contains("proxy") || name.contains("Proxy")) {
                    proxies.add(new AbstractMap.SimpleEntry<>(name, (Proxy) registry.lookup(name)));
                }
            }
        } catch (RemoteException | NotBoundException e) {
            throw new RuntimeException(e);
        }
        return proxies;
    }

    /**
     *
     * @return a map object of the central authority primary replica server name and object
     * @throws RemoteException requirement of RMI
     */
    @Override
    public synchronized Map.Entry<String, BackendService> getCentralAuthority() throws RemoteException {
        String currentName = "";
        try {
            System.out.println(Arrays.toString(registry.list()));
            for (String name : registry.list()) {
                if (name.contains("centralAuthority")) {
                    return new AbstractMap.SimpleEntry<>(name, (BackendService) registry.lookup(name));
                }
            }
            for (String name : registry.list()) {
                if (name.contains("server") || name.contains("Server")) {
                    currentName = name;
                    BackendService newLeader = (BackendService) registry.lookup(name);
                    unBindRemoteObject(name);
                    newLeader.becomeLeader();
                    setNewCentralAuthority("centralAuthority" + newLeader.hashCode(), newLeader);
                    return new AbstractMap.SimpleEntry<>("centralAuthority" + newLeader.hashCode(), newLeader);
                }
            }
        } catch (RemoteException e) {
            unBindRemoteObject(currentName);
            Map.Entry<String, BackendService> newLeader = getCentralAuthority();
            if (newLeader == null) {
                return new AbstractMap.SimpleEntry<>("", null);
            }
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
        return new AbstractMap.SimpleEntry<>("", null);
    }

    /**
     *
     * @param newName name of new primary Central authority server
     * @param newLeader reference to the object of new central authority server
     * This method is called to set a new server of current replicas to be the leader server
     */
    @Override
    public void setNewCentralAuthority(String newName, BackendService newLeader) {
        try {
            for (String name : registry.list()) {
                if (name.contains("centralAuthority")) {
                    unBindRemoteObject(name);
                }
            }
            bindRemoteObject(newLeader, newName);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


}

