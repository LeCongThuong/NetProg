package server;

import java.net.*;
import java.io.*;

import helpers.FileHelper;
import helpers.MessageControlHelper;

public class ClientConnection {
    // client info
    private int id;
    private ServerSocket servConnection;
    private Socket controlSocket;
    private Socket[] dataSockets;
    private int port;
    private String ip;
    private static final int bufferSize = 1024;
    private static final int nThreads = 2;

    // input
    private InputStream inputStream = null;
    private DataInputStream dataInputStream = null;

    // output
    private OutputStream outputStream = null;
    private DataOutputStream dataOutputStream = null;

    public ClientConnection(int clientId, ServerSocket connection) {
        this.id = clientId;
        this.servConnection = connection;
        try {
            controlSocket = connection.accept();
            dataSockets = new Socket[nThreads];
        } catch (IOException e) {
            System.out.println("Accept client connection error!");
            e.printStackTrace();
        }
        ip = controlSocket.getInetAddress().toString().replace("/", "");
        port = controlSocket.getPort();
        try {
            // Input init
            inputStream = controlSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = controlSocket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // Send clientId back to client
            dataOutputStream.write(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: handle error
    public void sendMessage(String message) {
        try {
            dataOutputStream.writeUTF(message);
        } catch (Exception e) {
            System.out.println("Can not send message to client");
        }
    }

    // TODO: handle error
    public boolean isSuccess() throws IOException {
        return dataInputStream.readBoolean();
    }

    public void sendFile(String fileName) {
        try {
            // TODO: remove when done testing
            File file = new File("server/" + fileName);
            long fileSize = file.length();
            MessageControlHelper.sendFileInfo(dataOutputStream, new MessageControlHelper.FileInfo(fileName, fileSize));
            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
                dataSockets[threadIndex] = this.servConnection.accept();
                DataOutputStream parallelOutputStream = new DataOutputStream(dataSockets[threadIndex].getOutputStream());
                // TODO: handle file partitioning
                new Thread(() -> {
                    try {
                        FileHelper.sendFile(parallelOutputStream, "server/" + fileName);
                    } catch (IOException e) {
                        System.out.println("Error in file sending threads");
                        e.printStackTrace();
                    }
                }).start();
//                FileHelper.sendFile(parallelOutputStream, fileName);
            }

        } catch (IOException e) {
            System.out.println("Something went wrong when send file");
        }
    }

    // TODO: handle error
    public long getFinishTime() throws IOException {
        return dataInputStream.readLong();
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }
}
