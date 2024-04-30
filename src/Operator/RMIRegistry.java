package Operator;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIRegistry {

    /**
     * @param args main argument from command line, not used
     *             this method sets up the registry for outside elements to connect to
     */
    public static void main(String[] args) {
        System.setProperty("java.rmi.server.hostname", "192.168.183.70"); //Put your own IP here
        System.out.println("Your IP is 192.168.183.70"); //Put your own IP here
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            RegistryOperations operator = new RegistryOperationsImpl(registry);
            registry.bind("operator", operator);
            System.out.println("RMI Registry Created Successfully");
        } catch (RemoteException | AlreadyBoundException e) {
            throw new RuntimeException(e);
        }
    }
}
