import java.io.*;
import java.net.Socket;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler extends Thread {
    BlockingQueue<Event> queue = new LinkedBlockingQueue<>();

    private static String username;
    private Socket socket;
    private BufferedReader inSocket;
    private PrintWriter outSocket;
    private static ConcurrentHashMap<String, ClientHandler> users = new ConcurrentHashMap<>();
    private ClientHandler connectedTo = null;
    private boolean connectionInProgress = false;

    private static String getFirstString(String name) {
        if (name != null) {
            return name.strip().split("\\s")[0];
        } else {
            return null;
        }
    }

    private static String getSecondSubstring(String name) {
        if (name != null) {
            return name.strip().split("\\s")[1];
        } else {
            return null;
        }
    }

    @Override
    public void run() {
        init();

        initInputThread();

        while (true) {
            Event event = null;

            try {
                event = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (event instanceof QuitEvent) {
                break;
            }
            event.run();
        }
    }

    private void init() {
        initReaderWriter();
        outSocket.println("Please choose user your username:");

        boolean success = initUsername();

        if (!success) {
            outSocket.println("To many tries. Exiting...");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            queue.add(new QuitEvent());
            return;
        }
    }

    private boolean initUsername() {
        int count = 3;
        do {
            try {
                username = inSocket.readLine(); // receive name
            } catch (IOException e) {
                e.printStackTrace();
            }
            username = getFirstString(username); //get first no space word
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

        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void initReaderWriter() {
        // initialize in, out using socket
        try {
            inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outSocket = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAvailableUsers() {
        for (Map.Entry<String, ClientHandler> user : users.entrySet()) {
            outSocket.println(user.getKey());
        }
    }

    private void initInputThread() {
        Thread inputThread = new Thread(() -> {
            while (true) {
                handleClientInput();
            }
        });
        inputThread.start();
    }

    private void handleClientInput() {
        if (connectedTo == null) {
            sendAvailableUsers();
        }
        String line = null;
        try {
            line = inSocket.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (line != null && getFirstString(line).contains("connect")) {
            String user = getSecondSubstring(line);
            initiateConnectionToUser(user);
        } /*else if(){

        } else if() {

        }*/
    }

    private synchronized void initiateConnectionToUser(String otherUser) {
        ClientHandler otherUserHandler = users.get(otherUser);
        otherUserHandler.handleConnectionRequest(username, false);
    }

    public void handleChatInitiateFailed(String requestedFailedUser) {
        queue.add(() -> {
            outSocket.println("You failed to initiate a chat with: " + requestedFailedUser);
        });
    }

    private void handleConnectionRequest(String otherUser, boolean initiator) {
        queue.add(() -> {
            if(!initiator) {
                ClientHandler otherUserHandler = users.get(otherUser);
                if (connectedTo == null) {
                    outSocket.println("Do you accept connection from: " + otherUser);
                    String input = null;
                    try {
                        input = inSocket.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(input != null && (input.toLowerCase().equals("yes")||input.toLowerCase().equals("y"))){
                        otherUserHandler.handleConnectionRequest(username, true);
                    }else{
                        otherUserHandler.handleChatInitiateFailed(username)
                    }

                    connectedTo = otherUserHandler;
                } else {
                    otherUserHandler.handleChatInitiateFailed(username);
                }
            } else if(initiator){
                ClientHandler otherUserHandler = users.get(otherUser);
                if (connectedTo == null) {

                }

                    connectedTo = otherUserHandler;
                } else {
                    otherUserHandler.handleChatInitiateFailed(username);
                }
            }

        });
    }
}
