package client;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;

public class Client {

	private static final long TIME_BETWEEN_FRAMES = 1500;
	private static final int BUFFER_SIZE = 5;
	private static final int TTL = 15;
	private static List<Socket> clients;
	private static Map<Integer, List<String>> tabela;
	private static final int MAX_CLIENT_SIZE = 30;

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException, InterruptedException {

		Scanner sc = new Scanner(System.in);
		//System.out.println("1 - Watch Stream\n2 - Host Stream \nChoose your action: ");
		if(args.length > 0) {
			String initialIp = args[0];
			String initialPort = args[1];

			IntStream.range(0, 30).forEach((int i) -> {
				try {
					randomWalk(initialIp, initialPort);
				} catch (NumberFormatException | ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}

		//Client client = new Client();
		//client.startServer(12345);
		SimpleServer server = new SimpleServer(12345);
		server.start();
		String comando;
		while(!(comando = sc.nextLine()).equals("exit")) {
			if(comando.equals("Visualizar")) {
				
			}
			switch(comando) {
			  case "Visualizar":
				System.out.println("Escolha umdos seguintes canais");
			    imprimeStreams();
			    boolean verifierVisualiza = true;
			    while(verifierVisualiza) {
				    String escolhaVis = sc.nextLine();
				    if(tabela.containsKey(Integer.parseInt(escolhaVis))){
				    	visualizarStream(Integer.parseInt(escolhaVis));
				    	verifierVisualiza = false;
				    	 System.out.println("Esta a assistir ao canal " + escolhaVis);
				    }else {
				    	System.out.println("Insira um canal v�lido");
				    }
			    }
			    break;
			  case "Transmitir":
			    System.out.println("Insira um ID unico para o seu canal");
			    boolean verifierTransmission = true;
			    while(verifierTransmission) {
			    	String escolhaId = sc.nextLine();
			    	if(!tabela.containsKey(Integer.parseInt(escolhaId))) {
			    		startTransmission(Integer.parseInt(escolhaId));
			    		verifierTransmission = false;
			    	}else {
			    		System.out.println("Id de canal j� est� em uso, tente de novo");
			    	}
			    }
			    break;
			  default:
			    System.out.println("Imprima um dos Seguintes comandos:");
			    System.out.println("Visualizar");
			    System.out.println("Transmitir");
			}
		}
		
		
		/*
		int input = Integer.parseInt(sc.nextLine());

		if(1 <= input && input <= 3)
			connectServer(args[0], Integer.parseInt(args[1]), input, sc);
		else
			System.err.println("INVALID ACTION");*/

	}
	

	private static void startTransmission(int idCanalTrans) {
		// TODO Ter� de iniciar uma thread para a tranmissao caso necess�rio e fazer gossip a avisar que est� a tranmitir esta stream
		
	}


	private static void visualizarStream(int idCanalVis) {
		// TODO Criar uma liga��o ao ip que estah na lista tabela e pedir para ele nos transmitir a stream
	}


	private static void imprimeStreams() {
		
		for(Integer i : tabela.keySet()) {
			System.out.println(i);
		}
		
	}


	private static void randomWalk(String ip, String port) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		Socket socket = new Socket(ip, Integer.parseInt(port));
		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

		outStream.writeObject("RandomWalk," + TTL + "," + socket.getLocalSocketAddress().toString().substring(1));


		
		String[] info = (socket.getRemoteSocketAddress().toString().substring(1)).split(":");
		System.out.println(socket.getRemoteSocketAddress().toString().substring(1));

		socket.close();
		outStream.close();

		ServerSocket server = new ServerSocket(Integer.parseInt(info[1]));
		Socket newSocket = server.accept();
		ObjectInputStream in = new ObjectInputStream(newSocket.getInputStream());

		int response = (int)in.readObject();

		if(response == 1) {

			//boolean result = true;

			synchronized (clients) {

				/*for(Socket compare : clients) {
					String[] something = (compare.getLocalAddress().toString().substring(1)).split(":");

					if(info[0].equals(something[0])) {
						result = false;
						break;
					}*/
				clients.add(newSocket);
				System.out.println("entrou");
				//}

				/*if(result)
					clients.add(newSocket);*/
			}
		}else {
			System.out.println("Errors");
		}

		newSocket.close();
		in.close();
		server.close();



	}



	private static void connectServer(String ip, int port, int action, Scanner sc) throws UnknownHostException, IOException, ClassNotFoundException, NumberFormatException, InterruptedException{


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

	private static void connectStream(String host) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException, InterruptedException {

		String[] info = host.split(":");
		ArrayBlockingQueue<byte[]> buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);


		Socket socket = new Socket(info[0], Integer.parseInt(info[1]));
		//ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
		//clients = new ArrayList<String>();

		Runnable r = () -> {
			try {
				while(true) {
					Thread.sleep(TIME_BETWEEN_FRAMES);
					if(buffer.isEmpty()) {
						System.out.println(buffer.remove().length);
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};

		Thread t = new Thread(r);
		t.start();

		LocalTime timer = LocalTime.now();

		while(LocalTime.now().getSecond()-timer.getSecond()<45) {
			buffer.put((byte[])inStream.readObject());
		}

		t.join();
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

		System.out.println("Escreva o canal que pretende visualizar das opcoes acima listadas: ");

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

		SimpleServer server = new SimpleServer(port);
		server.start();
		
		System.out.println("asdasdasdasfsdfds");
	}

	static class SimpleServer extends Thread implements Runnable{

		private ServerSocket socket = null;

		public SimpleServer(int port) {
			try {
				this.socket = new ServerSocket(port);
			} catch (IOException e) {
				System.out.println("Erro na criacao da server socket");
			}
		}

		@Override
		public void run() {

			Socket socketAceite;
			while(true) {
				try {
					if((socketAceite = this.socket.accept()) != null) {
						
						ObjectInputStream inStream = new ObjectInputStream(socketAceite.getInputStream());
						ObjectOutputStream outStream = new ObjectOutputStream(socketAceite.getOutputStream());

						String[] info = ((String)inStream.readObject()).split(",");

						if(info[0].equals("RandomWalk")) {
							String[] address = info[2].split(":");
							if(clients.size() < MAX_CLIENT_SIZE) {

								Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1]));

								boolean result = true;

								synchronized (clients) {

									for(Socket compare : clients) {
										String[] something = (compare.getLocalAddress().toString().substring(1)).split(":");

										if(address[0].equals(something[0])) {
											result = false;
											break;
										}
									}

									if(result) {
										clients.add(newVizinho);
										System.out.println("tenho fome"); 
									}

								}
							}else {
								int ttl = Integer.parseInt(info[1]) - 1;
								if(ttl > 0) {
									Random r = new Random();
									Socket reencaminhar = clients.get(r.nextInt(clients.size()));
									ObjectOutputStream out = new ObjectOutputStream(reencaminhar.getOutputStream());
									out.writeObject("RandomWalk," + ttl + "," + reencaminhar.getLocalAddress().toString().substring(1));
								}else {
									Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1]));
									ObjectOutputStream out = new ObjectOutputStream(newVizinho.getOutputStream());

									out.writeObject(-1);

									newVizinho.close();
									out.close();
								}

							}
						}
						
					}
				} catch (IOException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
