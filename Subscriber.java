import java.rmi.registry.Registry;
import java.rmi.Remote;

public class Subscriber implements Remote{
    String UUID = "";
    String logFile = "";
    private void unsubsribe(String topic, String ReqID) {
        // call server.unregisterSubscriber()
    }
    private void subscribe(String topic, String ReqID) {
        // call server.registerSubscriber()
    }
    public void receiveData(String topic, Data dt, String ReqID) {
        // receive data from server. This is called by server
    }
    private void outputToLog(String log) {

    }
    private void executeCommandsFromFile(String filename) {

    }

    public static void main(String[] args) {
        Subscriber obj = new Subscriber();
        // Create object, bind to UUID and call executeCommandsFromFile();
    }
}