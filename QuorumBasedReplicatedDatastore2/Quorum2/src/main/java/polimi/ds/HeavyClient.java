package polimi.ds;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

public class HeavyClient {
    private static final int port = 1099;
    private static final int numberOfRequests = 10000;
    private static DataStoreInterface dataStore;

    public static void main( String[] args )
    {
        if(args.length < 2){
            System.err.println("Args must contain address and name of replica");
            return;
        }
        try{
            Registry registry = LocateRegistry.getRegistry(args[0],port);
            dataStore = (DataStoreInterface) registry.lookup(args[1]+"Client");
        }catch(RemoteException e){
            System.err.println("Registry is uninitialized or unavailable");
            return;
        }
        catch (NotBoundException e) {
            System.err.println("Cannot find replica at address"+args[0]+" with name "+args[1]);
            return;
        }
        try{
            Random random = new Random();
            for (int i = 0; i < numberOfRequests; i++) {
                post(0,random.nextInt());
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    private static void post(int k, int v){
        try {
            dataStore.put(k,v);
        } catch (RemoteException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
