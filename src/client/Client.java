package client;

import java.util.List;
import java.util.Scanner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;

public class Client {

	private static List<String> clients;

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException {

		Scanner sc = new Scanner(System.in);
		int input = Integer.parseInt(sc.nextLine());
		
		if(1 <= input && input <= 3)
			connectServer(args[0], Integer.parseInt(args[1]), input, sc);
		else
			System.err.println("INVALID ACTION");

	}

	private static void connectServer(String ip, int port, int action, Scanner sc) throws UnknownHostException, IOException, ClassNotFoundException{
		Socket socket = new Socket(ip, port);

		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

		switch (action) {
		case 1:
			String host = getHost(inStream, outStream, sc);
			socket.close();
			outStream.close();
			inStream.close();
			if(host != null)
				connectStream(host);
			break;

		case 2:
			stream();
			socket.close();
			outStream.close();
			inStream.close();
			break;

		}



		


	}

	private static void connectStream(String host) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		
		String[] info = host.split(":");
		
		
		Socket socket = new Socket(info[0], Integer.parseInt(info[1]));
		//ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
		clients = new ArrayList<String>();
		
		LocalTime timer = LocalTime.now();
		
		while(LocalTime.now().getSecond()-timer.getSecond()<60) {
			System.out.println((String)inStream.readObject());
		}
		
		socket.close();
		inStream.close();
		
	}

	private static String getHost(ObjectInputStream in, ObjectOutputStream out, Scanner sc) throws ClassNotFoundException, IOException {
		
		String opcoes = (String) in.readObject();
		System.out.println(opcoes);

		System.out.println("Escreva o canal que pretende visualizar das opções acima listadas");

		String opcao = sc.nextLine();

		out.writeObject(opcao);
		String result = (String) in.readObject();
		System.out.println(result);
		
		return result;
		
	}

	private static void stream() {

	}
}
