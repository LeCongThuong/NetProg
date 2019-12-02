package client;

import helpers.FileHelper;
import helpers.MessageControlHelper;
import helpers.MessageControlHelper.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Controller {
    // client info
    private static int id;
    private static ClientAddress[] clientAddress = new ClientAddress[3];

    // server info
    private static final int serverPort = 9090;
//    private static final String serverIp = "127.0.0.1";
    private static final String serverIp = "192.168.2.2";

    // input utils
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;

    // output utils
    private static OutputStream outputStream = null;
    private static DataOutputStream dataOutputStream = null;

    public static void main(String[] args) {
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIp);
            Socket socket = new Socket(serverAddress, serverPort);

            // Input init
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // identify client
            id = dataInputStream.readByte();
            System.out.println("C" + id + " connected to server.");

            // get other client info
            String message = dataInputStream.readUTF();
            String[] address = message.split(" ", 3);
            for (int i = 0; i < 3; i++) {
                clientAddress[i] = new ClientAddress(address[i]);
            }

            if (id == 1) {
                executeAsForwarderClient();
            } else {
                executeAsNormalClient();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void executeAsForwarderClient() throws IOException {
        // c1 wait for c2 and c3 connect
        ServerSocket serverSocket = new ServerSocket(9000);
        dataOutputStream.writeBoolean(true);
        System.out.println("Listening at port 9000");

        // connect c2
        Socket c2 = serverSocket.accept();
        System.out.println("C2(" + clientAddress[1].getIp() + ":" + clientAddress[1].getPort() + ") connected");
        DataOutputStream c2OutputStream = new DataOutputStream(c2.getOutputStream());

        // connect c3
        Socket c3 = serverSocket.accept();
        System.out.println("C3(" + clientAddress[2].getIp() + ":" + clientAddress[2].getPort() + ") connected");
        DataOutputStream c3OutputStream = new DataOutputStream(c3.getOutputStream());

        while (true) {
            // forward file info
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(dataInputStream);
            System.out.println("Forwarder: " + fileInfo.fileName + " " + fileInfo.fileSize);
            MessageControlHelper.sendFileInfo(c2OutputStream, fileInfo);
            MessageControlHelper.sendFileInfo(c3OutputStream, fileInfo);
            // Receive and Forward file
            FileHelper.forwardFile(dataInputStream, new ArrayList<>(Arrays.asList(c2OutputStream, c3OutputStream)), "./c1/" + fileInfo.fileName, fileInfo.fileSize);
            long finishTime = System.currentTimeMillis();
            dataOutputStream.writeLong(finishTime);
            System.out.println("Receive file " + fileInfo.fileName + " successfully.");
        }
    }

    private static void executeAsNormalClient() throws IOException {
        Socket c1 = new Socket(clientAddress[0].getIp(), 9000);
        dataOutputStream.writeBoolean(true);
        System.out.println("Connected to C1 at " + clientAddress[0].getIp() + ":9000");
        DataInputStream c1InputStream = new DataInputStream(c1.getInputStream());
        while (true) {
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(c1InputStream);
            System.out.println("Receiver" + fileInfo.fileName + fileInfo.fileSize);
            String filepath = "./c" + id + "/" + fileInfo.fileName;
            FileHelper.receiveFile(c1InputStream, filepath, fileInfo.fileSize);
            long finishTime = System.currentTimeMillis();
            dataOutputStream.writeLong(finishTime);
            System.out.println("Receive file " + fileInfo.fileName + " successfully.");
        }
    }
}
