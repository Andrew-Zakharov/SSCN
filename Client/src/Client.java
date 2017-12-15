import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

class Client {
    final private static int UDP_LENGTH = 65000;
    final private static String DOWNLOADS_FOLDER_NAME = "Downloads";

    private static DatagramSocket clientSocket;
    private static InetAddress serverAddress;
    private static int serverPort;

    public static void main(String argv[]) throws Exception {
        String request;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        clientSocket = new DatagramSocket();
        clientSocket.setReuseAddress(true);

        boolean downloadFolderStatus = createDownloadsFolder();
        boolean run = true;

        String address, port;
        System.out.print("Enter server ip address: ");
        address = inFromUser.readLine();
        System.out.print("Enter server port: ");
        port = inFromUser.readLine();

        serverAddress = InetAddress.getByName(address);
        serverPort = Integer.parseInt(port);

        System.out.println("Server now is " + serverAddress.toString() + ":" + serverPort);

        while(run)
        {
            request = getUserRequest(inFromUser);
            if(request != null){
                request.trim();
                sendBytes(request.getBytes());

                if (isDownload(request)) {
                    String fileName = request.substring(request.indexOf(' ') + 1);

                    if(downloadFolderStatus){
                        fileName = DOWNLOADS_FOLDER_NAME + "/" + fileName;
                    }

                    File file = new File(fileName);
                    DataOutputStream writer = new DataOutputStream(new FileOutputStream(file));
                    download(writer, file);
                } else {
                    byte[] buffer = new byte[UDP_LENGTH];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    clientSocket.receive(packet);
                    System.out.println("Server >> " + getString(packet));
                }
            }
        }
    }

    private static String getString(DatagramPacket packet){
        String string = null;
        try {
            string = new String(packet.getData(), 0, packet.getLength());
        } catch (Exception e) {
            System.out.println("Can't read string from packet: " + e.getMessage());
        }

        return string;
    }

    private static boolean isDownload(String request){
        return request.startsWith("download");
    }

    private static String getUserRequest(BufferedReader reader){
        System.out.print("You >> ");
        String clientRequest = null;
        try {
            clientRequest = reader.readLine();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return clientRequest;
    }

    private static void sendBytes(byte[] buffer) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
        try {
            clientSocket.send(packet);
        } catch (IOException e) {
            System.out.println("Can't send packet: " + e.getMessage());
        }
    }

    private static boolean download(DataOutputStream writer, File file){
        boolean success = true;
        byte[] buffer = new byte[UDP_LENGTH];
        byte blockCounter = 0;
        double successCounter = 0;
        double failedCounter = 0;

        long fileSize = getFileSize();
        System.out.println("File size: " + fileSize);

        long startTime = System.nanoTime();
        while(file.length() < fileSize){
            try {
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                clientSocket.receive(udpPacket);
                buffer = udpPacket.getData();
                if (buffer[UDP_LENGTH - 1] == blockCounter + 1 || buffer[UDP_LENGTH - 1] == blockCounter - 127) {
                    blockCounter = buffer[UDP_LENGTH - 1];
                    if ((fileSize - file.length() - UDP_LENGTH) >= 0) {
                       writer.write(buffer, 0, udpPacket.getLength() - 1);
                    } else {
                        writer.write(buffer, 0, (int) (fileSize - file.length()));
                    }

                    //sendBytes("RECEIVED".getBytes());
                    successCounter += 1;
                } else {
                    sendBytes("REJECTED".getBytes());
                    failedCounter += 1;
                    continue;
                }
                System.out.print("\rDownloading file... " + file.length() + " / " + fileSize + " " + (file.length() * 100) / fileSize + "% ");
            } catch (IOException e) {
                sendBytes("REJECTED".getBytes());
                failedCounter += 1;
            }
        }
        long endTime = System.nanoTime();
        NumberFormat formatter = new DecimalFormat("#0.00");
        System.out.println("Time: " + formatter.format((double)(endTime - startTime) / 1000000000L) + " s");

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Download complete!");

        return success;
    }

    private static long getFileSize(){
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        long fileSize = 0;
        DatagramPacket udpPacket = new DatagramPacket(buffer.array(), buffer.array().length);
        try {
            clientSocket.receive(udpPacket);
            buffer.put(udpPacket.getData());
            buffer.flip();
            fileSize = buffer.getLong();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileSize;
    }

    private static boolean createDownloadsFolder(){
        File downloadsFolder = new File(DOWNLOADS_FOLDER_NAME);
        boolean result = false;

        if(!downloadsFolder.exists()){
            try{
                downloadsFolder.mkdir();
                result = true;
            }
            catch (SecurityException se){
                System.out.println(se.getMessage());
            }
        }
        else{
            result = true;
        }

        return result;
    }
}