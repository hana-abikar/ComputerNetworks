// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Hana Abikar
// 220017532
// hana.abikar@city.ac.uk


import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.math.BigInteger;



// DO NOT EDIT starts
interface TemporaryNodeInterface {
    boolean start(String startingNodeName, String startingNodeAddress) throws IOException;
    boolean store(String key, String value);
    String get(String key);
}
// DO NOT EDIT ends


public class TemporaryNode implements TemporaryNodeInterface {

    private Socket socket;

    private BufferedReader in;

    PrintWriter out;

    private Socket clientSocket;

    private final Map<String, String> networkMap = new HashMap<>();

    public String calculateHashID(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private int calculateDistance(String hashID1, String hashID2) {
        // Convert hex string to binary string
        StringBuilder bin1 = new StringBuilder(new BigInteger(hashID1, 16).toString(2));
        StringBuilder bin2 = new StringBuilder(new BigInteger(hashID2, 16).toString(2));

        // Pad strings to ensure they are of equal length
        while (bin1.length() < bin2.length()) bin1.insert(0, "0");
        while (bin2.length() < bin1.length()) bin2.insert(0, "0");

        // Calculate the distance
        int distance = 0;
        for (int i = 0; i < bin1.length(); i++) {
            if (bin1.charAt(i) != bin2.charAt(i)) {
                break;
            }
            distance++;
        }
        return 256 - distance;
    }



    @Override
    // Now, initializeConnection method takes care of everything, so no need to re-initialize streams in start.
    public boolean start(String startingNodeName, String startingNodeAddress) {

        try {

            String [] segments = startingNodeAddress.split(":");
            String ip = segments[0];
            int port = Integer.parseInt(segments[1]);

            System.out.println("Client connecting....");
            Socket clientSocket = new Socket(ip, port);

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()),true);

            System.out.println("Connected to starting node: " + startingNodeAddress);

            out.println("START 1 " + startingNodeName + "\n");
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private String findNearestNode(String targetHashID) {
        String nearestNodeAddress = null;
        int shortestDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry : networkMap.entrySet()) {
            int distance = calculateDistance(targetHashID, entry.getKey());
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestNodeAddress = entry.getValue();
            }
        }

        return nearestNodeAddress;
    }

    @Override
    public boolean store(String key, String value) {
        try {
            // Splitting key and value into lines
            String[] keyLines = key.split("\n");
            String[] valueLines = value.split("\n");

            // Sending the PUT? request with the number of lines for key and value
            out.println("PUT? " + keyLines.length + " " + valueLines.length);
            for (String line : keyLines) {
                out.println(line);
            }
            for (String line : valueLines) {
                out.println(line);
            }
            out.flush();

            // Reading the response from the server
            String response = in.readLine();
            return "SUCCESS".equals(response);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    @Override
    public String get(String key) {
        try {
            // Calculate the hashID for the key
            out.write("GET? " + key.split("\n").length + "\n");
            out.write(key + "\n");
            out.flush();

            String response = in.readLine();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    public static void main(String[] args) {
        TemporaryNode testTempNode = new TemporaryNode();

        if(testTempNode.start("hana.abikar@city.ac.uk", "127.0.0.1:1234")){
            System.out.println("connection successful");
        }
    }

}