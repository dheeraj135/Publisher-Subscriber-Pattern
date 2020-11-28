import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
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
        int lines = 200;
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

    public static void runTests(int num_subs) throws InterruptedException, IOException {
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
            String filename = Integer.toString(i);
            String[] args1 = new String[] { "java", "Subscriber", ""+i, filename};
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

        File testfile = new File("./tests/input.txt");
        Scanner reader;
        try {
            reader = new Scanner(testfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        while (reader.hasNextLine()) {
            Thread.sleep(500);
            String line = reader.nextLine();
            String[] splitStrings = line.split(" ");
            if (splitStrings.length != 3)
                return;
            if (splitStrings[0].compareTo("S") == 0 || splitStrings[0].compareTo("U") == 0) {
                String sub_num = splitStrings[2];
                writers[Integer.parseInt(sub_num)].write((splitStrings[0] + " " + splitStrings[1]+"\n").getBytes());
                writers[Integer.parseInt(sub_num)].flush();
            } else if (splitStrings[0].compareTo("T") == 0) {
                publisherwriter.write((splitStrings[1] + " " + splitStrings[2]+"\n").getBytes());
                publisherwriter.flush();
            } else {
                System.err.println("ERROR: Invalid Command line: " + line);
            }
        }
        Thread.sleep(5000);
        reader.close();
        publisher.destroy();
        server.destroy();
    }

    private static void checkTestCasesOutput(int num_subs) {
        boolean allTestsPassed = true;
        for (int i=0;i<num_subs;i++) {
            try {
                File f1 = new File("./logs/"+Integer.toString(i)+".txt");
                File f2 = new File("./tests/expected_output/"+Integer.toString(i)+".txt");
                FileReader fR1 = new FileReader(f1);
                FileReader fR2 = new FileReader(f2);
                BufferedReader reader1 = new BufferedReader(fR1);
                BufferedReader reader2 = new BufferedReader(fR2);
                String line1 = null;
                String line2 = null;
                int flag = 1;
                try {
                    while ((flag == 1) && ((line1 = reader1.readLine()) != null)
                            && ((line2 = reader2.readLine()) != null)) {
                        if (!line1.equals(line2))
                            flag = 0;
                    }
                    reader1.close();
                    reader2.close();
                } catch(IOException e) {
                    System.out.println(e);
                }
                if (flag==0){
                    allTestsPassed = false;
                }
            } catch(FileNotFoundException e){
                System.out.println(e);
            }
        }
        if (allTestsPassed){
            System.out.println("All test cases passed. :)");
        } else {
            System.out.println("Test cases did not pass. :(");
        }
    }

    public static void verifyTests(int num_subs) throws IOException {
        Map<String, Set<Integer>> map = new HashMap<String, Set<Integer>>();
        File testfile = new File("./logs/server.txt");
        Scanner reader;
        try {
            reader = new Scanner(testfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        FileWriter[] writers = new FileWriter[num_subs];
        for (int i = 0; i < num_subs; i++) {
            File file = new File("./tests/expected_output/"+i+".txt");
            writers[i] = new FileWriter(file.getAbsoluteFile());
        }
        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            String[] splitStrings = line.split(" ");
            if (splitStrings.length != 3)
                return;
            if (splitStrings[0].compareTo("S") == 0) {
                String key = splitStrings[2];
                Set<Integer> set = map.get(key);
                if (set == null){ 
                    set = new HashSet<Integer>();
                    map.put(key, set);
                }
                set.add(Integer.parseInt(splitStrings[1]));
            } else if (splitStrings[0].compareTo("U") == 0) {
                String key = splitStrings[2];
                Set<Integer> set = map.get(key);
                if (set != null){
                    set.remove(Integer.parseInt(splitStrings[1]));
                }
            } else if (splitStrings[0].compareTo("P") == 0) {
                String key = splitStrings[1];
                Set<Integer> set = map.get(key);
                if (set != null){
                    for (Integer sub: set){
                        writers[sub].write(splitStrings[2]+"\n");
                    }
                }
            }
        }
        for (int i=0;i<num_subs;i++){
            writers[i].close();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int num_topics = 10;
        if (args.length != 1) {
            System.out.println("Please pass the parameters num_subs.");
            System.exit(1);
        }
        int num_subs = Integer.parseInt(args[0]);
        File directory = new File("tests");
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            File file = new File("./tests/input.txt");
            FileWriter myWriter = new FileWriter(file.getAbsoluteFile());
            populateFile(myWriter, num_topics, num_subs);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        
        System.out.println("Running Test Cases\n");
        try {
            runTests(num_subs);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        verifyTests(num_subs);
        checkTestCasesOutput(num_subs);
        System.out.println("Completed Test Cases\n");
        Runtime.getRuntime().exec("killall -9 java");
    }
}