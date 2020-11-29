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

/*
 *  Server Class - This class is the manager server b/w the publishers and subscribers.
 * 
 *  This class have 2 modes of working,
 *  a. As Master - When the class is working as master, it receives requests and data from publishers and subscribers.
 *                  Master also propogates requests to the registered slave. This makes sure both the master and slave have
 *                  same topicSubscriberList HashMap.
 *  b. As Slave - When the class is working as slave, it receives requests from the master. When slave starts up, it does a bulk-
 *                  transfer to sync the topicSubscriberList, after that requests are received incrementally.
 *              
 */

public class Server implements ServerInterface{
    /*
     * topicSubscriberList - Maintain RMI ids of subscribers registered to topics.
     * reentrantReadWriteLock - Used to lock the topicSubscriberList.
     */
    HashMap<String, Set<String> > topicSubscriberList;
    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public Server() {
        topicSubscriberList = new HashMap<>();
    }

    /*
     *  amIUp()     -   This skeleton function is called by the Slave to make sure that master is working or not.
     * 
     *  isMasterUp()-   Get stub for master server and try calling the amIUp(). If the function executes then
     *                  the master server is up.
     */
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

    /*
     *  becomeMaster()  -   This function binds the current object to "master" key in the RMI registry.
     * 
     *  becomeSlave()   -   This function first sync the topicSubscriberList with master and then binds current object 
     *                      to the "slave" key in the RMI registry.
     */
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

    /*
     *  startNewSlave() -   Boots up a new Server jvm and redirects the input, output and error streams to the original terminal.
     */
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

    /*
     *  (un)subscribeToSlave(topic, UUID, ReqID) - Sends the (un)subscribe request to the slave.
     */

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
    /*
     *  syncWithSlave() - Returns the topicSubscriberList of master.
     */
    public HashMap<String,Set<String> > syncWithSlave() {
        return topicSubscriberList;
    }

    public void lockMaster() {
        reentrantReadWriteLock.writeLock().lock();
    }
    
    public void unlockMaster() {
        reentrantReadWriteLock.writeLock().unlock();
    }
    
    /*
     *  sendToSubscribers(topic, dt, ReqID) - This function is called by the publisher to send the data to the subscribers.
     */
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

        // Iterate over all the subscribers of this topic and send the data.
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

    /*
     *  outputToLog(str)   -   This function appends str to the log file of the server.
     */
    public void outputToLog(String str) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./logs/server.txt",true));
        writer.write(str+ "\n");
        writer.flush();
        writer.close();
    }

    /*
     *  createLogFile() -   Deletes old log file and creates a blank file.
     */
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
    
    /*
     *  __(un)registerSubscriber(topic, UUID, ReqID)    -   Directly changes the topicSubscriberList based on the request.
     *
     *  (un)registerSubscriber(topic, UUID, ReqID)  -   Do the changes locally and propogate the changes to the slave.
     */
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

    /*
     *  printTopicList()    -   Simply print the current topicSubscriber List.
     */
    public void printTopicList() {
        for (String ls: topicSubscriberList.keySet()) {
            System.out.println(ls+" "+topicSubscriberList.get(ls));
        }
    }
    
    public static void main(String[] args) throws InterruptedException, RemoteException {
        Server sobj = new Server();
        ServerInterface rmobj = (ServerInterface) UnicastRemoteObject.exportObject(sobj, 0);
        sobj.createLogFile();
        rmobj.becomeSlave();    //  Start the server as a slave.

        System.out.println("Slave Server: ");
        rmobj.printTopicList(); //  Prints the initial topicSubsriberList of slave.
        System.out.println("I am Slave Now!");

        System.out.println("Waiting for master to go down.");
        while(rmobj.isMasterUp()) {     // Poll the master every 0.5 secs.
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