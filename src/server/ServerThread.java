package server;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread implements Runnable {

	private Socket socket;
	private ConcurrentHashMap<String, ServerThread> users;
	private String username;
	private volatile String connecting_user = null;
	private volatile ConnectionType connection_type = ConnectionType.NOT_CONNECTED;
	ServerThread (Socket socket, ConcurrentHashMap<String, ServerThread> users) {
		this.socket = socket;
		this.users = users;
	}

	private synchronized void setConnectingUserAndType(String connectingUser, ConnectionType connectionType) {
		this.connecting_user = connectingUser;
		this.connection_type = connectionType;
	}

	private static String cleanInput(String name){
		if(name != null) {
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
			BufferedReader in_socket = new BufferedReader(new InputStreamReader (socket.getInputStream()));
			PrintWriter out_socket = new PrintWriter(new OutputStreamWriter (socket.getOutputStream()), true);
			
			out_socket.println("Welcome! What's your user name? (No Spaces)"); // send "Welcome" to the Client
			do{
				username = in_socket.readLine(); // receive name
				username = cleanInput(username); //get first no space word
				if(users.get(username) != null) {
					out_socket.println("User already connected. Please try other User");
				} else {
					break;
				}
			}while(true);

			users.put(username, this);
			out_socket.println("Username " + username + " is connected.");
			//User is connected

			while(true) {
				InputDaemon inputDaemon = new InputDaemon(in_socket, out_socket, this);
				inputDaemon.setDaemon(true);
				inputDaemon.start();

				while (connecting_user == null) {
					Thread.sleep(100);
				}

				inputDaemon.interrupt();

				ServerThread otherUserServerThread = users.get(connecting_user);
				//Need to test that the variable is not null;
				Socket otherUserSocket = otherUserServerThread.socket;
				String otherUsername = otherUserServerThread.username;

				// Other user I/O buffers:
				BufferedReader other_in_socket = new BufferedReader(new InputStreamReader(otherUserSocket.getInputStream()));
				PrintWriter other_out_socket = new PrintWriter(new OutputStreamWriter(otherUserSocket.getOutputStream()), true);

				String answer;

				if (connection_type == ConnectionType.CONNECTION_REQUESTED) {
					out_socket.println("Do you want to connect with " + otherUsername + ". Yy/Nn?");
					answer = cleanInput(in_socket.readLine());
					if (!(answer.equals("Y") || answer.equals("y"))) {
						connecting_user = null;
						connection_type = ConnectionType.NOT_CONNECTED;
						otherUserServerThread.connecting_user = null;
						otherUserServerThread.connection_type = ConnectionType.NOT_CONNECTED;
						continue;
					} else{
						connection_type = ConnectionType.CONNECTED;
						otherUserServerThread.connection_type = ConnectionType.CONNECTED;
					}
				} else if(connection_type == ConnectionType.INITIATED_CONNECTION){
					out_socket.println("Waiting for answer from " + otherUsername + ".");
					while(connection_type == ConnectionType.INITIATED_CONNECTION){
						Thread.sleep(100);
					}
					if(connection_type == ConnectionType.NOT_CONNECTED){
						continue;
					}
				}

				other_out_socket.println("Hi "+ otherUsername);
				if(!in_socket.readLine().equals("Hi " + username)){
					System.out.println("Error in connection");
				}


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
			System.out.println("Socket is closed.");
		}
	}

	private static void startChat(){

	}

	private static class InputDaemon extends Thread {
		BufferedReader in_socket;
		PrintWriter out_socket;
		ServerThread serverThread;
		public InputDaemon(BufferedReader in_socket, PrintWriter out_socket, ServerThread serverThread){
			this.in_socket = in_socket;
			this.out_socket = out_socket;
			this.serverThread = serverThread;
		}

		@Override
		public void run() {
			String username = serverThread.username;
			ConcurrentHashMap<String, ServerThread> users = serverThread.users;
			while (!Thread.interrupted()) {

				out_socket.println("Please enter the username of other user to chat:");
				for (Map.Entry<String, ServerThread> map : users.entrySet()) {
					if (!map.getKey().equals(username)) {
						out_socket.println(map.getKey());
					}
				}
				String otherUser = null;
				try {
					otherUser = in_socket.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				otherUser = cleanInput(otherUser);
				ServerThread otherUserServerThread = users.get(otherUser);
				if (otherUserServerThread == null) {
					out_socket.println("User doesn't exist.");
					continue;
				} else if (otherUserServerThread.connecting_user != null){
					out_socket.println("Other user is already connected");
					continue;
				} else if(serverThread.connecting_user != null){
					return;
				}

				synchronized (serverThread){
					synchronized (otherUserServerThread){
						otherUserServerThread.setConnectingUserAndType(username, ConnectionType.CONNECTION_REQUESTED);
						serverThread.setConnectingUserAndType(otherUserServerThread.username, ConnectionType.INITIATED_CONNECTION);
					}
				}
				break;

			}
		}
	}
}
