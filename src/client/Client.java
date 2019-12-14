package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

	private static final long TIME_BETWEEN_FRAMES = 1500;
	private static final int BUFFER_SIZE = 5;
	private static final int TTL = 15;
	private static List<Socket> viewers;
	private static List<Socket> toAdd = new ArrayList<>();
	private static List<Socket> toRemove = new ArrayList<>();
	private static List<Socket> transmissores;
	private static List<Socket> clients;
	private static Map<Integer, List<String>> tabela;
	private static final int MAX_CLIENT_SIZE = 30;
	private static final int THREASHOLD_VIZINHOS = 5;
	private static String localIp;
	private final static int probToGossip = 70;
	private static List<Integer> streams;
	private static int TIME_TO_GOSSIP = 5000;//5s por cada gossip

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException, InterruptedException {

		Scanner sc = new Scanner(System.in);
		//System.out.println("1 - Watch Stream\n2 - Host Stream \nChoose your action: ");
		clients = new ArrayList<>();



		viewers = new ArrayList<Socket>();
		tabela = new HashMap<>();
		transmissores = new ArrayList<Socket>();

		Listen listen = new Listen();
		listen.start();
		if(args.length > 0) {
			String initialIp = args[0];
			String initialPort = args[1];

			IntStream.range(0, MAX_CLIENT_SIZE).forEach((int i) -> {
				try {
					System.out.println("random: " + i);
					randomWalk(initialIp, initialPort);
				} catch (NumberFormatException | ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			});
		}else {
			SimpleServer server = new SimpleServer(12345);
			server.start();
		}

		streams = new ArrayList<>();
		SporadicGossip gossipTemporal = new SporadicGossip();//Faz gossip esporadico para avisar novos cliente que stream ele pode transmitir ou retransmitir
		gossipTemporal.start();

		String comando;
		boolean avaliador = true;
		System.out.println("Insira o comando que deseja:");
		while(!(comando = sc.nextLine()).equals("exit")) {
			switch(comando.toLowerCase()) {
			case "visualizar":
				boolean verifierVisualiza = imprimeStreams();
				while(verifierVisualiza && avaliador) {
					String escolhaVis = sc.nextLine();
					if(tabela.containsKey(Integer.parseInt(escolhaVis))){
						visualizarStream(Integer.parseInt(escolhaVis));
						verifierVisualiza = false;
						System.out.println("Esta a assistir ao canal " + escolhaVis);
					}else {
						if(tabela.size() == 0) {
							System.out.println("De momento nao existem streams disponiveis");
							avaliador = false;
						}else {
							System.out.println("Insira um canal valido");
						}
					}
				}
				break;
			case "transmitir":
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

		synchronized (clients) {
			for(Socket s : clients) {
				s.close();
			}
		}		

		sc.close();
	}


	private static void startTransmission(int idCanalTrans) throws InterruptedException {
		//gossip
		if(clients.size() > 0) {
			informaVizinhos("Gossip," + idCanalTrans + "," + localIp + "," + TTL);
		}

		int arrSize = 1000;
		char[] arrayEnviado = new char[arrSize];
		for(int i = 0; i < arrSize; i++) {
			arrayEnviado[i] = (char)i;
		}

		Transmit canal = new Transmit(arrayEnviado, idCanalTrans);
		canal.start();

		synchronized(streams) {
			streams.add(idCanalTrans);
		}

		canal.join();

		informaVizinhos("End," + idCanalTrans + "," + TTL);

		synchronized(streams) {
			streams.remove((Integer)idCanalTrans);
		}

	}


	private static void informaVizinhos(String string) {
		// Fazer Gossip
		if(clients.size() > 0) {
			int probGossip = probToGossip;

			if(clients.size() < THREASHOLD_VIZINHOS) {
				probGossip = 100;
			}

			List<Socket> gossip = new ArrayList<>();

			int size = (clients.size()*probGossip)/100;

			Random r = new Random();

			for(int i = 0; i < size; i++) {
				Socket temp = clients.get(r.nextInt(clients.size()));
				if(!gossip.contains(temp)) {
					gossip.add(temp);
					try {
						ObjectOutputStream out = new ObjectOutputStream(temp.getOutputStream());
						out.writeObject(string);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}


	private static void visualizarStream(int idCanalVis) {
		List<String> streamers = tabela.get(idCanalVis);

		if(streamers != null && streamers.size() > 0) {
			Random r = new Random();
			String streamerEscolhido = streamers.get(r.nextInt(streamers.size()));

			int response = checkIfExists(streamerEscolhido);

			Socket streamer = null;
			if(response >= 0) {
				streamer = clients.get(response);
			}else {
				try {
					streamer = new Socket(streamerEscolhido, 12345);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//System.out.println("Stream: " + streamer);
			ObjectOutputStream out;
			try {
				out = new ObjectOutputStream(streamer.getOutputStream());
				out.writeObject("Visualizar,");
			} catch (IOException e) {
				e.printStackTrace();
			}


			//avisar vizinhos que tb tranmitirei esta stream
			informaVizinhos("Gossip," + idCanalVis + "," + localIp + "," + TTL);

			synchronized(streams) {
				streams.add(idCanalVis);
			}
		}else {
			System.out.println("Nao existem mais transmissores da stream, tera de aguardar ate que seja encontrada um novo");
		}
	}


	private static boolean imprimeStreams() {

		if(tabela.size() > 0) {
			System.out.println("Escolha um dos seguintes canais");
			for(Integer i : tabela.keySet()) {
				System.out.println(i);
			}
			return true;
		}else {
			System.out.println("Nao existem canais disponiveis para visualizar");
			return false;
		}

	}


	private static void randomWalk(String ip, String port) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {

		Socket socket = null;
		int result = -1;
		int portS;

		synchronized (clients) {

			result = checkIfExists(ip);

		}
		if(result < 0) {
			socket = new Socket(ip, Integer.parseInt(port));
			portS = socket.getLocalPort()+2;
			localIp = socket.getLocalAddress().toString().substring(1);
		}else {
			socket = clients.get(result);
			//System.out.println(socket);
			portS = socket.getLocalPort()+2;
		}

		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

		//System.out.println(socket.getLocalSocketAddress().toString().substring(1));

		outStream.writeObject("RandomWalk," + TTL + "," + socket.getLocalSocketAddress().toString().substring(1));

		if(result < 0) {
			socket.close();
			outStream.close();
		}
		System.out.println(portS);
		ServerSocket server = new ServerSocket(portS);
		Socket newSocket = server.accept();
		System.out.println("server: " + newSocket.getLocalPort());

		ObjectInputStream in = new ObjectInputStream(newSocket.getInputStream()); 
		//fica preso AQUI caso tenhamos 1+ clientes

		System.out.println("resposta " );
		int response = (int)in.readObject();


		if(response == 1) {


			synchronized (clients) {

				toAdd.add(newSocket);
				System.out.println("entrou");

			}
		}else {
			//System.out.println("Errors");
			newSocket.close();
			in.close();

		}

		server.close();

	}

	private static int checkIfExists(String ipCompare) {

		int counter = 0;

		for(Socket compare : clients) {
			String[] something = (compare.getRemoteSocketAddress().toString().substring(1)).split(":");

			if(ipCompare.equals(something[0])) {
				return counter;
			}
			counter++;
		}

		return -1;
	}


	private static class SimpleServer extends Thread implements Runnable{

		private ServerSocket socket = null;

		public SimpleServer(int port) {
			try {
				this.socket = new ServerSocket(port);
			} catch (IOException e) {
				e.printStackTrace();
				//System.out.println("Erro na criacao da server socket");
			}
		}

		@Override
		public void run() {

			Socket socketAceite = null;
			Socket socketRemoved = null;
			int idStreamCrashed = 0;
			while(true) {
				try {
					//System.out.println("antes de ligar: " + this.socket);
					socketAceite = this.socket.accept();
					localIp = socketAceite.getLocalAddress().toString().substring(1);

					//System.out.println("ligou");

					ObjectInputStream inStream = new ObjectInputStream(socketAceite.getInputStream());


					//String[] info = ((String)inStream.readObject()).split(",");
					//System.out.println(info[2]);
					String recebido = (String) inStream.readObject();
					String[] info = new String[1];


					if(recebido.contains(",")) {
						info = recebido.split(",");
					}else {
						int val = (int)recebido.charAt(0);
						info[0] = val+"";
					}

					//if(info[0].equals("RandomWalk")) {
					switch(info[0]) {	
					case "RandomWalk":
						socketAceite.close();
						inStream.close();
						String[] address = info[2].split(":");
						int result = checkIfExists(address[0]);

						if(clients.size() < MAX_CLIENT_SIZE) {

							//System.out.println(address[0] + " " + Integer.parseInt(address[1]));
							Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1])+2);

							synchronized (clients) {

								if(result < 0) {
									toAdd.add(newVizinho);

									//System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));
									ObjectOutputStream outStream = new ObjectOutputStream(newVizinho.getOutputStream());
									outStream.writeObject(1);
									//System.out.println("Close: " + newVizinho.isClosed());
								}

							}
						}
						if(result >= 0 || clients.size() >= MAX_CLIENT_SIZE) {
							int ttl = Integer.parseInt(info[1]) - 1;
							if(ttl > 0) {
								Random r = new Random();
								Socket reencaminhar = clients.get(r.nextInt(clients.size()));
								//System.out.println("vou mandar");
								ObjectOutputStream out = new ObjectOutputStream(reencaminhar.getOutputStream());
								out.writeObject("RandomWalk," + ttl + "," + info[2]);
							}else {
								System.out.println("Fuck ttl");
								Socket newVizinho;
								ObjectOutputStream out;
								/*if(result >= 0) {
									newVizinho = clients.get(result);
									out = new ObjectOutputStream(newVizinho.getOutputStream());

									out.writeObject(-1);
								}else {
									newVizinho = new Socket(address[0], Integer.parseInt(address[1]));
									out = new ObjectOutputStream(newVizinho.getOutputStream());

									out.writeObject(-1);

									newVizinho.close();
									out.close();
								}*/
								newVizinho = new Socket(address[0], Integer.parseInt(address[1])+2);
								out = new ObjectOutputStream(newVizinho.getOutputStream());

								out.writeObject(-1);

								newVizinho.close();
								out.close();
							}

						}
						break;
					case "Visualizar":
						viewers.add(socketAceite);
						break;
					default:
						synchronized(clients) {
							toAdd.add(socketAceite);
						}
						int i = info[1].charAt(0);
						idStreamCrashed = i;
						System.out.println("recebido " + i);
						for(Socket s : viewers) {
							ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
							out.writeObject(info[1]);
						}							
						break;
					}
				} catch ( ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					System.out.println("entrou aqui simple server");
					socketRemoved = socketAceite;
					String[] addressToRemove = socketRemoved.getRemoteSocketAddress().toString().split(":");
					removeStreamerTable(idStreamCrashed, addressToRemove[0]);
					visualizarStream(idStreamCrashed);//em teoria vai buscar outro streamer
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			clients.remove(socketRemoved);
			viewers.remove(socketRemoved);
		}




	}

	private static boolean removeStreamerTable(int idStreamCrashed, String userCrashed) {

		List<String> streamers = tabela.get(idStreamCrashed);

		if(streamers != null) {
			return streamers.remove(userCrashed);
		}else {
			return false;
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

		@SuppressWarnings("unused")
		@Override
		public void run() {
			//boolean run = true;
			Socket socketRemoved = null;
			int idStreamCrashed = 0;
			while(true) {

				System.out.print(""); //ISTO SO CORRE POR CAUSA DISTO!!!

				for(Socket socket : clients) {
					ObjectInputStream in = null;
					try {
						//System.out.println("before in");
						if(in != null) {
							in.reset();
						}

						in = new ObjectInputStream(socket.getInputStream());

						String recebido = (String) in.readObject();
						String[] info = new String[1];



						if(recebido.contains(",")) {
							info = recebido.split(",");
						}else {
							int val = (int)recebido.charAt(0);
							info[0] = val+"";
						}

						switch (info[0]) {
						case "RandomWalk":

							int result = -1;
							String[] address = info[2].split(":");
							if(clients.size() < MAX_CLIENT_SIZE) {

								//System.out.println(address[0] + " " + Integer.parseInt(address[1]));

								result = checkIfExists(address[0]);


								synchronized (clients) {

									if(result < 0) {
										System.out.println("listen: " + address[1]);
										Socket newVizinho = new Socket(address[0], Integer.parseInt(address[1])+2);
										toAdd.add(newVizinho);
										//clients.add(newVizinho);
										//newVizinho = new Socket(address[0], Integer.parseInt(address[1])+2);
										//System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));
										ObjectOutputStream outStream = new ObjectOutputStream(newVizinho.getOutputStream());
										outStream.writeObject(1);
										//outStream.close();//fechar a socket que manda a resposta
										//newVizinho.close();
									}

								}
							}
							if(result >= 0 || clients.size() >= MAX_CLIENT_SIZE) {
								
								int ttl = Integer.parseInt(info[1]) - 1;
								System.out.println("TTL: "+ ttl);								
								if(ttl > 0) {
									//System.out.println("TTL GOOD");
									Random r = new Random();
									Socket reencaminhar = clients.get(r.nextInt(clients.size()));
									//System.out.println("porta de resposta port=" + socket.getRemoteSocketAddress());
									ObjectOutputStream out = new ObjectOutputStream(reencaminhar.getOutputStream());
									out.writeObject("RandomWalk," + ttl + "," + info[2]);
								}else {
									//System.out.println("TTL BAD");
									Socket newVizinho;
									ObjectOutputStream out;
									/*if(result >= 0) {
										newVizinho = clients.get(result);
										out = new ObjectOutputStream(newVizinho.getOutputStream());

										out.writeObject(-1);
									}else {
										newVizinho = new Socket(address[0], Integer.parseInt(address[1])+1);
										out = new ObjectOutputStream(newVizinho.getOutputStream());

										out.writeObject(-1);

										newVizinho.close();
										out.close();
									}*/
									
									System.out.println("test: " + (address[1]+2));
									newVizinho = new Socket(address[0], Integer.parseInt(address[1])+2);
									
									out = new ObjectOutputStream(newVizinho.getOutputStream());

									out.writeObject(-1);
									out.flush();
									//System.out.println("test: " + (Integer.parseInt(address[1])+2));

									newVizinho.close();
									out.close();
								}
							}

							break;

						case "Gossip":
							List<String> listStreams = tabela.get(Integer.parseInt(info[1]));
							if(listStreams != null) {
								if(listStreams.size() < 3) {
									if(!listStreams.contains(info[2])) {
										listStreams.add(info[2]);
									}
								}
							}else {
								listStreams = new ArrayList<String>();
								listStreams.add(info[2]);
								tabela.put(Integer.parseInt(info[1]), listStreams);
							}
							int currentTTL = Integer.parseInt(info[3])-1;
							if(currentTTL >= 0) {
								informaVizinhos(info[0]+ "," + info[1]+ "," + info[2] + "," + currentTTL);
							}
							//System.out.println(info[0]+ "," + info[1]+ "," + info[2] + "," + currentTTL);
							break;
						case "Visualizar":
							//System.out.println("Este adicionou me :" + socket);
							viewers.add(socket);
							break;

						case "End":
							tabela.remove(Integer.parseInt(info[1]));
							System.out.println("Acabou a transmissao " + info[1]);
							if((Integer.parseInt(info[2])-1) >= 0) {
								informaVizinhos(info[0]+ "," + info[1]+ "," + (Integer.parseInt(info[2])-1));
							}
							synchronized(streams) {
								streams.remove((Integer)Integer.parseInt(info[1]));//id do canal a remover
							}
							break;
						default:
							//Caso que recebe dados de uma transmissao
							int i = info[1].charAt(0);
							idStreamCrashed = i;
							System.out.println("recebido " + i);
							for(Socket s : viewers) {
								ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
								out.writeObject(recebido);
							}
							break;
						}

						//in.close();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (SocketException e) { //socket exception
						e.printStackTrace();
						System.out.println("entrou aqui listen");
						socketRemoved = socket;
						toRemove.add(socketRemoved);
						String[] addressToRemove = socketRemoved.getRemoteSocketAddress().toString().split(":");
						
						if(removeStreamerTable(idStreamCrashed, addressToRemove[0])) {
							visualizarStream(idStreamCrashed); //em teoria vai buscar outro streamer
						}
						
						break;
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				
				clients.addAll(toAdd);
				clients.removeAll(toRemove);
				//viewers.removeAll(toRemove);
				//clients.remove(socketRemoved);
				viewers.remove(socketRemoved);
			}
		}
	}

	private static class Transmit extends Thread implements Runnable{

		private char[] arrToTransmit;
		private int streamId;

		public Transmit(char[] arr, int id) {
			viewers = new ArrayList<>();
			arrToTransmit = arr;
			this.streamId = id;
		}

		@Override
		public void run() {

			//			arrToTransmit[0]++;
			Socket socketRemoved = null;
			//int counter = 0;

			for(char c : arrToTransmit) {
				//System.out.println("entre os for's " + viewers.size());
				for(Socket viewer : viewers) {
					ObjectOutputStream out = null;
					try {
						out = new ObjectOutputStream(viewer.getOutputStream());
						int val = (int)c;
						System.out.println("enviado: " + val);
						out.writeObject("Stream,"+c+"," + this.streamId);
						//out.writeObject(val+""); //precisas de enviar 1000 bytes
					} catch (IOException e) {
						socketRemoved = viewer;
						break;
					}
				}
				viewers.remove(socketRemoved);
				clients.remove(socketRemoved);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				/*counter++;
				if(counter % 50 == 0) {
					informaVizinhos("Gossip," + streamId + "," + localIp + "," + TTL);
					counter = 0;
				}*/
			}
			System.out.println("Acabou de transmitir");
		}
	}

	private static class SporadicGossip extends Thread implements Runnable{

		public SporadicGossip(){
		}

		@Override
		public void run() {

			while(true) {
				for(int idStream : streams) {
					informaVizinhos("Gossip," + idStream + "," + localIp + "," + TTL);
				}
				try {
					Thread.sleep(TIME_TO_GOSSIP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}//faz gossip a cada 5s
			}
		}

	}
}
