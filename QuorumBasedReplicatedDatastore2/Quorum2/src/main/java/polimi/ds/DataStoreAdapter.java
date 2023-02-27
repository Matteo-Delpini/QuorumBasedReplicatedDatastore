package polimi.ds;

import java.rmi.RemoteException;

public class DataStoreAdapter implements DataStoreInterface{

    private final Replica replica;

    public DataStoreAdapter(Replica replica) {
        this.replica = replica;
    }

    @Override
    public Integer get(int k) throws RemoteException {
        return replica.get(k);
    }

    @Override
    public boolean put(int k, int v) throws RemoteException, InterruptedException {
        return replica.put(k,v);
    }
}
