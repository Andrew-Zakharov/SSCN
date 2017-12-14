import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.*;

class Server {
    private static final String UNKNOWN_COMMAND = "Unknown command\n";
    private static final int BLOCK_SIZE = 1500;

    private static Map<String, DataInputStream> fileQueue;

    public static void main(String argv[]) throws Exception {
        ServerSocketChannel serverChannel = getServerSocket();

        Selector selector;
        selector = SelectorProvider.provider().openSelector();

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        fileQueue = new HashMap<>();

        while(true) {
            try{
                selector.select();
                Iterator selectedKeys = selector.selectedKeys().iterator();

                while(selectedKeys.hasNext()){
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid()){
                        continue;
                    }

                    if(key.isAcceptable()){
                        accept(key, selector);
                    }else if(key.isReadable()){
                        read(key);
                    }else if(key.isWritable()){
                        sendFilePart(key);
                    }
                }
            }
            catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }

    private static void accept(SelectionKey key, Selector selector){
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            socketChannel.socket().setKeepAlive(true);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
        String clientRequest = "";

        while (!clientRequest.contains("\n")) {
            byteBuffer.clear();

            int bytesRead;
            try {
                bytesRead = socketChannel.read(byteBuffer);
            } catch (IOException e) {
                key.cancel();
                socketChannel.close();
                return;
            }

            if (bytesRead == -1) {
                key.channel().close();
                key.cancel();
                return;
            }


            if (bytesRead != 0) {
                byte[] dataCopy = new byte[bytesRead];
                System.arraycopy(byteBuffer.array(), 0, dataCopy, 0, bytesRead);
                clientRequest += Charset.defaultCharset().decode(ByteBuffer.wrap(dataCopy));

            }
        }

        String command, arguments;
        if (clientRequest.contains(" ")) {
            command = clientRequest.substring(0, clientRequest.indexOf(' '));
            arguments = clientRequest.substring(clientRequest.indexOf(' ') + 1);
        } else {
            command = clientRequest;
            arguments = "";
        }

        System.out.println("Client request: " + command);
        System.out.println("Arguments: " + arguments);

        switch (command.trim()) {
            case "time": {
                sendTime(key);
            }
            break;

            case "echo": {
                sendEcho(key, arguments.trim());
            }
            break;

            case "close": {
                key.cancel();
            }
            break;

            case "ls": {
                sendFileList(key);
            }
            break;

            case "download": {
                sendFile(key, arguments);
            }
            break;

            default: {
                sendUnknownCommand(key);
            }
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

    private static void sendTime(SelectionKey key) {
        try {
            String time = LocalTime.now().toString() + "\n";

            key.interestOps(SelectionKey.OP_WRITE);
            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.write(ByteBuffer.wrap(time.getBytes()));
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendEcho(SelectionKey key, String echo) {
        try {
            echo += "\n";
            key.interestOps(SelectionKey.OP_WRITE);
            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.write(ByteBuffer.wrap(echo.getBytes()));
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFileList(SelectionKey key) {
        try {
            String fileList = getFileNames().toString() + "\n";

            key.interestOps(SelectionKey.OP_WRITE);
            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.write(ByteBuffer.wrap(fileList.getBytes()));
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendUnknownCommand(SelectionKey key){
        try {
            key.interestOps(SelectionKey.OP_WRITE);
            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.write(ByteBuffer.wrap(UNKNOWN_COMMAND.getBytes()));
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(SelectionKey key, String fileName) {
        File file = new File("Files/" + fileName.trim());
        SocketChannel socketChannel = null;

        key.interestOps(SelectionKey.OP_WRITE);
        socketChannel = (SocketChannel) key.channel();

        System.out.println(file.getAbsolutePath());
        if (file.exists()) {
            try {
                DataInputStream reader = new DataInputStream(new FileInputStream(file));
                System.out.println("Request for downloading: " + fileName);

                System.out.println("File length " + file.length());

                System.out.print("Sending file length...");
                sendFileSize(socketChannel, file.length());
                System.out.println("Success");

                fileQueue.put(String.valueOf(socketChannel.socket().getPort()), reader);
            } catch (IOException e) {
                e.printStackTrace();

            }
        }else{
            System.out.println("File not found!");
            try {
                sendFileSize(socketChannel, -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFilePart(SelectionKey key){
        key.interestOps(SelectionKey.OP_WRITE);
        SocketChannel socketChannel = (SocketChannel) key.channel();

        DataInputStream reader = fileQueue.get(String.valueOf(socketChannel.socket().getPort()));
        byte[] block = new byte[BLOCK_SIZE];

        try {
            int bytesRead = reader.read(block);
            if(bytesRead == -1){
                key.interestOps(SelectionKey.OP_READ);
                return;
            }
            int bytesWritten = socketChannel.write(ByteBuffer.wrap(block));
            System.out.println("Bytes written:" + bytesWritten);
        } catch (IOException e) {
            key.cancel();
            try {
                socketChannel.close();
            } catch (Exception e1) {
                e.printStackTrace();
            }
            System.out.println("Socket closed!");
        }
    }

    private static void sendFileSize(SocketChannel socketChannel, long fileLength) throws IOException {
        byte[] buffer = new byte[8];
        buffer[0] = (byte) (fileLength >>> 56);
        buffer[1] = (byte) (fileLength >>> 48);
        buffer[2] = (byte) (fileLength >>> 40);
        buffer[3] = (byte) (fileLength >>> 32);
        buffer[4] = (byte) (fileLength >>> 24);
        buffer[5] = (byte) (fileLength >>> 16);
        buffer[6] = (byte) (fileLength >>> 8);
        buffer[7] = (byte) (fileLength >>> 0);

        socketChannel.write(ByteBuffer.wrap(buffer));
    }

    private static ServerSocketChannel getServerSocket() {
        ServerSocketChannel serverChannel = null;
        int port = -1;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        while (serverChannel == null) {
            try {
                while (port == -1) {
                    try {
                        System.out.print("Input server port > ");
                        port = Integer.valueOf(input.readLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Wrong port format!");
                    }
                }
                serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);
                serverChannel.socket().bind(new InetSocketAddress(port));
            } catch (Exception e) {
                System.out.println("can't start server on this port!");
                port = -1;
            }
        }
        System.out.println(
                "Server successfully started: " + port);
        return serverChannel;
    }
}