import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.lang.management.ManagementFactory;

public class Subscriber implements SubscriberInterface {
    String UUID = "";
    String logFile = "";
    Registry registry;

    private void unsubsribe(String topic, String ReqID) {
        // call server.unregisterSubscriber()
        try {
            ServerInterface server = (ServerInterface) registry.lookup("master");
            server.unregisterSubscriber(topic, UUID, ReqID);
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
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void receiveData(String topic, Data dt, String ReqID) {
        // receive data from server. This is called by server
    }

    private void outputToLog(String log) {

    }

    public void executeCommandsFromFile(String filename) {
        subscribe("test1","0");
        subscribe("test2","1");
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
        robj.executeCommandsFromFile(args[0]);
    }
}