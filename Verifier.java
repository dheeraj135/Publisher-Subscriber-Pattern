import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.Scanner;


public class Verifier {

    static String getAlphaNumericString(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    public static void populateFile(FileWriter myWriter, int num_topics, int num_subs) throws IOException {
        Random rand = new Random();
        int lines = 1000;
        while (lines != 0) {
            int cur = rand.nextInt(3);
            if (cur == 0) {
                myWriter.write("S topic" + rand.nextInt(num_topics) + " " + rand.nextInt(num_subs) + "\n");
            } else if (cur == 1) {
                myWriter.write("U topic" + rand.nextInt(num_topics) + " " + rand.nextInt(num_subs) + "\n");
            } else {
                myWriter.write("T topic" + rand.nextInt(num_topics) + " " + getAlphaNumericString(15) + "\n");
            }
            lines = lines - 1;
        }
    }

    public static void runTest(int test_num, int num_subs) throws InterruptedException, IOException {
        Process publisher, server;
        String[] args = new String[] { "java", "Server" };
        ProcessBuilder pb = new ProcessBuilder(args);
        server = pb.start();
        Process[] subs = new Process[num_subs];
        for (int i = 0; i < num_subs; i++) {
            String[] args1 = new String[] { "java", "Subscriber" };
            ProcessBuilder pb1 = new ProcessBuilder(args1);
            try {
                subs[i] = pb1.start();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        String[] args2 = new String[] { "java", "Publisher" };
        ProcessBuilder pb2 = new ProcessBuilder(args2);
        publisher = pb2.start();
        OutputStream stdin = publisher.getOutputStream();
        BufferedWriter publisherwriter = new BufferedWriter(new OutputStreamWriter(stdin));

        Thread.sleep(5000);
        BufferedWriter[] writers = new BufferedWriter[num_subs];
        for (int i = 0; i < num_subs; i++) {
            OutputStream stdin1 = subs[i].getOutputStream();
            writers[i] = new BufferedWriter(new OutputStreamWriter(stdin1));
        }

        File testfile = new File("./tests/" + test_num + ".txt");
        Scanner reader;
        try {
            reader = new Scanner(testfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            String[] splitStrings = line.split(" ");
            if (splitStrings.length != 3)
                return;
            if (splitStrings[0].compareTo("S") == 0 || splitStrings[0].compareTo("U") == 0) {
                String sub_num = splitStrings[2];
                writers[Integer.parseInt(sub_num)].write(splitStrings[0] + " " + splitStrings[1]);
            } else if (splitStrings[0].compareTo("T") == 0) {
                publisherwriter.write(splitStrings[1] + " " + splitStrings[2]);
            } else {
                System.err.println("ERROR: Invalid Command line: " + line);
            }
        }
        reader.close();
        for (int i = 0; i < num_subs; i++) {
            subs[i].destroy();
        }
        publisher.destroy();
        server.destroy();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int num_topics = 10;
        if (args.length != 2) {
            System.out.println("Please pass the parameters num_subs num_tests.");
            System.exit(1);
        }
        int num_subs = Integer.parseInt(args[0]);
        int num_tests = Integer.parseInt(args[1]);
        File directory = new File("tests");
        if (!directory.exists()) {
            directory.mkdir();
        }

        for (int i = 0; i < num_tests; i++) {
            try {
                File file = new File("./tests/" + i + ".txt");
                FileWriter myWriter = new FileWriter(file.getAbsoluteFile());
                populateFile(myWriter, num_topics, num_subs);
                myWriter.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
        int i=0;
        // for (int i = 0; i < num_tests; i++) {
            System.out.println("Running Test " + i + "\n");
            try {
                runTest(i, num_subs);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("Completed Test " + i + "\n");
        // }
    }
}