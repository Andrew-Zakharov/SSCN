package com.mishchenko.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServerApp {

    public static void main(String[] args) {
        ServerSocket serverSocket = getServerSocket();
        Socket clientSocket = null;

        try {
            clientSocket = accept(serverSocket);
        } catch (IOException e) {
            System.out.println("Can't accept new connection!");
        }

        while (true) {
            String request = readFromSocket(clientSocket);
            if (request != null) {
                parseRequestAndSendResponse(request, clientSocket);
            }
        }
    }

    public static ServerSocket getServerSocket() {
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

    public static Socket accept(ServerSocket serverSocket) throws IOException {
        System.out.println("Waiting for connection...");
        Socket socket = serverSocket.accept();
        System.out.println("Client connected: " + socket.getRemoteSocketAddress().toString());
        return socket;
    }

    public static String readFromSocket(Socket socket) {
        try {
            BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String data = socketInput.readLine();
            return data;
        } catch (Exception e) {
            System.out.println("Can't read from socket!");
            return null;
        }
    }

    public static void writeToSocket(Socket socket, String data) {
        DataOutputStream socketOutput;
        try {
            socketOutput = new DataOutputStream(socket.getOutputStream());
            socketOutput.writeBytes(data + "\n");
        } catch (IOException e) {
            System.out.println("Can't write to socket!");
        }
    }

    public static void parseRequestAndSendResponse(String request, Socket socket) {
        String[] requestArgs = request.split(" ");
        switch (requestArgs[0].toUpperCase()) {
        case "TIME":
            writeToSocket(socket, LocalDateTime.now().toString());
            break;
        case "ECHO":
            String response = "";
            for (int i = 1; i < requestArgs.length; i++) {
                response += requestArgs[i];
            }
            writeToSocket(socket, response);
            break;
        case "EXIT":
            System.exit(0);
            break;
        case "LIST":
            List<String> filesList = listFiles();
            writeToSocket(socket, filesList.toString());
            break;
        case "GET":
            sendFile(requestArgs[1], socket);
            break;
        default:
            writeToSocket(socket, "Unknown command");
        }
    }

    public static List<String> listFiles() {
        File[] files = new File(".").listFiles();
        List<String> filesList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                filesList.add(file.getName());
            }
        }
        return filesList;
    }

    private static void sendFile(String fileName, Socket socket) {
        File file = new File(fileName);
        DataOutputStream socketOutput = null;

        try {
            socketOutput = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            System.out.println("Can't get socket output stream!");
        }

        try {
            DataInputStream fileReader = new DataInputStream(new FileInputStream(file));
            byte[] block = new byte[1024 * 1024];
            long bytesSended = 0L;

            try {
                socketOutput.writeBoolean(true);
                socketOutput.writeLong(file.length());
                socketOutput.writeUTF(file.getName());
                socketOutput.flush();

                int bytesRead;
                while ((bytesRead = fileReader.read(block)) > 0) {
                    socketOutput.write(block, 0, bytesRead);
                    socketOutput.flush();
                    bytesSended += bytesRead;
                    System.out.println("Bytes sended:" + bytesSended + "/" + file.length());
                }
                fileReader.close();
            } catch (Exception e) {
                System.out.println("Can't write to socket!");
            }
        } catch (FileNotFoundException e) {
            System.out.println("No such file:" + fileName);
            try {
                socketOutput.writeBoolean(false);
            } catch (IOException e1) {
                System.out.println("Can't write to socket!");
            }
        }
    }
}
