package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {
	private ConcurrentHashMap<String, ServerThread> users = new ConcurrentHashMap<>();

	public ServerMain() throws Exception{
		
		ServerSocket server_socket = new ServerSocket(2020);
		System.out.println("Port 2020 is now open.");
		
		// infinite while loop: wait for new connections
		while(true) {
			Socket socket = server_socket.accept();
			ServerThread server_thread = new ServerThread(socket, users);
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
