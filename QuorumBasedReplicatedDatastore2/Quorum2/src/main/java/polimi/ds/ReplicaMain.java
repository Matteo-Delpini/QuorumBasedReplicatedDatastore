package polimi.ds;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

import static java.lang.System.exit;

public class ReplicaMain {

    private static final int port = 1099;

    public static void main(String[] args) throws IOException {
        if(args.length < 1 || args.length == 2){
            System.err.println("Arguments must be <replica_name> [<access_replica_address> <access_replica_name>]");
            exit(1);
        }

        Replica replica = new Replica();
        Registry registry;
        String name = args[0];
        String hostAddress = Utils.getIP();

        System.setProperty("java.rmi.server.hostname", hostAddress);
        try{
            registry = LocateRegistry.createRegistry(port);
        }catch(RemoteException e){
            try {
                registry = LocateRegistry.getRegistry(port);
            } catch (RemoteException ex) {
                System.err.println("Registry cannot be initialized");
                return;
            }
        }

        ReplicaInterface replicaInterface = null;
        DataStoreInterface dataStoreInterface = null;
        try {
            replicaInterface = (ReplicaInterface) UnicastRemoteObject.exportObject(new ReplicaAdapter(replica),0);
            dataStoreInterface = (DataStoreInterface) UnicastRemoteObject.exportObject(new DataStoreAdapter(replica),0);
        } catch (RemoteException e) {
            System.err.println("Cannot export replica");
            exit(1);
        }

        try {
            registry.bind(name + "BackEnd", replicaInterface);
            if (args.length >= 3)
                initializeCopyReplica(replicaInterface, args[1], args[2]);
            else
                setThresholds(replica);

            registry.bind(name + "Client", dataStoreInterface);
        }catch (RemoteException e){
            System.err.println("Cannot bind interfaces");
            exit(1);
        } catch (AlreadyBoundException e) {
            System.err.println("Name " + name + " is already in use on this host");
            exit(1);
        }

        System.out.println("Started replica listening on "+hostAddress+ " with name "+ name);
        int r = replica.getReadThreshold();
        int w = replica.getWriteThreshold();
        System.out.println("Read threshold: "+r+"\tWrite threshold: "+w);
        System.out.println("Ensure that the number of replicas is at least "+Math.max(w,r)+" and at most "+Math.min(w*2-1,r+w-1)+ " to get a correct functioning of the algorithm");
        System.out.println();

        System.out.println("At anytime, press 1 to print all values, 2 to dump commit log, 0 to exit");

        menu(replica,name);
        exit(0);
    }

    private static void setThresholds(Replica replica) {
        int readThreshold=-1, writeThreshold=-1;
        Scanner input =new Scanner(System.in);
        while(readThreshold <= 0 || writeThreshold <= 0){
            try{
                System.out.println("Insert read threshold");
                readThreshold = Integer.parseInt(input.nextLine());
                System.out.println("Insert write threshold");
                writeThreshold = Integer.parseInt(input.nextLine());
                if(readThreshold <= 0 || writeThreshold <= 0)
                    System.err.println("Thresholds must be strictly positive, please reinsert");
            }catch (NumberFormatException e){
                System.err.println("Thresholds must be integer numbers, please reinsert");
                readThreshold = -1;
                writeThreshold = -1;
            }
        }
        replica.setReadThreshold(readThreshold);
        replica.setWriteThreshold(writeThreshold);
    }

    private static void menu(Replica replica,String name) throws IOException {
        int resp = 1;
        Scanner input =new Scanner(System.in);

        while(resp != 0){
            resp = Integer.parseInt(input.nextLine());
            switch (resp){
                case 1:
                    replica.printDataStore();
                    break;
                case 2:
                    replica.dumpLog(name);
                case 0:
                    break;
            }
        }
    }

    private static void initializeCopyReplica(ReplicaInterface replicaInterface, String ip, String accessReplicaName){
        try {
            Registry registry = LocateRegistry.getRegistry(ip,port);
            ReplicaInterface accessReplica = (ReplicaInterface) registry.lookup(accessReplicaName+"BackEnd");
            accessReplica.connectReplica(replicaInterface);
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Cannot locate access replica at address "+ip+" with name "+accessReplicaName);
            exit(2);
        }
    }
}
