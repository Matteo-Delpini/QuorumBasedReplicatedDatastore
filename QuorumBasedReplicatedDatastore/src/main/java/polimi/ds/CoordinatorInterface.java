package polimi.ds;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CoordinatorInterface extends Remote {

    void replicaConnection(ReplicaInterface replicaInterface) throws RemoteException;

    boolean put(int k, int v) throws RemoteException;

    Integer get(int k) throws RemoteException;
}
