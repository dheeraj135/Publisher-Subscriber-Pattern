import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        // pb.redirectOutput(Redirect.INHERIT);
        server = pb.start();

        Thread.sleep(1000);

        String[] args2 = new String[] { "java", "Publisher" };
        ProcessBuilder pb2 = new ProcessBuilder(args2);
        publisher = pb2.start();
        // OutputStream stdin = publisher.getOutputStream();
        OutputStream publisherwriter = publisher.getOutputStream();

        Thread.sleep(1000);

        Process[] subs = new Process[num_subs];
        for (int i = 0; i < num_subs; i++) {
            String filename = Integer.toString(test_num)+"_"+Integer.toString(i);
            String[] args1 = new String[] { "java", "Subscriber", "test_mode", filename};
            ProcessBuilder pb1 = new ProcessBuilder(args1);
            pb1.redirectOutput(Redirect.INHERIT);
            try {
                subs[i] = pb1.start();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        
        Thread.sleep(2000);

        OutputStream[] writers = new OutputStream[num_subs];
        for (int i = 0; i < num_subs; i++) {
            // writers[i] = new BufferedWriter(new OutputStreamWriter(subs[i].getOutputStream()));
            writers[i] = subs[i].getOutputStream();
        }

        File testfile = new File("./tests/inputs/" + test_num + ".txt");
        Scanner reader;
        try {
            reader = new Scanner(testfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        File[] testsubfile = new File[num_subs];
        FileWriter[] testsubfilewriter = new FileWriter[num_subs];
        for (int i = 0; i < num_subs; i++) {
            testsubfile[i] = new File("./tests/expected_output/" + test_num + "_" + i + ".txt");
            testsubfilewriter[i] = new FileWriter(testsubfile[i].getAbsoluteFile());
        }

        Map<String, Set<Integer>> map = new HashMap<String, Set<Integer>>();
        while (reader.hasNextLine()) {
            Thread.sleep(100);
            String line = reader.nextLine();
            String[] splitStrings = line.split(" ");
            if (splitStrings.length != 3)
                return;
            if (splitStrings[0].compareTo("S") == 0 || splitStrings[0].compareTo("U") == 0) {
                String sub_num = splitStrings[2];
                writers[Integer.parseInt(sub_num)].write((splitStrings[0] + " " + splitStrings[1]+"\n").getBytes());
                writers[Integer.parseInt(sub_num)].flush();
                if (splitStrings[0].compareTo("S") == 0) {
                    String key = splitStrings[1];
                    Set<Integer> set = map.get(key);
                    if (set == null){ 
                        set = new HashSet<Integer>();
                        map.put(key, set);
                    }
                    set.add(Integer.parseInt(splitStrings[2]));
                } else if (splitStrings[0].compareTo("U") == 0) {
                    String key = splitStrings[1];
                    Set<Integer> set = map.get(key);
                    if (set != null){
                        set.remove(Integer.parseInt(splitStrings[2]));
                    }
                }
            } else if (splitStrings[0].compareTo("T") == 0) {
                publisherwriter.write((splitStrings[1] + " " + splitStrings[2]+"\n").getBytes());
                publisherwriter.flush();
                String key = splitStrings[1];
                Set<Integer> set = map.get(key);
                if (set != null){
                    for (int sub: set) {
                        testsubfilewriter[sub].write(splitStrings[2] + "\n");
                    }
                }
            } else {
                System.err.println("ERROR: Invalid Command line: " + line);
            }
        }
        Thread.sleep(5000);
        reader.close();
        for (int i = 0; i < num_subs; i++) {
            subs[i].destroy();
            testsubfilewriter[i].close();
        }
        //Runtime.getRuntime().exec("kill -SIGNINT " + server.pid());
        publisher.destroy();
        server.destroyForcibly();
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
                File file = new File("./tests/inputs/" + i + ".txt");
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

        //After this write code to diff between logs and expected_output/
    }
}