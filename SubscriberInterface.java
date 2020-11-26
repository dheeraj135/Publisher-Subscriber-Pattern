import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SubscriberInterface extends Remote {
    public void receiveData(String topic, Data dt, String ReqID) throws RemoteException;
    public void executeCommandsFromFile(String filename) throws RemoteException;
    public void takeInputFromCommandLine() throws RemoteException;
    public String getUUID() throws RemoteException;
    public void register() throws RemoteException;
}