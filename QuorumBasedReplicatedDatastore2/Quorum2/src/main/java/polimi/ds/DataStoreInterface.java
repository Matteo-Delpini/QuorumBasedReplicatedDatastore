package polimi.ds;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DataStoreInterface extends Remote {
    Integer get(int k) throws RemoteException;

    boolean put(int k, int v) throws RemoteException, InterruptedException;
}
