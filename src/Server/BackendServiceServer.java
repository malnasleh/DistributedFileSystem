package Server;

import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

import Operator.RegistryOperations;

public class BackendServiceServer {

    /**
     * @param args main class argument
     * @throws RemoteException      Used for catching errors for fault tolerance
     * @throws UnknownHostException required exception handling.
     */
    public static void main(String[] args) throws RemoteException, UnknownHostException {
        System.setProperty("java.rmi.server.hostname", "192.168.183.70"); //Put your own machine's IP here
        Registry registry = LocateRegistry.getRegistry("192.168.183.70", 1099); //Put IP of machine hosting RMI here.
        RegistryOperations registryOperations = null;
        BackendServiceImpl server = new BackendServiceImpl(registry);
        Map.Entry<String, BackendService> currentLeader = null;
        try {
            // tries to bind to the primary server
            registryOperations = (RegistryOperations) registry.lookup("operator");
            currentLeader = registryOperations.getCentralAuthority();
            // first server to come online, join as primary server
            if (currentLeader.getValue() == null) {
                server.isLeader = true;
                registryOperations.setNewCentralAuthority("centralAuthority" + server.hashCode(), server);
                System.out.println("Main Server bound with name: centralAuthority" + server.hashCode());
            } else {
                // join as a replica server
                server.isLeader = false;
                // if current leader is dead, following line will throw RemoteException
                currentLeader.getValue().readAll(server);
                String name = InetAddress.getLocalHost().getHostName() + server.hashCode() + "server";
                registryOperations.bindRemoteObject(server, name);
                System.out.println("New Server bound with name: " + name);
            }
        } catch (RemoteException e) {
            // current leader is dead, initiate leader election
            if (registryOperations != null && currentLeader != null) {
                // find a new leader
                registryOperations.unBindRemoteObject(currentLeader.getKey());
                currentLeader = registryOperations.getCentralAuthority();
                // if no servers are registered, become the new leader
                if (currentLeader.getValue() == null) {
                    server.isLeader = true;
                    registryOperations.setNewCentralAuthority("centralAuthority" + server.hashCode(), server);
                    System.out.println("Main Server bound with name: centralAuthority" + server.hashCode());
                } else {
                    // join as a replica server
                    server.isLeader = false;
                    currentLeader.getValue().readAll(server);
                    String name = InetAddress.getLocalHost().getHostName() + server.hashCode() + "server";
                    registryOperations.bindRemoteObject(server, name);
                    System.out.println("New Server bound with name: " + name);
                }
            }
        } catch (NotBoundException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
