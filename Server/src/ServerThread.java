import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.util.ArrayList;

public class ServerThread extends Thread {
    private static final int UDP_DATA_LENGTH = 65000;

    private String command;
    private String arguments;
    private InetAddress address;
    private int port;

    ServerThread(InetAddress address
                , int port
                , String command
                , String arguments){
        this.command = command;
        this.arguments = arguments;
        this.address = address;
        this.port = port;
    }

    public void run(){
        System.out.println("Started response thread to command: " + command);
        switch(command){
            case "time": {
                sendTime();
            }
            break;

            case "echo": {
                sendEcho();
            }
            break;

            case "ls": {
                sendFileList();
            }
            break;

            case "download": {
                String clientID = address.toString() + ":" + String.valueOf(port);
                sendFile(arguments);
                Server.queue.remove(clientID);
            }
            break;

            default: {
                sendBytes("Unknown Command".getBytes());
            }
        }
        System.out.println("Ended response thread to command: " + command);
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

    private void sendTime() {
        sendBytes(LocalTime.now().toString().getBytes());
    }

    private void sendEcho() {
        sendBytes(arguments.getBytes());
    }

    private void sendFileList() {
        sendBytes(getFileNames().toString().getBytes());
    }

    private void sendBytes(byte[] buffer){
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            Server.serverSocket.send(packet);
        } catch (IOException e) {
            System.out.println("Can't send packet: " + e.getMessage());
        }
    }

    private void sendFile(String fileName){
        File file = new File("./Files/" + fileName);

        if(file.exists())
        {
            try {
                DataInputStream reader = new DataInputStream(new FileInputStream(file));
                long fileLength = file.length();
                String clientID = address.toString() + ":" + String.valueOf(port);

                sendFileSize(file.length());

                byte[] buffer = new byte[UDP_DATA_LENGTH];
                int length;
                long bytesSend = 0L;
                byte blockCounter = 1;

                while((length = reader.read(buffer, 0, UDP_DATA_LENGTH - 1)) > 0) {
                    String response;
                    buffer[UDP_DATA_LENGTH - 1] = blockCounter;

                    do {
                        sendBytes(buffer);

                        long startTimeout = System.currentTimeMillis();

                        while (true) {
                            response = Server.queue.get(clientID);
                            if (!response.equalsIgnoreCase("")) {
                                Server.queue.replace(clientID, "");
                                break;
                            }
                            if (System.currentTimeMillis() - startTimeout > 10000) {
                                System.out.println(Thread.currentThread().getName() + ": no response from client. Sending aborted");
                                return;
                            }
                        }
                    } while (response.equals("REJECTED"));

                    if (blockCounter < 127)
                        blockCounter++;
                    else
                        blockCounter = 0;

                    bytesSend += length;
                    System.out.print("\rSending file... " + bytesSend + " / " + fileLength + " " + (bytesSend * 100) / fileLength + "% ");
                }

                System.out.println("");
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFileSize(long fileSize) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(fileSize);
        sendBytes(buffer.array());
    }
}
