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
    boolean start(String nodeName, String nodeAddress) throws IOException;
    boolean store(String key, String value);
    String get(String key);
}
// DO NOT EDIT ends

public class TemporaryNode implements TemporaryNodeInterface {

    private BufferedReader inputReader;
    private PrintWriter outputWriter;
    private Socket clientSocket;
    private final Map<String, String> nodeDistanceMap = new HashMap<>();

    public String generateHashFromString(String text) throws NoSuchAlgorithmException {
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

    private int calculateHammingDistance(String hashID1, String hashID2) {
        StringBuilder bin1 = new StringBuilder(new BigInteger(hashID1, 16).toString(2));
        StringBuilder bin2 = new StringBuilder(new BigInteger(hashID2, 16).toString(2));
        while (bin1.length() < bin2.length()) bin1.insert(0, "0");
        while (bin2.length() < bin1.length()) bin2.insert(0, "0");
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
    public boolean start(String nodeName, String nodeAddress) {
        try {
            String[] segments = nodeAddress.split(":");
            String ip = segments[0];
            int port = Integer.parseInt(segments[1]);
            clientSocket = new Socket(ip, port);
            inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            outputWriter.println("START 1 " + nodeName + "\n");
            outputWriter.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getNearestNodeAddress(String targetHashID) {
        String nearestNodeAddress = null;
        int shortestDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, String> entry : nodeDistanceMap.entrySet()) {
            int distance = calculateHammingDistance(targetHashID, entry.getKey());
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
            String[] keyLines = key.split("\n");
            String[] valueLines = value.split("\n");
            outputWriter.println("PUT? " + keyLines.length + " " + valueLines.length);
            for (String line : keyLines) {
                outputWriter.println(line);
            }
            for (String line : valueLines) {
                outputWriter.println(line);
            }
            outputWriter.flush();
            String response = inputReader.readLine();
            return "SUCCESS".equals(response);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String get(String key) {
        try {
            // Write the GET command to the server
            outputWriter.println("GET? " + key.split("\n").length);
            outputWriter.println(key);
            outputWriter.flush();  // Make sure to flush the writer to send data immediately

            // Read the response from the server
            String response = inputReader.readLine();

            // Check if the response indicates a value was found
            if (response != null && response.startsWith("VALUE")) {
                return response.substring(6); // Assuming the response is "VALUE <actual_value>"
            } else {
                System.err.println("Error or no value found for key: " + key + " - Response: " + response);
                return null;  // Return null if no value found or there is an error
            }
        } catch (IOException e) {
            System.err.println("IOException during get operation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
