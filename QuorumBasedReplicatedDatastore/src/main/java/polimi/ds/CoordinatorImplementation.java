package polimi.ds;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static java.lang.System.exit;

public class CoordinatorImplementation implements CoordinatorInterface {

    private final Collection<ReplicaInterface> connectedReplicas;
    private Integer readThreshold;
    private Integer writeThreshold;

    protected CoordinatorImplementation() throws RemoteException {
        connectedReplicas = new ArrayList<>();
    }

    protected CoordinatorImplementation(int readThreshold,int writeThreshold) throws RemoteException {
        this();
        this.readThreshold = readThreshold;
        this.writeThreshold = writeThreshold;
    }

    @Override
    public void replicaConnection(String name, String addr, int port) throws RemoteException, NotBoundException {
        Registry replicaRegistry = LocateRegistry.getRegistry(addr,port);
        ReplicaInterface replicaInterface = (ReplicaInterface) replicaRegistry.lookup(name);
        if(!connectedReplicas.contains(replicaInterface)) {
            //duplicate data to new replica
            connectedReplicas.stream().findFirst().ifPresent(alreadyConnectedReplica -> {
                try {
                    replicaInterface.initData(alreadyConnectedReplica.getAllData());
                } catch (RemoteException e) {
                    //should never get here
                    throw new RuntimeException(e);
                }
            });
            //add to connected replicas
            connectedReplicas.add(replicaInterface);
            System.err.println("Connected replicas are now "+connectedReplicas.size());
        }
    }

    @Override
    public boolean put(int k, int v) throws RemoteException{
        System.out.println("Received client request put("+k+","+v+")");

        //iterate on every replica calling put method
        for(ReplicaInterface replicaInterface : connectedReplicas){
            replicaInterface.put(k,v);
        }
        //get the last written value from every replica

        Integer votes = getVotes(k,v);

        //check that the most voted value satisfies threshold, otherwise abort
        if (votes < writeThreshold){
            System.out.println("Aborting request put("+k+","+v+")");
            for (ReplicaInterface replicaInterface : connectedReplicas)
                replicaInterface.abortPut(k,v);
            return false;
        }
        System.out.println("Committing request put("+k+","+v+")");
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
        System.out.println("Received client request get("+k+")");
        //iterate on every replica and get their vote
        Map<Integer, Integer> valueVotes = new HashMap<>();
        Integer candidate = getMostReadVoted(k,valueVotes);

        //check if read threshold is reached, if not return null
        if(candidate == null || valueVotes.get(candidate) < readThreshold)
            return null;

        //return most voted item
        return candidate;
    }

    @Override
    public void disconnectReplica(ReplicaInterface replica) throws RemoteException{
        System.err.println("Received disconnect request from replica");
        connectedReplicas.remove(replica);
        System.err.println("Connected replicas are now "+connectedReplicas.size());
    }

    public static void main(String[] args){
        if(args.length < 1){
            System.err.println("Args must contain port of coordinator");
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
            String localAddress = Utils.getIP();
            System.setProperty("java.rmi.server.hostname", localAddress);
            CoordinatorImplementation coordinatorImplementation = new CoordinatorImplementation(1,1);
            CoordinatorInterface coordinatorInterface = (CoordinatorInterface) UnicastRemoteObject.exportObject(coordinatorImplementation,0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("CoordinatorService",coordinatorInterface);
            System.err.println("Server ready on ip "+ localAddress +" port "+port);
            menu(coordinatorImplementation);
        } catch (RemoteException e) {
            System.err.println("Could not publish coordinator service");
        } catch (AlreadyBoundException e) {
            System.err.println("There is already a coordinator in the network");
        }
    }


    public Integer getReadThreshold() {
        return readThreshold;
    }

    public void setReadThreshold(Integer readThreshold) {
        this.readThreshold = readThreshold;
    }

    public Integer getWriteThreshold() {
        return writeThreshold;
    }

    public void setWriteThreshold(Integer writeThreshold) {
        this.writeThreshold = writeThreshold;
    }

    private static void menu(CoordinatorImplementation coordinator){
        Scanner input = new Scanner(System.in);
        int choice;
        do{
            System.out.println("CURRENT READ THRESHOLD: "+coordinator.getReadThreshold()+"" +
                    "\nCURRENT WRITE THRESHOLD: "+coordinator.getWriteThreshold());
            System.out.println("At anytime, press 1 to change thresholds, 0 to exit");
            choice = Integer.parseInt(input.nextLine());
            switch (choice){
                case 1:
                    int r,w;
                try{
                    System.out.println("Please insert read threshold:");
                    r = Integer.parseInt(input.nextLine());
                    System.out.println("Please insert write threshold:");
                    w = Integer.parseInt(input.nextLine());
                    coordinator.setReadThreshold(r);
                    coordinator.setWriteThreshold(w);
                }catch(NumberFormatException e){
                    System.out.println("Thresholds must be numbers");
                }
                    break;
                case 0:
                    System.out.println("Exiting...");
                    break;
            }
        }while(choice != 0);
        exit(0);
    }
}
