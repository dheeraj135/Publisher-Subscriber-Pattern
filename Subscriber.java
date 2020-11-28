import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Subscriber implements SubscriberInterface {
    String UUID = "";
    String logFile = "log_sub";
    Integer reqCounter = 0;
    ReentrantLock counterLock = new ReentrantLock(true);
    Registry registry;

    private void unsubscribe(String topic, String ReqID) {
        // call server.unregisterSubscriber()
        try {
            ServerInterface server = (ServerInterface) registry.lookup("master");
            server.unregisterSubscriber(topic, UUID, ReqID);
            System.out.println("UnSubscribe @"+topic);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void subscribe(String topic, String ReqID) {
        // call server.registerSubscriber()
        try {
            ServerInterface server = (ServerInterface) registry.lookup("master");
            server.registerSubscriber(topic, UUID, ReqID);
            System.out.println("Subscribe @"+topic);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void receiveData(String topic, Data dt, String ReqID) {
        // receive data from server. This is called by server
        System.out.println("Received @"+topic+" Data: "+dt.getData()+" with reqID: "+ReqID);
        outputToLog(dt.getData());
    }

    private void outputToLog(String log){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("./logs/"+logFile+".txt",true));
            writer.write(log + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void updateLogFileName(String name){
        logFile = name;
        Path path = Paths.get("./logs/"+logFile+".txt"); 
        try { 
            Files.deleteIfExists(path); 
        } 
        catch (IOException e) { 
            e.printStackTrace(); 
        } 
        File logfile = new File("./logs/"+logFile+".txt");
    }

    private void executeCommand(String line) {
        String[] splitStrings = line.split(" ");
        if(splitStrings.length != 2)
            return;
        String reqId = UUID;
        counterLock.lock();
        reqId += reqCounter;
        reqCounter++;
        counterLock.unlock();
        if(splitStrings[0].compareTo("S") == 0)
            subscribe(splitStrings[1], reqId);
        else if(splitStrings[0].compareTo("U") == 0)
            unsubscribe(splitStrings[1], reqId);
        else {
            System.err.println("ERROR: Invalid Command line: "+line);
        }
    }

    public void executeCommandsFromFile(String filename) {
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

    public void takeInputFromCommandLine() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String cmd = scanner.nextLine();
            if (cmd == "exit") {
                scanner.close();
            }
            executeCommand(cmd);
        }
    }

    public Subscriber() {
        UUID = ManagementFactory.getRuntimeMXBean().getName();
        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getUUID() {
        return UUID;
    }

    public void register() {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry();
            registry.rebind(UUID, this);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public static void main(String[] args) throws RemoteException {
        Subscriber obj = new Subscriber();
        // Create object, bind to UUID and call executeCommandsFromFile();
        SubscriberInterface robj = (SubscriberInterface) UnicastRemoteObject.exportObject(obj,0);
        robj.register();
        if (args.length == 1){
            robj.executeCommandsFromFile(args[0]);
        } else if (args.length == 2) {
            robj.updateLogFileName(args[1]);
            robj.takeInputFromCommandLine();
        } else {
            robj.takeInputFromCommandLine();
        }
    }
}