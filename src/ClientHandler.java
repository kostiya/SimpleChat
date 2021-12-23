import server.ServerThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler extends Thread{
    BlockingQueue<Event> queue = new LinkedBlockingQueue<>();

    private Socket socket;
    private BufferedReader inSocket;
    private PrintWriter outSocket;
    private boolean userInConnectionProcess = false;

    public

    @Override
    public void run() {
        init();//connect to remote client and send available users

        while (true) {
            Event event = null;

            try {
                event = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(event instanceof QuitEvent) {
                break;
            }
            event.run();
        }
    }

    private void init() {
        initReaderWriter();
        sendAvailableUsers();
    }

    private void initReaderWriter() {
        // initialize in, out using socket
        initInputThread();
    }


    private void connectToUser(String user) {
        queue.add(() -> {
            if(userInConnectionProcess) {
                //handle error - this user is currently unavaliable for new connection
            }
            userInConnectionProcess = true;

            ClientHandler otherUserHandler = users.get(user);
            otherUserHandler.handleConnectionRequest(this.name);

        });
    }

    public void handleChatInitiateFailed(String requestedFailedUser) {
        queue.add(() -> {
            socketOut.write("You failed to initiate a chat with: " + requestedFailedUser);
            sendAvailableUsers();
        });
    }

    private void sendAvailableUsers() {
        for(String user: users) {
            socketOut.write(user);
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
        String line = inSocket.readLine();
        if(line.contains("connect")) {
            String user = line.split(" ")[1];
            connectToUser(user);
        } else if(){

        } else if () {

        }
    }
}
