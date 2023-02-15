package polimi.ds;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ReplicaImplementation implements ReplicaInterface{

    Map<Integer, Collection<Integer>> liveDB = new HashMap<>();
    Map<Integer,Integer> lastCommittedValues = new HashMap<>();
    static CoordinatorInterface stub;

    protected ReplicaImplementation() throws RemoteException {
    }

    public synchronized void put(int k, int v)  throws RemoteException {
        Collection<Integer> candidateValues = liveDB.get(k);
        if(candidateValues == null)
            candidateValues = new ArrayList<>();
        if(!candidateValues.contains(v))
            candidateValues.add(v);
        liveDB.put(k,candidateValues);
    }

    public synchronized Integer get(int k) throws RemoteException {
        return lastCommittedValues.get(k);
    }

    public synchronized boolean vote(int k,int v) throws RemoteException{
        return liveDB.containsKey(k) && liveDB.get(k).contains(v);
    }

    @Override
    public synchronized void abortPut(int k, int v) throws RemoteException{
        System.out.println("Received abort command for key "+k+" value "+v);
        Collection<Integer> candidateValues = liveDB.remove(k);
        if(candidateValues == null || candidateValues.isEmpty())
            return;
        candidateValues.remove(v);
        if(!candidateValues.isEmpty())
            liveDB.put(k,candidateValues);
    }

    @Override
    public synchronized void commitPut(int k, int v) throws RemoteException{
        System.out.println("Received commit command for key "+k+" value "+v);
        liveDB.remove(k);
        lastCommittedValues.put(k, v);
    }

    public synchronized void printAllValues() throws RemoteException{
        for(Integer k : lastCommittedValues.keySet()){
            System.out.println(lastCommittedValues.get(k)+" -");
        }
    }

    public static void main(String[] args) throws RemoteException {
        if(args.length < 2){
            System.err.println("Args must contain address and port of coordinator");
            return;
        }

        Scanner input = new Scanner(System.in);
        int print;
        ReplicaImplementation replica = new ReplicaImplementation();
        try {

            Registry registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            ReplicaInterface replicaInterface = (ReplicaInterface) UnicastRemoteObject.exportObject(replica,0);
            stub = (CoordinatorInterface) registry.lookup("CoordinatorService");
            stub.replicaConnection(replicaInterface);
        }catch(RemoteException e){
            System.err.println("Registry is uninitialized or unavailable");
            return;
        }
        catch(NumberFormatException e){
            System.err.println("Port argument must be a number");
            return;
        }
        catch (NotBoundException e) {
            System.err.println("Coordinator unavailable");
            return;
        }
        do {
                System.out.println("|| I'm a REPLICA || "+"\nPress 1 to print all values, 0 to exit");
                print = Integer.parseInt(input.nextLine());
                switch (print){
                    case 1:
                        replica.printAllValues();
                        break;
                    case 0:
                        System.out.println("Disconnecting replica...");
                        break;
                }
            }while(print > 0);
    }

}
