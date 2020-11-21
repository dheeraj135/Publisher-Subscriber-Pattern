import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.rmi.registry.LocateRegistry;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class Server implements Remote{
    HashMap<String, List<String> > topicSubscriberList;
    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    
    // Create a lock
    private boolean isMasterUp() {
        return false;
    }
    private void becomeMaster() {

    }
    private void startNewSlave() {

    }
    private void subscribeToSlave(String topic, String UUID, String ReqID) {
        // Get slave instance from RMI
        try {  
            // Get the registry 
            Registry registry = LocateRegistry.getRegistry(); 
            // Look up the registry for the remote object 
            Server stub = (Server) registry.lookup("slave");
            // Pass a message to the remote object
            stub.registerSubscriber(topic, UUID, ReqID);
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
            Server stub = (Server) registry.lookup("slave");
            // Pass a message to the remote object
            stub.unregisterSubscriber(topic, UUID, ReqID);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString()); 
            e.printStackTrace(); 
        }
    }
    public void lockMaster() {
        //lock.lock();
    }
    public HashMap<String,List<String> > syncWithSlave() {
        return topicSubscriberList;
    }
    public void unlockMaster() {
        //lock.unlock();
    }
    private void sendToSubscribers(String topic, Data dt, String ReqID) {
        // Send data to all subscribers of topic
        List <String> topicSubscribers = topicSubscriberList.get(topic);
        for (String sub: topicSubscribers) {
            try {  
                Registry registry = LocateRegistry.getRegistry(); 
                Subscriber stub = (Subscriber) registry.lookup(sub);
                stub.receiveData(topic, dt, ReqID);
            } catch (Exception e) {
                System.err.println("Client exception: " + e.toString()); 
                e.printStackTrace();
            }
        }
    }
    public void __registerSubscriber(String topic, String UUID, String ReqID) {
        // Update the hashmap
        if (!topicSubscriberList.containsKey(topic)) {
            topicSubscriberList.put(topic, new ArrayList<String>());
        }
        topicSubscriberList.get(topic).add(UUID);
    }
    public void registerSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        reentrantReadWriteLock.writeLock().lock();
        try {
            // Send this data to slave
            __registerSubscriber(topic, UUID, ReqID);
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
    }
    public void unregisterSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        reentrantReadWriteLock.writeLock().lock();
        try {
            // Send this data to slave
            __unregisterSubscriber(topic, UUID, ReqID);
        } finally {
            // Release lock
            reentrantReadWriteLock.writeLock().unlock();
            unsubscribeToSlave(topic, UUID, ReqID);
        }
    }
}