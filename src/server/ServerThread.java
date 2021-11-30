package server;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread implements Runnable {

    private Socket socket;
    private static ConcurrentHashMap<String, ServerThread> users = new ConcurrentHashMap<>();
    private String username;
    private ArrayBlockingQueue<UserConnectionPair> connectionQueue;
    private ArrayBlockingQueue<Boolean> waitingForAnswerQueue;
    private boolean isConnected = false;

    ServerThread(Socket socket) {
        this.socket = socket;
        connectionQueue = new ArrayBlockingQueue<>(1, false);
        waitingForAnswerQueue = new ArrayBlockingQueue<>(1, false);
    }

    private static String cleanInput(String name) {
        if (name != null) {
            return name.strip().split("\\s")[0];
        } else {
            return null;
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Client has connected.");

            // I/O buffers:
            BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter outSocket = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            outSocket.println("Welcome! What's your user name? (No Spaces)"); // send "Welcome" to the Client
            int count = 3;
            do {
                username = inSocket.readLine(); // receive name
                username = cleanInput(username); //get first no space word
                if (!username.isBlank()) {
                    outSocket.println("Username is blank. Please try again");
                    count--;
                } else if (users.get(username) != null) {
                    outSocket.println("User already connected. Please try other User");
                    count--;
                } else {
                    break;
                }

            } while (count > 0);

            if (count == 0) {
                outSocket.println("To many tries. Exiting...");
                socket.close();
                return;
            }

            users.put(username, this);
            outSocket.println("Username " + username + " is connected.");
            //User is connected

            while (!Thread.interrupted()) {
                InputDaemon inputDaemon = new InputDaemon(inSocket, outSocket, this);
                inputDaemon.setDaemon(true);
                inputDaemon.start();

                UserConnectionPair userPair = connectionQueue.take();

                inputDaemon.interrupt();

                ServerThread otherUserServerThread;
                boolean isInitiating;
                if (userPair.initiatingUser.equals(username)) {
                    otherUserServerThread = users.get(userPair.acceptingUser);
                    isInitiating = true;
                } else {
                    otherUserServerThread = users.get((userPair.initiatingUser));
                    isInitiating = false;
                }
                Socket otherUserSocket = otherUserServerThread.socket;
                String otherUsername = otherUserServerThread.username;

                // Other user I/O buffers:
                BufferedReader otherInSocket = new BufferedReader(new InputStreamReader(otherUserSocket.getInputStream()));
                PrintWriter otherOutSocket = new PrintWriter(new OutputStreamWriter(otherUserSocket.getOutputStream()), true);

                String answer;

                if (!isInitiating) {
                    outSocket.println("Do you want to connect with " + otherUsername + ". Yy/Nn?");
                    answer = cleanInput(inSocket.readLine());
                    if (!(answer.equals("Y") || answer.equals("y"))) {
                        isConnected = false;
                        otherUserServerThread.isConnected = false;
                        otherUserServerThread.waitingForAnswerQueue.offer(Boolean.FALSE);
                        continue;
                    }
                } else if (isInitiating) {
                    outSocket.println("Waiting for answer from " + otherUsername + ".");
                    boolean connectionAnswer = waitingForAnswerQueue.take().booleanValue();
                    if (!connectionAnswer) {
                        continue;
                    }
                }

                otherOutSocket.println("Hi " + otherUsername);
                if (!inSocket.readLine().equals("Hi " + username)) {
                    System.out.println("Error in connection");
                }

                ChatPipeDaemon chatPipeDaemon = new ChatPipeDaemon(inSocket, username, otherOutSocket);
                chatPipeDaemon.setDaemon(true);
                chatPipeDaemon.start();
                Thread.sleep(1000000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Socket is closed.");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            users.remove(username);
        }
    }

    private static class InputDaemon extends Thread {
        BufferedReader inSocket;
        PrintWriter outSocket;
        ServerThread serverThread;

        public InputDaemon(BufferedReader inSocket, PrintWriter outSocket, ServerThread serverThread) {
            this.inSocket = inSocket;
            this.outSocket = outSocket;
            this.serverThread = serverThread;
        }

        @Override
        public void run() {
            String username = serverThread.username;
            ConcurrentHashMap<String, ServerThread> users = serverThread.users;
            while (!Thread.interrupted()) {

                outSocket.println("Please enter the username of other user to chat:");
                for (Map.Entry<String, ServerThread> map : users.entrySet()) {
                    if (!map.getKey().equals(username)) {
                        outSocket.println(map.getKey());
                        outSocket.println();
                    }
                }
                String otherUser = null;
                try {
                    otherUser = inSocket.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                otherUser = cleanInput(otherUser);
                ServerThread otherUserServerThread = users.get(otherUser);
                if (otherUserServerThread == null) {
                    outSocket.println("User doesn't exist.");
                    continue;
                }
                synchronized (otherUserServerThread) {
                    synchronized (serverThread) {
                        if (!otherUserServerThread.connectionQueue.isEmpty() || otherUserServerThread.isConnected) {
                            outSocket.println("Other user is already connected");
                            continue;
                        } else if (!serverThread.connectionQueue.isEmpty() || serverThread.isConnected) {
                            return;
                        }


                        UserConnectionPair pair = new UserConnectionPair();
                        pair.initiatingUser = username;
                        pair.acceptingUser = otherUser;
                        serverThread.connectionQueue.offer(pair);
                        otherUserServerThread.connectionQueue.offer(pair);
                        serverThread.isConnected = true;
                        otherUserServerThread.isConnected = true;
                    }
                }
                break;

            }
        }
    }

    private static class ChatPipeDaemon extends Thread {
        private final BufferedReader from_socket;
        private final String from_username;
        private final PrintWriter to_socket;

        public ChatPipeDaemon(BufferedReader from_socket, String from_username, PrintWriter to_socket) {
            this.from_socket = from_socket;
            this.from_username = from_username;
            this.to_socket = to_socket;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    to_socket.println(from_username + ": " + from_socket.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class UserConnectionPair {
        String initiatingUser = null;
        String acceptingUser = null;
    }

}
