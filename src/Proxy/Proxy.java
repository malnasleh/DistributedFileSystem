package Proxy;

import Server.FileData;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;

public interface Proxy extends Remote {

    FileData forwardRead(String fileName) throws RemoteException;

    FileData forwardWrite(String client, byte[] data, Instant timeStamp) throws RemoteException;

}
