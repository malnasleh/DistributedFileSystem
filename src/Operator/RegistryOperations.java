package Operator;

import Proxy.Proxy;
import Server.BackendService;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

public interface RegistryOperations extends Remote {

    void bindRemoteObject(Remote objectToBeBound, String name) throws RemoteException;

    void unBindRemoteObject(String name) throws RemoteException;

    ArrayList<Map.Entry<String, BackendService>> getAllServers() throws RemoteException;

    ArrayList<Map.Entry<String, Proxy>> getAllProxies() throws RemoteException;

    Map.Entry<String, BackendService> getCentralAuthority() throws RemoteException;

    void setNewCentralAuthority(String newName, BackendService newLeader) throws RemoteException;
}
