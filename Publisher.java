import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.rmi.registry.LocateRegistry;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/*
 * Publisher class is a simple class because it will not be binded to
 * any RMI id. Thus, we can make the entire class static.
 */
public class Publisher {
    static String UUID = "";
    static String logFile = "";
    static Registry registry;

    /*
     *  reqCounter  -   Request Counter, to generate unique request Ids.
     *  counterLock -   To lock the reqCounter.
     */

    static Integer reqCounter = 0;
    static ReentrantLock counterLock = new ReentrantLock(true);

    /*
     *  publish(topic, dt, ReqID) - Publish dt to the subscribers of topic.
     */

    private static void publish(String topic, Data dt, String ReqID)
            throws AccessException, RemoteException, NotBoundException {

        System.out.println(topic+ dt.getData()+ReqID);
        registry = LocateRegistry.getRegistry();
        ServerInterface server = (ServerInterface) registry.lookup("master");
        System.out.println(server);
        server.sendToSubscribers(topic, dt, ReqID);
    }

    // private static void outputToLog(String log) {

    // }

    /*
     *  executeCommand(String line) -   Extract and execute the command from line.
     */
    static void executeCommand(String line) {
        String[] splitStrings = line.split(" ");
        if(splitStrings.length != 2)
            return;
        Data dt = new Data();
        dt.setStringData(splitStrings[1]);
        String reqId = UUID;
        counterLock.lock();
        reqId += reqCounter;
        reqCounter++;
        counterLock.unlock();
        try {
            publish(splitStrings[0], dt, reqId);
        } catch (AccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotBoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     *  executeCommandsFromFile(filename)   -   Read the filename file and execute commands from this file line by line.
     */
    static void executeCommandsFromFile(String filename) {
        File testfile = new File(filename);
        Scanner reader;
        try {
            reader = new Scanner(testfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            executeCommand(line);
        }
        reader.close();
    }

    /*
     *  takeInputFromCommandLine()      -   Take input from terminal and execute commands line by line.
     */
    static void takeInputFromCommandLine() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String cmd = scanner.nextLine();
            if (cmd == "exit") {
                scanner.close();
            }
            executeCommand(cmd);
        }
    }


    public static String getUUID() {
        return UUID;
    }

    /*
     *  main() - 
     *  
     *  UUID is the unique identifier of JVM, usually it is of the form pid@hostname.
     */

    public static void main(String[] args) {
        UUID = ManagementFactory.getRuntimeMXBean().getName();
        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (args.length == 1) {
            executeCommandsFromFile(args[0]);
        } else {
            takeInputFromCommandLine();
        }
    }
}