import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.rmi.registry.LocateRegistry;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class Server implements Remote{
    HashMap<String,List<String> > topicSubscriberList;
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
        // slave.__registerSubscriber()
    }
    private void unsubscribeToSlave(String topic, String UUID, String ReqID) {
        // Get slave instance from RMI
        // slave.__registerSubscriber()
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
    }
    public void __registerSubscriber(String topic, String UUID, String ReqID) {
        // Update the hashmap
    }
    public void registerSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        // Send this data to slave
        __registerSubscriber(topic,UUID,ReqID);
        // Release lock
    }
    public void __unregisterSubscriber(String topic, String UUID, String ReqID) {
        // Update the hashmap
    }
    public void unregisterSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        // Send this data to slave
        __unregisterSubscriber(topic,UUID,ReqID);
        // Release lock
    }
    public void recievePublication(String topic, String Data, String ReqID) {
        // Call sendToSubscribers.
    }
}