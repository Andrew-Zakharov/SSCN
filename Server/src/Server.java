import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

class Server {
    private static final int UDP_PORT = 55555;
    private static final int UDP_LENGTH = 65507;
    public static ConcurrentHashMap<String, String> queue;
    public static DatagramSocket serverSocket;

    public static void main(String argv[]) throws Exception {
        serverSocket = new DatagramSocket(UDP_PORT);
        queue = new ConcurrentHashMap<>();
        System.out.println("Server started on " + serverSocket.getLocalAddress().toString() + ":" + serverSocket.getLocalPort());

        while(true) {
            try {
                DatagramPacket packet = getPacket(serverSocket);
                String clientRequest = getClientRequest(packet);
                String clientID = packet.getAddress().toString() + ":" + String.valueOf(packet.getPort());

                System.out.println("Client request: " + clientRequest);

                if (!queue.containsKey(clientID)) {
                    if(getCommand(clientRequest).equals("download")){
                        queue.put(clientID, "");
                        System.out.println("Command = download");
                    }
                    ServerThread thread = new ServerThread(packet.getAddress(), packet.getPort(),
                            getCommand(clientRequest), getArguments(clientRequest));
                    System.out.println("Starting response thread...");
                    thread.start();
                } else {
                    queue.replace(clientID, clientRequest);
                }
            }catch(IOException e){
                System.out.println("Socket error: " + e.getMessage());
                return;
            }
        }
    }

    private static DatagramPacket getPacket(DatagramSocket serverSocket) throws IOException{
        byte[] buffer = new byte[UDP_LENGTH];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(packet);
        return packet;
    }

    private static String getClientRequest(DatagramPacket packet){
        String clientRequest;

        clientRequest = new String(packet.getData(), 0, packet.getLength());
        clientRequest.trim();

        return clientRequest;
    }

    private static String getCommand(String clientRequest) {
        if(clientRequest.contains(" ")) {
            return clientRequest.substring(0, clientRequest.indexOf(' '));
        }else {
            return clientRequest;
        }
    }

    private static String getArguments(String clientRequest) {
        if(clientRequest.contains(" ")) {
            return clientRequest.substring(clientRequest.indexOf(' ') + 1);
        }else{
            return "";
        }
    }
}