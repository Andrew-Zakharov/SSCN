import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

class Client {
    final static private int UDP_PORT = 55555;
    final private static int BUFFER_LENGTH = 1024;
    final private static int UDP_LENGTH = 65507;
    final private static int UDP_RECEIVE_TIMEOUT = 1000;
    final private static byte URGENT_DATA = -77;
    final private static int TCP_DATA_LENGTH = 65000;
    final private static String DOWNLOADS_FOLDER_NAME = "Downloads";
    public final static String SERVER_ADDRESS = "192.168.2.3";

    public static void main(String argv[]) throws Exception {
        String request, serverResponse;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        DataInputStream inFromServer = null;
        boolean connected = false;

        boolean downloadFolderStatus = createDownloadsFolder();

        while (!connected) {
            String ip, port;
            System.out.println("Enter server ip address: ");
            ip = inFromUser.readLine();
            System.out.println("Enter server port: ");
            port = inFromUser.readLine();

            try {
                clientSocket = new Socket(ip, Integer.parseInt(port));
            }
            catch(UnknownHostException e){
                System.out.println("Can't connect to " + ip + " host.");
            }
            catch (ConnectException e){
                System.out.println("Can't connect to server. Try again.");
            }

            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.setKeepAlive(true);
                clientSocket.setOOBInline(true);
                connected = true;
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new DataInputStream(clientSocket.getInputStream());
            }

            while (clientSocket != null && !clientSocket.isClosed()) {
                System.out.print("You >> ");
                request = inFromUser.readLine();

                try {
                    outToServer.writeBytes(request + "\n");
                    outToServer.flush();

                    if (request.startsWith("download") || request.startsWith("downloadUDP")) {
                        String fileName = request.substring(request.indexOf(' ') + 1);

                        if(downloadFolderStatus){
                            fileName = DOWNLOADS_FOLDER_NAME + "/" + fileName;
                        }

                        File file = new File(fileName);
                        DataOutputStream writer = new DataOutputStream(new FileOutputStream(file));

                        if(request.startsWith("downloadUDP")) {
                            downloadUDP(writer, file);
                        }
                        else{
                            System.out.println("Prepare for download...");
                            download(clientSocket, writer, file);
                        }
                    } else {
                        serverResponse = inFromServer.readLine();
                        System.out.println("Server >> " + serverResponse.trim());
                        if (serverResponse.equalsIgnoreCase("close")) {
                            clientSocket.close();
                        }
                    }
                } catch (IOException exception) {
                    System.out.println(exception.getMessage());
                    System.out.println("A Error occured when sending to server. Try again later.\n");
                }


                if (clientSocket.isClosed()) {
                    System.out.println("Connection closed.\n");
                }
            }
        }
    }

    private static boolean download(Socket socket, DataOutputStream writer, File file){
        DataInputStream inFromServer = null;
        boolean success = true;
        try {
            inFromServer = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] buffer = new byte[TCP_DATA_LENGTH];
        int length;
        long estimatedSize = 0;
        int urgentDataIndex = -1;
        boolean urgentDataFlag = false;
        long bytesReceived = 0L;
        byte[] fileLengthBytes = new byte[8];

        try {
            System.out.print("Waiting for file size...");
            //inFromServer.readLong(fileLengthBytes);
            estimatedSize = inFromServer.readLong();
            System.out.println("Success");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(estimatedSize == -1) {
            System.out.println("File not found. Aborting download");
            success = false;
            return success;
        }
        else {
            System.out.println("Estimated size " + estimatedSize);
        }

        long startTime = System.nanoTime();
        try {
            while (file.length() < estimatedSize) {
                //System.out.println("Prepare for read...");
                length = inFromServer.read(buffer);
                bytesReceived += length;
                writer.write(buffer, 0, length);
                writer.flush();
                //System.out.println("Received: " + bytesReceived + " bytes");
                System.out.print("\rDownloading file... " + file.length() + " / " + estimatedSize + " " + (file.length() * 100) / estimatedSize + "% ");
            }
            writer.close();
            System.out.println("Download complete!");
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
            System.out.println("Can't write bytes. Download canceled.");
            success = false;
        }
        long endTime = System.nanoTime();
        NumberFormat formatter = new DecimalFormat("#0.00");
        System.out.println("Time: " + formatter.format((double)(endTime - startTime) / 1000000000L) + " s");

        return success;
    }


    public static void writeStringToSocket(DatagramSocket socket, String string) {
        try {
            InetAddress inetAddress = InetAddress.getByName(SERVER_ADDRESS);
            byte[] buffer = string.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, UDP_PORT);
            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Can't write to socket!");
        }
    }
    private static boolean downloadUDP(DataOutputStream writer, File file){
        DatagramSocket udpSocket = null;
        boolean success = true;
        byte[] buffer = new byte[UDP_LENGTH];
        DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
        // Arrays.fill(buffer, (byte) 0);
        byte blockCounter = 0;
        double successCounter = 0;
        double failedCounter = 0;
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            udpSocket.setSoTimeout(UDP_RECEIVE_TIMEOUT);
            udpSocket.setReuseAddress(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        long fileSize = getFileSize(udpSocket);
        System.out.println("File size: " + fileSize);

        long startTime = System.nanoTime();
        while(file.length() < fileSize){
            try {
                udpSocket.receive(udpPacket);
                buffer = udpPacket.getData();
                    if (buffer[UDP_LENGTH - 1] == blockCounter + 1 || buffer[UDP_LENGTH - 1] == blockCounter - 127) {
                        blockCounter = buffer[UDP_LENGTH - 1];
                        if ((fileSize - file.length() - UDP_LENGTH) >= 0) {
                            writer.write(buffer, 0, udpPacket.getLength() - 1);
                        } else {
                            writer.write(buffer, 0, (int) (fileSize - file.length()));
                        }

                        writeStringToSocket(udpSocket, "RECEIVED");
                        successCounter += 1;
                    } else {
                        writeStringToSocket(udpSocket, "REJECTED");
                        failedCounter += 1;
                        continue;
                    }
             /*  writer.write(udpPacket.getData(), 0, udpPacket.getLength());
                writer.flush();*/
                System.out.print("\rDownloading file... " + file.length() + " / " + fileSize + " " + (file.length() * 100) / fileSize + "% ");
            } catch (IOException e) {
                writeStringToSocket(udpSocket, "REJECTED");
                failedCounter += 1;
                continue;
            }
        }
        long endTime = System.nanoTime();
        //double bandwidth = file.length() / (double)((endTime - startTime) / 1000000000L);
        NumberFormat formatter = new DecimalFormat("#0.00");
        //System.out.print(formatter.format(bandwidth / (1024 * 1024)) + " MB/s ");
        System.out.println("Time: " + formatter.format((double)(endTime - startTime) / 1000000000L) + " s");
        System.out.println("Hit percent: " + failedCounter * 100 / (successCounter + failedCounter));

        try {
            writer.close();
            udpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(success) {
            System.out.println("Download complete!");
        }

        return success;
    }

    private static long getFileSize(DatagramSocket udpSocket){
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        long fileSize = 0;
        DatagramPacket udpPacket = new DatagramPacket(buffer.array(), buffer.array().length);
        try {
            udpSocket.receive(udpPacket);
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