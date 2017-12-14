import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

class Client {
    final private static int TCP_DATA_LENGTH = 65000;
    final private static String DOWNLOADS_FOLDER_NAME = "Downloads";

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

                    if (request.startsWith("download")) {
                        String fileName = request.substring(request.indexOf(' ') + 1);

                        if(downloadFolderStatus){
                            fileName = DOWNLOADS_FOLDER_NAME + "/" + fileName;
                        }

                        File file = new File(fileName);
                        DataOutputStream writer = new DataOutputStream(new FileOutputStream(file));

                        download(clientSocket, writer, file);
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

        try {
            System.out.print("Waiting for file size...");
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
                length = inFromServer.read(buffer);
                writer.write(buffer, 0, length);
                writer.flush();
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