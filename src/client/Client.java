package client;

import java.util.ArrayList;
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

public class Client {

	private static final long TIME_BETWEEN_FRAMES = 1500;
	private static final int BUFFER_SIZE = 5;
	private static final int TTL = 15;
	private static List<Socket> viewers;
	private static List<Socket> clients;
	private static Map<Integer, List<String>> tabela;
	private static final int MAX_CLIENT_SIZE = 30;
	private static final int THREASHOLD_VIZINHOS = 5;

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException, InterruptedException {

		Scanner sc = new Scanner(System.in);
		//System.out.println("1 - Watch Stream\n2 - Host Stream \nChoose your action: ");
		clients = new ArrayList<>();
		if(args.length > 0) {
			String initialIp = args[0];
			String initialPort = args[1];

			IntStream.range(0, 30).forEach((int i) -> {
				try {
					System.out.println("random: " + i);
					randomWalk(initialIp, initialPort);
				} catch (NumberFormatException | ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			});
		}else { //isto?
			SimpleServer server = new SimpleServer(12345);
			server.start();
		}
		
		Listen listen = new Listen();
		listen.start();
		String comando;
		while(!(comando = sc.nextLine()).equals("exit")) {
			if(comando.equals("Visualizar")) {

			}
			switch(comando) {
			case "Visualizar":
				System.out.println("Escolha um dos seguintes canais");
				imprimeStreams();
				boolean verifierVisualiza = true;
				while(verifierVisualiza) {
					String escolhaVis = sc.nextLine();
					if(tabela.containsKey(Integer.parseInt(escolhaVis))){
						visualizarStream(Integer.parseInt(escolhaVis));
						verifierVisualiza = false;
						System.out.println("Esta a assistir ao canal " + escolhaVis);
					}else {
						System.out.println("Insira um canal valido");
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
						System.out.println("Id de canal ja esta em uso, tente de novo");
					}
				}
				break;
			default:
				System.out.println("Imprima um dos Seguintes comandos:");
				System.out.println("Visualizar");
				System.out.println("Transmitir");
			}
		}

		sc.close();


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

		Socket socket = null;
		boolean result = true;

		synchronized (clients) {

			for(Socket compare : clients) {
				String[] something = (compare.getLocalAddress().toString().substring(1)).split(":");

				if(ip.equals(something[0])) {
					socket = compare;
					result = false;
					break;
				}
			}

		}
		if(result) {
			socket = new Socket(ip, Integer.parseInt(port));
		}

		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

		System.out.println(socket.getLocalSocketAddress().toString().substring(1));

		outStream.writeObject("RandomWalk," + TTL + "," + socket.getLocalSocketAddress().toString().substring(1));

		int portS = socket.getLocalPort()+1;

		socket.close();
		outStream.close();

		ServerSocket server = new ServerSocket(portS);
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
			newSocket.close();
			in.close();
			server.close();
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

	private static class SimpleServer extends Thread implements Runnable{

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
					socketAceite = this.socket.accept();

					System.out.println("ligou");

					ObjectInputStream inStream = new ObjectInputStream(socketAceite.getInputStream());


					String[] info = ((String)inStream.readObject()).split(",");
					//System.out.println(info[2]);
					socketAceite.close();
					inStream.close();

					if(info[0].equals("RandomWalk")) {
						boolean result = true;
						String[] address = info[2].split(":");
						if(clients.size() < MAX_CLIENT_SIZE) {

							System.out.println(address[0] + " " + Integer.parseInt(address[1]));
							Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1])+1);



							synchronized (clients) {

								for(Socket compare : clients) {
									String[] something = (compare.getRemoteSocketAddress().toString().substring(1)).split(":");
									System.out.println("Address: " + address[0] + " Compare: " + something[0]);

									if(address[0].equals(something[0])) {
										result = false;
										break;
									}
								}

								if(result) {
									clients.add(newVizinho);

									System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));
									ObjectOutputStream outStream = new ObjectOutputStream(newVizinho.getOutputStream());
									outStream.writeObject(1);
									//outStream.close();
									System.out.println("Close: " + newVizinho.isClosed());
								}

							}
						}
						if(!result || clients.size() >= MAX_CLIENT_SIZE) {
							int ttl = Integer.parseInt(info[1]) - 1;
							if(ttl > 0) {
								Random r = new Random();
								Socket reencaminhar = clients.get(r.nextInt(clients.size()));
								ObjectOutputStream out = new ObjectOutputStream(reencaminhar.getOutputStream());
								out.writeObject("RandomWalk," + ttl + "," + reencaminhar.getLocalSocketAddress().toString().substring(1));
								out.close();
							}else {
								Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1]));
								ObjectOutputStream out = new ObjectOutputStream(newVizinho.getOutputStream());

								out.writeObject(-1);

								newVizinho.close();
								out.close();
								inStream.close();
							}

						}

					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}

			}
		}


	}

	/*
	 * Usado para comunica�ao entre sockets que ja estao abertas
	 * ou seja random walks com alguem que j� foi adicionado aos vizinhos
	 * ou entao para receber gossip dos vizinhos e saber propaga-lo
	 * Tamebm tem de incluir os casos em que a pessoa que ira assistir
	 * ah nossa stream ja seja nossa vizinha e use essa socket para 
	 * nos pedir para o adicionar � lista de viewers da stream
	 */
	private static class Listen extends Thread implements Runnable{

		public Listen() {}

		@Override
		public void run() {
			boolean run = true;

			while(run) {

				System.out.println("GAS GAS GAS");

				for(Socket socket : clients) {
					ObjectInputStream in;
					try {
						in = new ObjectInputStream(socket.getInputStream());


						String[] info = ((String)in.readObject()).split(",");

						switch (info[0]) {
						case "RandomWalk":

							boolean result = true;
							String[] address = info[2].split(":");
							if(clients.size() < MAX_CLIENT_SIZE) {

								System.out.println(address[0] + " " + Integer.parseInt(address[1]));



								synchronized (clients) {

									for(Socket compare : clients) {
										String[] something = (compare.getLocalAddress().toString().substring(1)).split(":");

										if(address[0].equals(something[0])) {
											result = false;
											break;
										}
									}

									if(result) {
										Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1]));
										clients.add(newVizinho);
										System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));
										ObjectOutputStream outStream = new ObjectOutputStream(newVizinho.getOutputStream());
										outStream.writeObject(1);
										outStream.close();
									}

								}
							}
							if(!result || clients.size() >= MAX_CLIENT_SIZE) {
								int ttl = Integer.parseInt(info[1]) - 1;
								if(ttl > 0) {
									Random r = new Random();
									Socket reencaminhar = clients.get(r.nextInt(clients.size()));
									ObjectOutputStream out = new ObjectOutputStream(reencaminhar.getOutputStream());
									out.writeObject("RandomWalk," + ttl + "," + reencaminhar.getLocalSocketAddress().toString().substring(1));
									out.close();
								}else {
									Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1]));
									ObjectOutputStream out = new ObjectOutputStream(newVizinho.getOutputStream());

									out.writeObject(-1);

									newVizinho.close();
									out.close();
								}

							}

							break;

						case "Disconnect":

							break;

						default:
							break;
						}

						in.close();
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}

				}

			}
		}
	}

	private static class Transmit extends Thread implements Runnable{

		private char[] arrToTransmit;

		public Transmit(char[] arr) {
			viewers = new ArrayList<>();
			arrToTransmit = arr;
		}

		@Override
		public void run() {

			arrToTransmit[0]++;

			for(Socket viewer : viewers) {
				try {
					ObjectOutputStream out = new ObjectOutputStream(viewer.getOutputStream());
					out.writeObject(arrToTransmit); //precisas de enviar 1000 bytes
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
