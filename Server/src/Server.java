import java.io.*;
import java.net.*;

class Server {
    private static final int UDP_PORT = 55555;
    private static final int UDP_LENGTH = 65507;

    public static void main(String argv[]) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(UDP_PORT);
        System.out.println("Server started on " + serverSocket.getLocalAddress().toString() + ":" + serverSocket.getLocalPort());

        while(true) {
            DatagramPacket packet = getPacket(serverSocket);
            String clientRequest = getClientRequest(packet);

            //System.out.println("Client request: " + clientRequest);

            ServerThread thread = new ServerThread(serverSocket, packet.getAddress(), packet.getPort(),
                                                       getCommand(clientRequest), getArguments(clientRequest));
            thread.start();
        }
    }

    private static DatagramPacket getPacket(DatagramSocket serverSocket){
        byte[] buffer = new byte[UDP_LENGTH];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            serverSocket.receive(packet);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

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