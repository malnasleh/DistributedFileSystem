package Proxy;

import Server.BackendService;

import java.net.Inet4Address;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

import Operator.RegistryOperations;

public class ProxyServer {

    /**
     * @param args main method argument
     *             this method runs the proxy implementation
     */
    public static void main(String[] args) {
        System.setProperty("java.rmi.server.hostname", "192.168.183.70"); //Put your own IP here
        try {
            Registry registry = LocateRegistry.getRegistry("192.168.183.70", 1099); //Put IP of machine hosting RMI here.
            RegistryOperations registryOperations = (RegistryOperations) registry.lookup("operator");
            Map.Entry<String, BackendService> server = registryOperations.getCentralAuthority();
            if (server.getValue() == null) {
                System.out.println("No servers exist. Please connect a server and try again. Exiting.");
                return;
            }
            ProxyImpl proxy = new ProxyImpl(server, registryOperations, registry);
            registryOperations.bindRemoteObject(proxy, Inet4Address.getLocalHost().getHostName() + proxy.hashCode() + "proxy");
            System.out.println("Proxy object bound to RMI registry: " + Inet4Address.getLocalHost().getHostName() + proxy.hashCode() + "proxy");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
