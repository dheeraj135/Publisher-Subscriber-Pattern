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
    static Integer reqCounter = 0;
    static ReentrantLock counterLock = new ReentrantLock(true);

    private static void publish(String topic, Data dt, String ReqID)
            throws AccessException, RemoteException, NotBoundException {

        System.out.println(topic+ dt.getData()+ReqID);
        registry = LocateRegistry.getRegistry();
        ServerInterface server = (ServerInterface) registry.lookup("master");
        System.out.println(server);
        server.sendToSubscribers(topic, dt, ReqID);
    }
    private static void outputToLog(String log) {

    }
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

    public static String getUUID() {
        return UUID;
    }
    public static void main(String[] args) {
        UUID = ManagementFactory.getRuntimeMXBean().getName();
        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        executeCommandsFromFile(args[0]);
    }
}