package polimi.ds;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

/***
 * A replica of the key-value datastore. Every replica can be accessed by the client and communicate with the other replicas to reach quorums.
 * Specifically Read One - Write All policy is implemented to detect if an operation is agreed to be valid or not
 */
public class Replica implements ReplicaInterface, DataStoreInterface{

    private final List<Integer> commitLogForKeyZero;

    private final Coordinator coordinator;

    private final Map<Integer,Integer> datastore;
    private final Map<Integer,KeyStatus> keyStatusMap;
    private final Map<Integer,Integer> lastWriteRequests;

    private final Map<Integer, Integer> numberOfWriteRequestsWaitingForVote;

    public Replica() {
        coordinator = new Coordinator(1,1);
        datastore = new HashMap<>();
        keyStatusMap = new HashMap<>();
        lastWriteRequests = new HashMap<>();
        commitLogForKeyZero = new ArrayList<>();
        numberOfWriteRequestsWaitingForVote = new HashMap<>();
    }

    @Override
    public Integer get(int k) throws RemoteException {
        Integer localVote;
        synchronized (datastore) {
            localVote = datastore.get(k);
        }
        return coordinator.get(k, localVote);
    }

    @Override
    public boolean put(int k, int v) throws RemoteException, InterruptedException {
        System.out.println("Received put request for "+k+"t"+v);
        synchronized (keyStatusMap){
            while(keyStatusMap.get(k) == KeyStatus.WAITING_COMMIT)
                keyStatusMap.wait();
            lastWriteRequests.put(k,v);
        }
        //call coordinator to propagate put
        coordinator.propagatePutRequest(k,v);

        //based on the return of the coordinator commit or abort
        incrementWriteRequestsWaitingVote(k);
        if(coordinator.startVote(k,v, lastWriteRequests.get(k))){
            commitWrite(k,v);
            return true;
        }
        abortWrite(k,v);
        return false;
    }

    @Override
    public int getWriteVote(int k) throws RemoteException {
        synchronized (keyStatusMap){
            incrementWriteRequestsWaitingVote(k);
            return lastWriteRequests.get(k);
        }
    }

    private void incrementWriteRequestsWaitingVote(int k) {
        synchronized (keyStatusMap){
            keyStatusMap.put(k, KeyStatus.WAITING_COMMIT);
            keyStatusMap.notifyAll();
        }
        synchronized (numberOfWriteRequestsWaitingForVote){
            if (!numberOfWriteRequestsWaitingForVote.containsKey(k))
                numberOfWriteRequestsWaitingForVote.put(k, 0);
            int n = numberOfWriteRequestsWaitingForVote.get(k);
            n++;
            numberOfWriteRequestsWaitingForVote.put(k, n);
        }
    }

    @Override
    public void commitWrite(int k, int v) throws RemoteException {
        synchronized (datastore){
            datastore.put(k, v);
        }
        if(k == 0)
            commitLogForKeyZero.add(v);
        decrementWriteRequestsWaitingForVote(k);
    }

    private void decrementWriteRequestsWaitingForVote(int k) {
        synchronized (keyStatusMap){
            synchronized (numberOfWriteRequestsWaitingForVote){
                int n = numberOfWriteRequestsWaitingForVote.get(k);
                n--;
                numberOfWriteRequestsWaitingForVote.put(k, n);
                if(n == 0){
                    keyStatusMap.put(k, KeyStatus.READY);
                    keyStatusMap.notifyAll();
                }
            }
        }
    }

    @Override
    public void abortWrite(int k, int v) throws RemoteException {
        decrementWriteRequestsWaitingForVote(k);
    }

    @Override
    public void connectReplica(ReplicaInterface replica) throws RemoteException {
        coordinator.connectReplica(replica);
    }

    @Override
    public void propagateReplica(ReplicaInterface replica) throws RemoteException {
        coordinator.addReplica(replica);
    }

    @Override
    public void receivePropagatedPut(int k, int v) throws RemoteException, InterruptedException {
        synchronized (keyStatusMap){
            while(keyStatusMap.get(k) == KeyStatus.WAITING_COMMIT)
                keyStatusMap.wait();
            lastWriteRequests.put(k,v);
        }
    }

    @Override
    public int propagateGet(int k) throws RemoteException {
        synchronized (datastore){
            return datastore.get(k);
        }
    }

    @Override
    public void propagateReadThreshold(int readThreshold) throws RemoteException {
        coordinator.setReadThreshold(readThreshold);
    }

    @Override
    public void propagateWriteThreshold(int writeThreshold) throws RemoteException {
        coordinator.setWriteThreshold(writeThreshold);
    }

    void dumpLog(String replicaName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(replicaName+"log.txt"));
        commitLogForKeyZero.forEach(v -> {
            try {
                writer.write(v+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writer.close();
    }

    public void printDataStore() {
        datastore.forEach((k,v)-> System.out.println("("+k+","+v+")\t"));
    }

    public void setReadThreshold(int readThreshold) {
        coordinator.setReadThreshold(readThreshold);
        coordinator.propagateReadThreshold(readThreshold);
    }

    public void setWriteThreshold(int writeThreshold) {
        coordinator.setWriteThreshold(writeThreshold);
        coordinator.propagateWriteThreshold(writeThreshold);
    }

    public int getReadThreshold() {
        return coordinator.getReadThreshold();
    }

    public int getWriteThreshold() {
        return coordinator.getWriteThreshold();
    }
}
