package polimi.ds;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReplicaInterface extends Remote {
    int getWriteVote(int k) throws RemoteException;

    void commitWrite(int k, int v) throws RemoteException;

    void abortWrite(int k, int v) throws RemoteException;

    void connectReplica(ReplicaInterface replica) throws RemoteException;

    void propagateReplica(ReplicaInterface replica) throws RemoteException;

    void receivePropagatedPut(int k, int v) throws RemoteException, InterruptedException;

    int propagateGet(int k) throws RemoteException;

    void propagateReadThreshold(int readThreshold) throws RemoteException;

    void propagateWriteThreshold(int writeThreshold) throws RemoteException;
}
