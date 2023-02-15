package polimi.ds;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CoordinatorInterface extends Remote {

    void replicaConnection(String replicaName) throws RemoteException, NotBoundException;

    boolean put(int k, int v) throws RemoteException;

    Integer get(int k) throws RemoteException;
}
