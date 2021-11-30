package server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
	private static final int SERVER_PORT = 2020;

	public ServerMain() throws Exception{
		
		ServerSocket server_socket = new ServerSocket(SERVER_PORT);
		System.out.println("Port " + SERVER_PORT + " is now open.");
		
		// infinite while loop: wait for new connections
		while(true) {
			Socket socket = server_socket.accept();
			ServerThread server_thread = new ServerThread(socket);
			Thread thread = new Thread(server_thread);
			thread.start();
		}
		
	}
	
	public static void main(String[] args) {
		try {
			new ServerMain();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
