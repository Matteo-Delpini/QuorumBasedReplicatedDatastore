package polimi.ds;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
public class Client
{
    private static final int port = 1099;
    private static final Scanner input = new Scanner(System.in);
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
            int menuChoice;
            do {
                menuChoice = menu();
                switch (menuChoice) {
                    case 1 -> get();
                    case 2 -> post();
                    case 0 -> System.out.println("Exiting...");
                }
            }while(menuChoice > 0);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static int menu() {
        System.out.println("""
                1. get by key
                2. post by key
                0. exit""");
        try{
            int response = Integer.parseInt(input.nextLine());
            return response >= 0 ? response : 4;
        }catch(NumberFormatException e){
            return 4;
        }
    }

    private static void get(){
        System.out.println("Please insert an integer value for the key:");
        int k = Integer.parseInt(input.nextLine());
        try {
            System.out.println("Value: "+dataStore.get(k));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static void post(){
        System.out.println("Please insert a key:");
        int k = Integer.parseInt(input.nextLine());
        System.out.println("Please insert a value:");
        int value = Integer.parseInt(input.nextLine());
        try {
            System.out.println("Server says: "+dataStore.put(k,value));
        } catch (RemoteException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
