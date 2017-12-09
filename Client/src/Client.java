import java.io.*;
import java.net.*;

class Client {

    public static void main(String argv[]) throws Exception {
        String request, serverResponse;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        DataInputStream inFromServer = null;
        boolean connected = false;

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
                connected = true;
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new DataInputStream(clientSocket.getInputStream());
            }

            while (clientSocket != null && !clientSocket.isClosed()) {
                System.out.print("(You)>> ");
                request = inFromUser.readLine();

                try {
                    outToServer.writeBytes(request + "\n");
                    outToServer.flush();

                    if (request.startsWith("download")) {
                        String fileName = request.substring(request.indexOf(' ') + 1);
                        System.out.println("File name: " + fileName);
                        File file = new File(fileName);
                        DataOutputStream writer = new DataOutputStream(new FileOutputStream(file));

                        download(clientSocket, writer, file);
                    } else {
                        serverResponse = inFromServer.readLine();
                        System.out.println(serverResponse);
                        if (serverResponse.equalsIgnoreCase("close")) {
                            clientSocket.close();
                        }
                    }
                } catch (IOException exception) {
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
        byte[] buffer = new byte[65536];
        int length;
        long estimatedSize = 0;
        try {
            estimatedSize = inFromServer.readLong();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Estimated size " + estimatedSize);
        try {
            while (file.length() < estimatedSize) {
                length = inFromServer.read(buffer);
                System.out.println("Prepare to receive bytes from server...");
                //inFromServer.readFully(buffer);
                System.out.println("Received " + length + " bytes from server");
                System.out.println("Prepare for write bytes...");
                //writer.write(buffer, 0, length);
                writer.write(buffer, 0, length);
                System.out.println("Write " + length + " bytes to file " + file.getName());
            }
            writer.close();
            System.out.println("Download complete!");
        } catch (IOException exception) {
            System.out.println("Can't write bytes. Download canceled.");
            success = false;
        }

        return success;
    }
}