package polimi.ds;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static java.lang.System.exit;

public class ReplicaImplementation implements ReplicaInterface{

    Map<Integer, Collection<Integer>> liveDB = new HashMap<>();
    Map<Integer, Integer> lastWriteRequests = new HashMap<>();
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
        lastWriteRequests.put(k,v);
    }

    public synchronized Integer get(int k) throws RemoteException {
        return lastCommittedValues.get(k);
    }

    public synchronized boolean vote(int k,int v) throws RemoteException{
        return liveDB.containsKey(k) && liveDB.get(k).contains(v)
                && lastWriteRequests.containsKey(k) && lastWriteRequests.get(k) == v;
    }

    @Override
    public Map<Integer, Integer> getAllData() throws RemoteException {
        return new HashMap<>(lastCommittedValues);
    }

    @Override
    public void initData(Map<Integer, Integer> allData) throws RemoteException {
        lastCommittedValues.putAll(allData);
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
        if(v == lastWriteRequests.get(k))
            lastWriteRequests.remove(k);
    }

    @Override
    public synchronized void commitPut(int k, int v) throws RemoteException{
        System.out.println("Received commit command for key "+k+" value "+v);
        liveDB.remove(k);
        lastWriteRequests.remove(k);
        lastCommittedValues.put(k, v);
    }

    public synchronized void printAllValues() throws RemoteException{
        for(Integer k : lastCommittedValues.keySet()){
            System.out.println("("+k+","+lastCommittedValues.get(k)+")\t");
        }
    }

    public static void main(String[] args) throws RemoteException {
        if(args.length < 3){
            System.err.println("Args must contain address and port of coordinator and name to which register the replica");
            return;
        }
        String name = args[2];
        Scanner input = new Scanner(System.in);
        int print;
        ReplicaImplementation replica = new ReplicaImplementation();
        ReplicaInterface replicaInterface = null;
        try {

            Registry registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            stub = (CoordinatorInterface) registry.lookup("CoordinatorService");

            //connection to coordinator
            Registry registry2;
            System.setProperty("java.rmi.server.hostname", Utils.getIP());
            int port = 1099;
            try{
                registry2 = LocateRegistry.createRegistry(port);
            }catch(RemoteException e){
                registry2 = LocateRegistry.getRegistry(port);
            }
            replicaInterface = (ReplicaInterface) UnicastRemoteObject.exportObject(replica,0);
            registry2.bind(name,replicaInterface);

            stub.replicaConnection(name,Utils.getIP(),port);
        }catch(RemoteException e){
            System.err.println("Registry is uninitialized or unavailable");
            exit(1);
        }
        catch(NumberFormatException e){
            System.err.println("Port argument must be a number");
            exit(2);
        }
        catch (NotBoundException e) {
            System.err.println("Coordinator unavailable");
            exit(3);
        } catch (AlreadyBoundException e) {
            System.err.println("Name already present in the registry");
            exit(4);
        }
        do {
                System.err.println("|| I'm a REPLICA || "+"\nAt anytime, press 1 to print all values, 0 to exit");
                print = Integer.parseInt(input.nextLine());
                switch (print){
                    case 1:
                        replica.printAllValues();
                        break;
                    case 0:
                        System.out.println("Disconnecting replica...");
                        stub.disconnectReplica(replicaInterface);
                        break;
                }
            }while(print > 0);
        exit(0);
    }
}
