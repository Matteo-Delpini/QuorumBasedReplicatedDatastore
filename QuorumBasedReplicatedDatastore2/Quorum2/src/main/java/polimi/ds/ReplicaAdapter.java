package polimi.ds;

import java.rmi.RemoteException;

public class ReplicaAdapter implements ReplicaInterface{

    private final Replica replica;

    public ReplicaAdapter(Replica replica) {
        this.replica = replica;
    }

    @Override
    public int getWriteVote(int k) throws RemoteException {
        return replica.getWriteVote(k);
    }

    @Override
    public void commitWrite(int k, int v) throws RemoteException {
        replica.commitWrite(k,v);
    }

    @Override
    public void abortWrite(int k, int v) throws RemoteException {
        replica.abortWrite(k,v);
    }

    @Override
    public void connectReplica(ReplicaInterface replica) throws RemoteException {
        this.replica.connectReplica(replica);
    }

    @Override
    public void propagateReplica(ReplicaInterface replica) throws RemoteException {
        this.replica.propagateReplica(replica);
    }

    @Override
    public void receivePropagatedPut(int k, int v) throws RemoteException, InterruptedException {
        replica.receivePropagatedPut(k,v);
    }

    @Override
    public int propagateGet(int k) throws RemoteException {
        return replica.propagateGet(k);
    }

    @Override
    public void propagateReadThreshold(int readThreshold) throws RemoteException {
        replica.propagateReadThreshold(readThreshold);
    }

    @Override
    public void propagateWriteThreshold(int writeThreshold) throws RemoteException {
        replica.propagateWriteThreshold(writeThreshold);
    }
}
