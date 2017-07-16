package com.glnosg;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class BeatBoxServer {

    ArrayList<ObjectOutputStream> outputStreamsList;

    public static void main(String[] args) {
        new BeatBoxServer().setUp();
    }

    public void setUp() {
        outputStreamsList = new ArrayList<ObjectOutputStream>();
        try {
            ServerSocket serverSocket = new ServerSocket(47017);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                outputStreamsList.add(out);

                Thread clientListener = new Thread(new ClientListener(clientSocket));
                clientListener.start();

                System.out.println("New client connected");
                System.out.println("Number of connected clients: " + outputStreamsList.size());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendToAll(Object obj1, Object obj2) {
        for (ObjectOutputStream os : outputStreamsList) {
            try {
                os.writeObject(obj1);
                os.writeObject(obj2);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class ClientListener implements Runnable {

        ObjectInputStream in;
        Socket clientSocket;

        public ClientListener (Socket socket) {
            try {
                clientSocket = socket;
                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void run() {
            Object obj1;
            Object obj2;
            try {
                while((obj1 = in.readObject()) != null) {
                    obj2 = in.readObject();

                    sendToAll(obj1, obj2);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

