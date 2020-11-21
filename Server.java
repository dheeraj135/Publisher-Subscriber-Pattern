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
    private void sendToSlave() {

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
    private void registerSubscriber(String topic, String UUID, String ReqID) {
        // Acquire lock on topicSubsriberList
        // Send this data to slave
    }
}