// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Hana Abikar
// 220017532
// hana.abikar@city.ac.uk


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;


// DO NOT EDIT starts
interface FullNodeInterface {
    public boolean listen(String ipAddress, int portNumber);
    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}
// DO NOT EDIT ends

public class FullNode implements FullNodeInterface {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, String> keyValueStore = new ConcurrentHashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final ConcurrentHashMap<String, String> networkMap = new ConcurrentHashMap<>();

    ServerSocket serverSocket;
    Writer out;
    BufferedReader in;

    public boolean listen(String ipAddress, int portNumber) {
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("FullNode listening on " + ipAddress + ":" + portNumber);
            return true;
        } catch (Exception e) {
            System.err.println("Could not listen on " + ipAddress + ":" + portNumber + " - " + e.getMessage());
            return false;
        }
    }


    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        // handleIncomingConnections();
        Socket clientSocket;
        while (true) {
            try {
                System.out.println("handling connection starting");
                clientSocket = serverSocket.accept();
                Socket newClient = clientSocket;
                new Thread(() -> {
                    try {
                        handleClient(newClient);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                // Removed the direct thread creation and start to avoid redundancy.
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void handleClient(Socket clientSocket) throws Exception{
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            Writer out = new OutputStreamWriter(clientSocket.getOutputStream());

            String request;
            while ((request = in.readLine()) != null) {
                if (request.startsWith("PUT?")) {

                    String[] parts = request.split("\\s+", 4);

                    if (parts.length == 3) {
                        int keyLines = Integer.parseInt(parts[1]);
                        int valueLines = Integer.parseInt(parts[2]);
                        StringBuilder keyBuilder = new StringBuilder();
                        StringBuilder valueBuilder = new StringBuilder();

                        for (int i = 0; i < keyLines; i++) {
                            keyBuilder.append(in.readLine()).append("\n");
                        }
                        for (int i = 0; i < valueLines; i++) {
                            valueBuilder.append(in.readLine()).append("\n");
                        }

                        String key = keyBuilder.toString().trim();
                        String value = valueBuilder.toString().trim();
                        keyValueStore.put(key, value);
                        out.write("SUCCESS\n");
                        out.flush();
                    } else {
                        out.write("ERROR Invalid PUT request format\n");
                        out.flush();
                    }
                } else if (request.startsWith("GET?")) {
                    String[] parts = request.split("\\s+", 3);
                    if (parts.length == 3) {
                        String key = parts[2];
                        String value = keyValueStore.getOrDefault(key, null);
                        if (value != null) {
                            out.write("VALUE " + value.length() + "\n" + value);
                            out.flush();
                        } else {
                            out.write("NOPE");
                            out.flush();
                        }
                    }
                } else if (request.startsWith("NOTIFY")) {

                    String[] parts = request.split("\\s+");
                    if (parts.length >= 3) {
                        String nodeName = parts[1];
                        String nodeAddress = parts[2];
                        networkMap.put(nodeName, nodeAddress);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing the client socket: " + e.getMessage());
            }
        }
    }

}