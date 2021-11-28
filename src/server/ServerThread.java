package server;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread implements Runnable {

	private Socket socket;
	private ConcurrentHashMap<String, ServerThread> users;
	private String username;
	private String connectingUser;
	private ConnectionType connectionType;
	ServerThread (Socket socket, ConcurrentHashMap<String, ServerThread> users) {
		this.socket = socket;
		this.users = users;
	}

	public synchronized void setConnectingUserAndType(String connectingUser, ConnectionType connectionType) {
		this.connectingUser = connectingUser;
		this.connectionType = connectionType;
	}

	public Socket getSocket() {
		return socket;
	}

	private static String cleanInput(String name){
		return name.strip().split("\\s")[0];
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

			while (true) {
				out_socket.println("Please enter the username of other user to chat:");
				for (Map.Entry<String, ServerThread> map : users.entrySet()) {
					if (!map.getKey().equals(username)) {
						out_socket.println(map.getKey());
					}
				}
				String otherUser = in_socket.readLine();
				otherUser = cleanInput(otherUser);
				ServerThread otherUserServerThread = users.get(otherUser);
				if (otherUserServerThread == null) {
					continue;
				}

				otherUserServerThread.setConnectingUserAndType(username, ConnectionType.INITIATE_CONNECTION);
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

	private static class InputDaemon extends Thread {

		@Override
		public void run() {

		}
	}
}
