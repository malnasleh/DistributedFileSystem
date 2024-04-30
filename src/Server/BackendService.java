package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.ArrayList;

public interface BackendService extends Remote {

    FileData read(String fileName) throws RemoteException;

    FileData write(String fileName, byte[] data, Instant timeStamp) throws RemoteException;

    ArrayList<BackendService> getAllServers() throws RemoteException;

    void broadcastWrite(String fileName, byte[] data, Instant timeStamp) throws RemoteException;

    void becomeLeader() throws RemoteException;

    void readAll(BackendService newServer) throws RemoteException;

}
