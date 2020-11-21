import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.rmi.registry.LocateRegistry;
import java.rmi.Remote;
import java.rmi.RemoteException;

/*
 * Publisher class is a simple class because it will not be binded to
 * any RMI id. Thus, we can make the entire class static.
 */
public class Publisher{
    static String UUID="";
    static String logFile="";
    private static void publish(String topic, Data dt, String ReqID) {

    }
    private static void outputToLog(String log) {

    }
    static void executeCommandsFromFile(String filename) {

    }
    public static void main(String[] args) {

    }
}