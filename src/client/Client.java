package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	public Client() throws Exception {

		Scanner keyboard = new Scanner (System.in);
		System.out.println("Please Enter Server IP/Host:");
		String hostOrIP = keyboard.nextLine();
		System.out.println("Please Enter Server Port:");
		int port = keyboard.nextInt();
		Socket socket = new Socket(hostOrIP,port);
		System.out.println("Successful connection to the server.");
		
		// I/O streams
		BufferedReader in_socket = new BufferedReader (new InputStreamReader (socket.getInputStream()));
		PrintWriter out_socket = new PrintWriter (new OutputStreamWriter (socket.getOutputStream()), true);

		System.out.println("Please Enter Your Username from the list:");
		String userInList;
		do{
			userInList = in_socket.readLine();
			if(!userInList.equals("\\n")){
				System.out.println(userInList);
			} else {
				break;
			}
		}while(1==1);

		String username = keyboard.nextLine();
		out_socket.println(username);
		String answer_message = in_socket.readLine();

		while(!answer_message.equals("Connected")){
			System.out.println(answer_message);
			username = keyboard.nextLine();
			out_socket.println(username);
			answer_message = in_socket.readLine();
		}
		
		socket.close();
		System.out.println("Socket closed.");
		
	}
	
	public static void main(String[] args) {
		try {
			new Client();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
