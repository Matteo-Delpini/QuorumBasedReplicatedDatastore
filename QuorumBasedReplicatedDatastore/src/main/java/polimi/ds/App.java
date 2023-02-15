package polimi.ds;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    private static Scanner input = new Scanner(System.in);
    private static CoordinatorInterface stub;
    public static void main( String[] args )
    {
        try{
            Registry registry = LocateRegistry.getRegistry("localhost",9395);
            stub = (CoordinatorInterface) registry.lookup("CoordinatorService");
            int menuChoice;
            do {
                menuChoice = menu();
                switch (menuChoice){
                    case 1:
                        get();
                        break;
                    case 2:
                        post();
                        break;
                    case 0:
                        System.out.println("Exiting...");
                        break;
                }
            }while(menuChoice > 0);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static int menu() {
        System.out.println("1. get by key" +
                "\n2. post by key" +
                "\n0. exit");
        return Integer.parseInt(input.nextLine());
    }

    private static void get(){
        System.out.println("Please insert an integer value for the key:");
        int k = Integer.parseInt(input.nextLine());
        try {
            System.out.println("Value: "+stub.get(k));
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
            System.out.println("Server says: "+stub.put(k,value));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

}
