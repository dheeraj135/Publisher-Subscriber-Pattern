import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.rmi.registry.LocateRegistry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class Server implements ServerInterface{
    HashMap<String, Set<String> > topicSubscriberList;
    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public Server() {
        topicSubscriberList = new HashMap<>();
    }

    public boolean amIUp() {
        return true;
    }

    public boolean isMasterUp() {
        // Get slave instance from RMI
        try {  
            // Get the registry 
            Registry registry = LocateRegistry.getRegistry(); 
            // Look up the registry for the remote object 
            try {
                ServerInterface stub = (ServerInterface) registry.lookup("master");
                return stub.amIUp();
            } catch(NotBoundException e) {
                return false;
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString()); 
            e.printStackTrace(); 
        }
        return false;
    }
    public int becomeMaster() {

        try { 
            // Export the remote object to the stub
            // ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);  
            // Bind the remote object (stub) in the registry 
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("master", this);
            return 0;
        } catch (Exception e) { 
            System.err.println("Server exception: " + e.toString()); 
            e.printStackTrace(); 
        }
        return -1;
    }
    public void becomeSlave() {
        Registry registry;
        try{
            registry = LocateRegistry.getRegistry(); 
            ServerInterface server = (ServerInterface) registry.lookup("master");
            topicSubscriberList = server.syncWithSlave();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try { 
            // Export the remote object to the stub
            // ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);
            // Bind the remote object (stub) in the registry
            registry = LocateRegistry.getRegistry(); 
            registry.rebind("slave", this);
        } catch (Exception e) { 
            System.err.println("Server exception: " + e.toString()); 
            e.printStackTrace(); 
        }
    }
    private void startNewSlave() {
        String[] args = new String[] { "java", "Server"};
        ProcessBuilder pb = new ProcessBuilder(args);
        try {
            pb.redirectOutput(Redirect.INHERIT);
            pb.redirectError(Redirect.INHERIT);
            pb.redirectInput(Redirect.INHERIT);     // CTRL + C brings everything down and we don't have to kill all processes manually.
            pb.start();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    private void subscribeToSlave(String topic, String UUID, String ReqID) {
        // Get slave instance from RMI
        try {  
            // Get the registry 
            Registry registry = LocateRegistry.getRegistry(); 
            // Look up the registry for the remote object 
            ServerInterface stub = (ServerInterface) registry.lookup("slave");
            // Pass a message to the remote object
            stub.__registerSubscriber(topic, UUID, ReqID);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString()); 
            e.printStackTrace(); 
        }
    }
    private void unsubscribeToSlave(String topic, String UUID, String ReqID) {
        // Get slave instance from RMI
        try {  
            // Get the registry 
            Registry registry = LocateRegistry.getRegistry(); 
            // Look up the registry for the remote object 
            ServerInterface stub = (ServerInterface) registry.lookup("slave");
            // Pass a message to the remote object
            stub.__unregisterSubscriber(topic, UUID, ReqID);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString()); 
            e.printStackTrace(); 
        }
    }
    public void lockMaster() {
        reentrantReadWriteLock.writeLock().lock();
    }
    public HashMap<String,Set<String> > syncWithSlave() {
        return topicSubscriberList;
    }
    public void unlockMaster() {
        reentrantReadWriteLock.writeLock().unlock();
    }
    
    public void sendToSubscribers(String topic, Data dt, String ReqID) {
        // Acquire lock on topicSubsriberList
        reentrantReadWriteLock.readLock().lock();
        Set <String> topicSubscribers = topicSubscriberList.get(topic);
        reentrantReadWriteLock.readLock().unlock();
        
        try {
            outputToLog("P " + topic + " " + dt.getData());
        } catch(IOException e){
            e.printStackTrace();
        }
        if(topicSubscribers == null)
            return;
        for (String sub: topicSubscribers) {
            try {  
                Registry registry = LocateRegistry.getRegistry(); 
                SubscriberInterface stub = (SubscriberInterface) registry.lookup(sub);
                stub.receiveData(topic, dt, ReqID);
            } catch (Exception e) {
                System.err.println("Client exception: " + e.toString()); 
                e.printStackTrace();
            }
        }
    }

    public void outputToLog(String str) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./logs/server.txt",true));
        writer.write(str+ "\n");
        writer.flush();
        writer.close();
    }

    public void createLogFile(){
        Path path = Paths.get("./logs/server.txt"); 
        try { 
            Files.deleteIfExists(path); 
        } 
        catch (IOException e) { 
            e.printStackTrace(); 
        } 
        File newFile = new File("./logs/server.txt");
    }
         
    public void __registerSubscriber(String topic, String UUID, String ReqID) {
        // Update the hashmap
        if (!topicSubscriberList.containsKey(topic)) {
            topicSubscriberList.put(topic, new HashSet<String>());
        }
        topicSubscriberList.get(topic).add(UUID);
        System.out.println("Topic: "+topic +" UUID: "+UUID+" ReqID: "+ReqID);
        printTopicList();
    }
    public void registerSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        reentrantReadWriteLock.writeLock().lock();
        try {
            // Send this data to slave
            __registerSubscriber(topic, UUID, ReqID);
            try {
                outputToLog("S " + UUID + " " + topic);
            } catch(IOException e){
                e.printStackTrace();
            }
        } finally {
            // Release lock
            reentrantReadWriteLock.writeLock().unlock();
            subscribeToSlave(topic, UUID, ReqID);
        }
    }
    public void __unregisterSubscriber(String topic, String UUID, String ReqID) {
        // Update the hashmap
        if (topicSubscriberList.containsKey(topic)) {
            topicSubscriberList.get(topic).remove(UUID);
            if (topicSubscriberList.get(topic).size()==0){
                topicSubscriberList.remove(topic);
            }
        }
        System.out.println("Topic: "+topic +" UUID: "+UUID+" ReqID: "+ReqID);
        printTopicList();
    }
    public void unregisterSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        reentrantReadWriteLock.writeLock().lock();
        try {
            // Send this data to slave
            __unregisterSubscriber(topic, UUID, ReqID);
            try {
                outputToLog("U " + UUID + " " + topic);
            } catch(IOException e){
                e.printStackTrace();
            }
        } finally {
            // Release lock
            reentrantReadWriteLock.writeLock().unlock();
            unsubscribeToSlave(topic, UUID, ReqID);
        }
    }
    public void printTopicList() {
        for (String ls: topicSubscriberList.keySet()) {
            System.out.println(ls+" "+topicSubscriberList.get(ls));
        }
    }
    
    public static void main(String[] args) throws InterruptedException, RemoteException {
        Server sobj = new Server();
        ServerInterface rmobj = (ServerInterface) UnicastRemoteObject.exportObject(sobj, 0);
        sobj.createLogFile();
        rmobj.becomeSlave();
        System.out.println("Slave Server: ");
        rmobj.printTopicList();
        System.out.println("I am Slave Now!");
        System.out.println("Waiting for master to go down.");
        while(rmobj.isMasterUp()) {
            // System.out.println("Going to sleep!");
            Thread.sleep(500);
        }
        System.out.println("Becoming Master");
        if(rmobj.becomeMaster() != 0 ) {
            System.out.println("Unable to become master. I will not start a slave.");
            System.out.println("Dying!");
            return;
        }
        System.out.println("I am master now. Starting new slave.");
        sobj.startNewSlave();
        System.out.println("Dying!");
    }
}