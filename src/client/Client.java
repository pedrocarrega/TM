package client;

import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;

public class Client {

	private static List<String> clients;

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException {

		Scanner sc = new Scanner(System.in);
		System.out.println("1 - Watch Stream\n2 - Host Stream \nChoose your action: ");
		int input = Integer.parseInt(sc.nextLine());

		if(1 <= input && input <= 3)
			connectServer(args[0], Integer.parseInt(args[1]), input, sc);
		else
			System.err.println("INVALID ACTION");

	}

	private static void connectServer(String ip, int port, int action, Scanner sc) throws UnknownHostException, IOException, ClassNotFoundException{


		switch (action) {
		case 1:
			String host = getHost(ip, port, action, sc);
			if(host != null)
				connectStream(host);
			break;

		case 2:
			stream(ip, port, action, sc);
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

		while(LocalTime.now().getSecond()-timer.getSecond()<45) {
			byte[] stuff = (byte[])inStream.readObject();
			System.out.println("Size: " + stuff.length);
		}

		socket.close();
		inStream.close();

	}

	private static String getHost(String ip, int port, int action, Scanner sc) throws ClassNotFoundException, IOException {

		Socket socket = new Socket(ip, port);

		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
		outStream.writeObject(action);
		
		String opcoes = (String) inStream.readObject();
		System.out.println("\n" + opcoes);

		System.out.println("Escreva o canal que pretende visualizar das opções acima listadas: ");

		String opcao = sc.nextLine();

		outStream.writeObject(opcao);
		String result = (String) inStream.readObject();
		System.out.println(result);
		
		socket.close();
		outStream.close();
		inStream.close();

		return result;

	}	

	private static int warnServer(String ip, int port, int action, Scanner sc) throws IOException, ClassNotFoundException {
		Socket socket = new Socket(ip, port);

		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
		
		outStream.writeObject(action);
		System.out.println("Indique o seu userId: ");

		int userId = Integer.parseInt(sc.nextLine());
		
		outStream.writeObject(userId);
		
		int result = (int) inStream.readObject();
		
		while (result == -1) {
			System.out.println("UserId nao unico, indique outro: ");

			userId = Integer.parseInt(sc.nextLine());
			
			outStream.writeObject(userId);
			
			result = (int) inStream.readObject();
		}

		socket.close();
		outStream.close();
		inStream.close();
		
		return result;
		
	}
	
	private static void stream(String ip, int port, int action, Scanner sc) throws UnknownHostException, IOException, ClassNotFoundException {
		int sPort = warnServer(ip, port, action, sc);
		
		System.out.println("Port: " + sPort);
		
		Client client = new Client();
		client.startServer(sPort);

	}
	
	private void startServer(int port) throws IOException {

		@SuppressWarnings("resource")
		ServerSocket socket = new ServerSocket(port);

		while(true) {
			new SimpleServer(socket.accept()).start();
		}

	}
	
	class SimpleServer extends Thread{

		private Socket socket = null;

		public SimpleServer(Socket accept) {
			this.socket = accept;
		}

		public void run() {
			try {
				
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				
				LocalTime timer = LocalTime.now();
				
				while((LocalTime.now().getSecond()-timer.getSecond())<30) 
					outStream.writeObject(new byte[1500]);

				socket.close();
				outStream.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
