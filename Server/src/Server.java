import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.util.ArrayList;

class Server {
    private static final String prefix = "Server >> ";
    private static final int UDP_PORT = 8888;
    private static final int UDP_LENGTH = 65507;
    private static final int BUFFER_LENGTH = 508;
    private static final int UDP_TIMEOUT = 1000;

    public static void main(String argv[]) throws Exception {
        String clientRequest;
        ServerSocket welcomeSocket = getServerSocket();

        while(true) {
            Socket connectionSocket = welcomeSocket.accept();
            connectionSocket.setKeepAlive(true);
            DataInputStream inFromClient =
                    new DataInputStream(connectionSocket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            System.out.println("Client " + connectionSocket.getInetAddress().toString() + " connected.");

            while (!connectionSocket.isClosed()) {
                System.out.println("Waiting for client request...");
                try {
                    clientRequest = inFromClient.readLine();
                }
                catch(SocketException e){
                    System.out.println(e.getMessage());
                    break;
                }
                System.out.println("Client request: " + clientRequest);

                String command, arguments;
                if (clientRequest.contains(" ")) {
                    command = clientRequest.substring(0, clientRequest.indexOf(' '));
                    arguments = clientRequest.substring(clientRequest.indexOf(' ') + 1);
                } else {
                    command = clientRequest;
                    arguments = "";
                }

                switch (command) {
                    case "time": {
                        sendTime(connectionSocket);
                    }
                    break;

                    case "echo": {
                        sendEcho(connectionSocket, arguments);
                    }
                    break;

                    case "close": {
                        closeConnection(connectionSocket);
                    }
                    break;

                    case "ls": {
                        sendFileList(connectionSocket);
                    }
                    break;

                    case "download": {
                        sendFile(connectionSocket, arguments);
                    }
                    break;

                    case "downloadUDP":{
                        sendFileUDP(connectionSocket.getInetAddress(), arguments);
                    }
                    break;

                    default: {
                        outToClient.writeBytes("Unknown command\n");
                        outToClient.flush();
                    }
                }
            }
            System.out.println("Client " + connectionSocket.getInetAddress().toString() + " disconnected.");
        }
    }

    private static ArrayList<String> getFileNames() {
        File currentFolder = new File("./Files/");
        File[] files = currentFolder.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }

    private static void sendTime(Socket socket) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(prefix + LocalTime.now().toString() + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendEcho(Socket socket, String echo) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(prefix + echo + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFileList(Socket socket) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(prefix + getFileNames().toString() + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(Socket socket, String fileName) {
        File file = new File("./Files/" + fileName);
        if (file.exists()) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream reader = new DataInputStream(new FileInputStream(file));
                System.out.println("Request for downloading " + fileName);

                System.out.println("File length " + file.length());
                long fileLength = file.length();

                out.writeLong(fileLength);
                out.flush();

                byte[] buffer = new byte[65536];
                int length;
                while ((length = reader.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                    out.flush();
                    System.out.println("Send " + length + " bytes");
                }
                reader.close();
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFileUDP(InetAddress address, String fileName){
        DatagramSocket udpSocket = null;

        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            udpSocket.setSoTimeout(UDP_TIMEOUT);
        } catch (SocketException e) {
            System.out.println("Failed to create socket: " + e.getMessage());
            return;
        }

        File file = new File("./Files/" + fileName);
        if(file.exists())
        {
            try {
                DataInputStream reader = new DataInputStream(new FileInputStream(file));
                long fileLength = file.length();

                sendFileSize(udpSocket, address, fileLength);

                System.out.println("File length: " + fileLength);

                byte[] buffer = new byte[UDP_LENGTH];
                int length;
                long bytesSend = 0L;
                while((length = reader.read(buffer)) > 0){
                    try {
                        DatagramPacket udpPacket = new DatagramPacket(buffer, length, address, UDP_PORT);
                        udpSocket.send(udpPacket);
                        bytesSend += length;
                        System.out.print("\rSending file... " + bytesSend + " / " + fileLength + " " + (bytesSend * 100) / fileLength + "% ");
                    }
                    catch(IOException e){
                        System.out.println(e.getMessage());
                    }
                }

                System.out.println("");

                reader.close();
                udpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFileSize(DatagramSocket udpSocket, InetAddress address, long fileSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(fileSize);
        DatagramPacket udpPacket = new DatagramPacket(buffer.array(), buffer.array().length, address, UDP_PORT);
        udpSocket.send(udpPacket);
    }

    private static void closeConnection(Socket socket){
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes("close\n");
            out.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static ServerSocket getServerSocket() {
        ServerSocket serverSocket = null;
        int port = -1;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        while (serverSocket == null) {
            try {
                while (port == -1) {
                    try {
                        System.out.print("Input server port > ");
                        port = Integer.valueOf(input.readLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Wrong port format!");
                    }
                }
                serverSocket = new ServerSocket(port);
            } catch (Exception e) {
                System.out.println("can't start server on this port!");
                port = -1;
            }
        }
        System.out.println(
                "Server successfully started: " + serverSocket.getLocalSocketAddress().toString() + ":" + port);
        return serverSocket;
    }
}