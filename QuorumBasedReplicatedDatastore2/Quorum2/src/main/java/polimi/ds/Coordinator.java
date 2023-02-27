package polimi.ds;

import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Coordinator {
    private int readThreshold;
    private int writeThreshold;

    private final Collection<ReplicaInterface> connectedReplicas;

    public Coordinator(int initialReadThreshold,int initialWriteThreshold) {
        readThreshold = initialReadThreshold;
        writeThreshold = initialWriteThreshold;
        connectedReplicas = new ArrayList<>();
    }

    public void connectReplica(ReplicaInterface replica) throws RemoteException {
        if(replica == null || connectedReplicas.contains(replica))
            return;

        //set new replica thresholds
        replica.propagateReadThreshold(readThreshold);
        replica.propagateWriteThreshold(writeThreshold);

        //send replica reference to other replicas
        connectedReplicas.parallelStream().forEach(r-> {
            try {
                r.propagateReplica(replica);
                replica.propagateReplica(r);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        //add replica to local list
        addReplica(replica);
    }

    public boolean startVote(int k, int v, int localVote) {
        //get votes
        long votes = connectedReplicas.parallelStream().map(r-> {
            try {
                return r.getWriteVote(k);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }).filter(vote -> vote == v).count();

        if(localVote == v)
            votes++;

        if(votes >= writeThreshold){
            connectedReplicas.parallelStream().forEach(r-> {
                try {
                    r.commitWrite(k,v);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        }
        connectedReplicas.parallelStream().forEach(r-> {
            try {
                r.abortWrite(k,v);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        return false;
    }

    public void addReplica(ReplicaInterface replica) {
        connectedReplicas.add(replica);
    }

    public void propagatePutRequest(int k, int v) {
        connectedReplicas.parallelStream().forEach(r-> {
            try {
                r.receivePropagatedPut(k,v);
            } catch (RemoteException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Integer get(int k, Integer localVote) {
        if(readThreshold == 1 && writeThreshold == connectedReplicas.size()+1)
            return localVote;//read one write all case

        //propagate get
        Map<Integer, Long> votes = connectedReplicas.parallelStream().map(r-> {
                    try {
                        return r.propagateGet(k);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.groupingBy(Function.identity(),Collectors.counting()));

        //add localVote to the count
        if(!votes.containsKey(localVote))
            votes.put(localVote,0L);
        votes.put(localVote,votes.get(localVote)+1);

        //get max voted
        Integer candidate = Collections.max(votes.keySet(), Comparator.comparingLong(votes::get));
        if(votes.get(candidate) >= readThreshold)
            return candidate;
        return null;
    }

    public void setReadThreshold(int readThreshold) {
        this.readThreshold = readThreshold;
    }

    public void propagateReadThreshold(int readThreshold) {
        connectedReplicas.parallelStream().forEach(r-> {
            try {
                r.propagateReadThreshold(readThreshold);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void setWriteThreshold(int writeThreshold) {
        this.writeThreshold = writeThreshold;
    }

    public void propagateWriteThreshold(int writeThreshold) {
        connectedReplicas.parallelStream().forEach(r-> {
            try {
                r.propagateWriteThreshold(writeThreshold);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public int getReadThreshold() {
        return readThreshold;
    }

    public int getWriteThreshold() {
        return writeThreshold;
    }
}
