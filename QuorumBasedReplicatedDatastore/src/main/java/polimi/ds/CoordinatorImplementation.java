package polimi.ds;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CoordinatorImplementation extends UnicastRemoteObject implements CoordinatorInterface {

    Collection<ReplicaInterface> connectedReplicas;
    Integer readThreshold;
    Integer writeThreshold;

    protected CoordinatorImplementation() throws RemoteException {
        connectedReplicas = new ArrayList<>();
    }

    protected CoordinatorImplementation(int readThreshold,int writeThreshold) throws RemoteException {
        this();
        this.readThreshold = readThreshold;
        this.writeThreshold = writeThreshold;
    }

    @Override
    public void replicaConnection(ReplicaInterface replicaInterface) throws RemoteException{
        if(!connectedReplicas.contains(replicaInterface))
            connectedReplicas.add(replicaInterface);
    }

    @Override
    public boolean put(int k, int v) throws RemoteException{
        //iterate on every replica calling put method
        for(ReplicaInterface replicaInterface : connectedReplicas){
            replicaInterface.put(k,v);
        }
        //get the last written value from every replica

        Integer votes = getVotes(k,v);

        //check that the most voted value satisfies threshold, otherwise abort
        if (votes < writeThreshold){
            for (ReplicaInterface replicaInterface : connectedReplicas)
                replicaInterface.abortPut(k,v);
            return false;
        }
        for (ReplicaInterface replicaInterface : connectedReplicas)
            replicaInterface.commitPut(k,v);
        return true;
    }

    private Integer getVotes(int k, int v) throws RemoteException {
        int i = 0;
        for (ReplicaInterface replicaInterface : connectedReplicas){
            i += (replicaInterface.vote(k,v)) ? 1 : 0;
        }
        return i;
    }

    private Integer getMostReadVoted(int k, Map<Integer, Integer> valueVotes) throws RemoteException{
        for (ReplicaInterface replicaInterface : connectedReplicas){
            Integer value = replicaInterface.get(k);
            if(!valueVotes.containsKey(value))
                valueVotes.put(value,0);
            valueVotes.put(value,valueVotes.get(value)+1);
        }

        //extract the item with the most votes
        Integer candidate = null;
        for(Integer value : valueVotes.keySet()){
            if(candidate == null || valueVotes.get(candidate) < valueVotes.get(value))
                candidate = value;
        }
        return candidate;
    }

    @Override
    public Integer get(int k) throws RemoteException {
        //iterate on every replica and get their vote
        Map<Integer, Integer> valueVotes = new HashMap<>();
        Integer candidate = getMostReadVoted(k,valueVotes);

        //check if read threshold is reached, if not return null
        if(candidate == null || valueVotes.get(candidate) < readThreshold)
            return null;

        //return most voted item
        return candidate;
    }

    public static void main(String[] args){
        if(args.length < 1){
            System.err.println("Args must contain address and port of coordinator");
            return;
        }
        int port;
        try{
            port = Integer.parseInt(args[0]);
        }catch (NumberFormatException e){
            System.err.println("Port argument must be a number");
            return;
        }
        try {
            CoordinatorImplementation coordinatorImplementation = new CoordinatorImplementation(1,2);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("CoordinatorService",coordinatorImplementation);
        } catch (RemoteException e) {
            System.err.println("Could not publish coordinator service");
        }
    }
}
