package polimi.ds;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReplicaInterface extends Remote {
    void put(int k, int v) throws RemoteException;
    Integer get(int k) throws RemoteException;

    void abortPut(int k, int v) throws RemoteException;
    void commitPut(int k, int v) throws RemoteException;
    boolean vote(int k, int v) throws RemoteException;
}
