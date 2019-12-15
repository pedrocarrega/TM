package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

	private static final long TIME_BETWEEN_FRAMES = 150;
	private static final int TTL = 5;
	private static List<Node> viewers;
	private static List<Node> toAdd = new ArrayList<>();
	private static List<Node> toRemove = new ArrayList<>();
	private static List<Node> clients;
	private static Map<Integer, List<String>> tabela;
	private static final int MAX_CLIENT_SIZE = 30;
	private static final int THREASHOLD_VIZINHOS = 5;
	private static String localIp;
	private final static int probToGossip = 70;
	private static List<Integer> streams;
	private static int TIME_TO_GOSSIP = 15000;//5s por cada gossip
	private static LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>() ;

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException, InterruptedException {

		Scanner sc = new Scanner(System.in);
		//System.out.println("1 - Watch Stream\n2 - Host Stream \nChoose your action: ");
		clients = new ArrayList<>();
		streams = new ArrayList<>();
		



		viewers = new ArrayList<>();
		tabela = new HashMap<>();
		new ArrayList<Socket>();

		Listen listen = new Listen();
		listen.start();
		if(args.length > 0) {
			String initialIp = args[0];
			String initialPort = args[1];
			
			for(int i = 0; i < MAX_CLIENT_SIZE/2; i++){
				try {
					System.out.println("random: " + i);
					randomWalk(initialIp, initialPort);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}//else {
			SimpleServer server = new SimpleServer(12345);
			server.start();
		//}

		//SporadicGossip gossipTemporal = new SporadicGossip();//Faz gossip esporadico para avisar novos cliente que stream ele pode transmitir ou retransmitir
		//gossipTemporal.start();

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
						verStream(Integer.parseInt(escolhaVis));
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
			for(Node n : clients) {
				n.close();
			}
		}
		sc.close();
	}


	private static void verStream(int escolhaVis) throws InterruptedException {
		do{
			if(!buffer.isEmpty()) {
			System.out.println("Recebi " + buffer.remove());
			}
			Thread.sleep(TIME_BETWEEN_FRAMES);
		}while (tabela.containsKey(escolhaVis) || !buffer.isEmpty());
		
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

			List<Node> gossip = new ArrayList<>();

			int size = (clients.size()*probGossip)/100;

			Random r = new Random();

			for(int i = 0; i < size; i++) {
				Node temp = clients.get(r.nextInt(clients.size()));
				if(!gossip.contains(temp)) {
					gossip.add(temp);
					try {
						ObjectOutputStream out = temp.getOutputStream();
						//out.flush();
						out.writeObject(string);
					} catch (IOException e) {
						toRemove.add(temp);
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

			Node streamer = null;
			if(response >= 0) {
				streamer = clients.get(response);
			}else {
				try {
					streamer = new Node(new Socket(streamerEscolhido, 12345));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//System.out.println("Stream: " + streamer);
			ObjectOutputStream out;
			try {
				out = streamer.getOutputStream();
				//out.flush();
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


	private static void randomWalk(String ip, String port){

		Node node;
		Socket socket;
		int result = -1;
		int portS;
		
		//System.out.println("Size: " + clients.size());

		try {
			synchronized (clients) {
		

			result = checkIfExists(ip);

		}
		if(result < 0) {
			
				node = new Node(new Socket(ip, Integer.parseInt(port)));
			
			socket = node.getSocket();
			portS = socket.getLocalPort()+2;
			localIp = socket.getLocalAddress().toString().substring(1);
		}else {
			node = clients.get(result);
			socket = node.getSocket();
			System.out.println(socket);
			portS = socket.getLocalPort()+2;
		}

		ObjectOutputStream outStream = node.getOutputStream();
		//outStream.flush();

		System.out.println(socket.getLocalSocketAddress().toString().substring(1));

		outStream.writeObject("RandomWalk," + TTL + "," + socket.getLocalSocketAddress().toString().substring(1));

		if(result < 0) {
			socket.close();
			outStream.close();
		}
		System.out.println(portS);
		ServerSocket server = new ServerSocket(portS);
		Node newNode = new Node(server.accept());
		System.out.println("server: " + newNode.getSocket().getLocalAddress());

		ObjectInputStream in = newNode.getInputStream(); 
		//fica preso AQUI caso tenhamos 1+ clientes

		//System.out.println("resposta " );
		int response = (int)in.readObject();


		if(response == 1) {


			synchronized (clients) {

				toAdd.add(newNode);
				System.out.println("entrou");

			}
		}else {
			//System.out.println("Errors");
			newNode.close();

		}
		server.close();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		

	}

	private static int checkIfExists(String ipCompare) {

		int counter = 0;

		for(Node node : clients) {
			Socket compare = node.getSocket();
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

			Node nodeAceite = null;
			Node nodeRemoved;
			int idStreamCrashed = 0;
			
			while(true) {
				try {
					//System.out.println("antes de ligar: " + this.socket);
					nodeAceite = new Node(this.socket.accept());
					localIp = nodeAceite.getSocket().getLocalAddress().toString().substring(1);

					System.out.println("ligou");

					ObjectInputStream inStream = nodeAceite.getInputStream();


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
						nodeAceite.close();
						String[] address = info[2].split(":");
						int result = checkIfExists(address[0]);

						if(clients.size() < MAX_CLIENT_SIZE) {

							//System.out.println("size: " + clients.size());
							//System.out.println(address[0] + " " + Integer.parseInt(address[1]));


							synchronized (clients) {

								if(result < 0) {

									if(!address[0].equals(localIp)) {
										Node newVizinho = null;
										try {
											System.out.println("adicionar");
										newVizinho = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
										} catch (Exception e) {
											e.printStackTrace();
											// TODO: handle exception
										}
										int port = (Integer.parseInt(address[1])+2);
										System.out.println(address[0] + ":" + port);
										//System.out.println(newVizinho.getSocket().getRemoteSocketAddress().toString());
										
										
										toAdd.add(newVizinho);

										//System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));
										ObjectOutputStream outStream = newVizinho.getOutputStream();
										//outStream.flush();
										outStream.writeObject(1);
										//System.out.println("Close: " + newVizinho.isClosed());
									}else {
										result = 0;
									}
								}

							}
						}
						if(result >= 0 || clients.size() >= MAX_CLIENT_SIZE) {
							int ttl = Integer.parseInt(info[1]) - 1;
							if(ttl > 0) {
								Random r = new Random();
								Node reencaminhar = clients.get(r.nextInt(clients.size()));
								//System.out.println("vou mandar");
								ObjectOutputStream out = reencaminhar.getOutputStream();
								//out.flush();
								out.writeObject("RandomWalk," + ttl + "," + info[2]);
							}else {
								//System.out.println("Fuck ttl");
								Node newVizinho;
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
								newVizinho = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
								out = newVizinho.getOutputStream();
								//out.flush();

								out.writeObject(-1);

								newVizinho.close();
							}

						}
						break;
					case "Visualizar":
						viewers.add(nodeAceite);
						break;
					default:
						synchronized(clients) {
							toAdd.add(nodeAceite);
						}
						int i = info[1].charAt(0);
						idStreamCrashed = i;
						//System.out.println("recebido " + i);
						for(Node n : viewers) {
							ObjectOutputStream out = n.getOutputStream();
							//out.flush();
							out.writeObject(info[1]);
						}							
						break;
					}
				} catch ( ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					//System.out.println("entrou aqui simple server");
					nodeRemoved = nodeAceite;
					String[] addressToRemove = nodeRemoved.getSocket().getRemoteSocketAddress().toString().split(":");
					removeStreamerTable(idStreamCrashed, addressToRemove[0]);
					visualizarStream(idStreamCrashed);//em teoria vai buscar outro streamer
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			clients.remove(nodeRemoved);
			viewers.remove(nodeRemoved);
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
	 * Usado para comunicao entre sockets que ja estao abertas
	 * ou seja random walks com alguem que ja foi adicionado aos vizinhos
	 * ou entao para receber gossip dos vizinhos e saber propaga-lo
	 * Tamebm tem de incluir os casos em que a pessoa que ira assistir
	 * ah nossa stream ja seja nossa vizinha e use essa socket para 
	 * nos pedir para o adicionar a lista de viewers da stream
	 */
	private static class Listen extends Thread implements Runnable{

		public Listen() {}

		
		
		@SuppressWarnings("unused")
		@Override
		public void run() {
			//boolean run = true;
			Node nodeRemoved = null;
			int idStreamCrashed = 0;
			while(true) {

				System.out.print(""); //ISTO SO CORRE POR CAUSA DISTO!!!

				for(Node node : clients) {
					ObjectInputStream in;
					try {
						//System.out.println("before in");

						in = node.getInputStream();
						
						String recebido;

						if(!(recebido = (String) in.readObject()).equals("")) {
						String[] info = new String[1];



						if(recebido.contains(",")) {
							info = recebido.split(",");
						}else {
							int val = (int)recebido.charAt(0);
							info[0] = val+"";
						}

						switch (info[0]) {
						case "RandomWalk":
							System.out.println("random time");

							int result = -1;
							String[] address = info[2].split(":");
							if(clients.size() < MAX_CLIENT_SIZE) {

								System.out.println("size: " + clients.size());
								System.out.println(address[0] + " " + Integer.parseInt(address[1]));

								result = checkIfExists(address[0]);


								synchronized (clients) {

									if(result < 0) {
										System.out.println("listen: " + address[1]);
										if(!address[0].equals(localIp)) {
											Node newVizinho = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));										
											toAdd.add(newVizinho);
											ObjectOutputStream outStream = newVizinho.getOutputStream();
											//outStream.flush();
											outStream.writeObject(1);
										}else {
											result = 0;
										}
										//clients.add(newVizinho);
										//newVizinho = new Socket(address[0], Integer.parseInt(address[1])+2);
										//System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));

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
									Node reencaminhar = clients.get(r.nextInt(clients.size()));
									//System.out.println("porta de resposta port=" + socket.getRemoteSocketAddress());
									ObjectOutputStream out = reencaminhar.getOutputStream();
									//out.flush();
									out.writeObject("RandomWalk," + ttl + "," + info[2]);
									//System.out.println("mandei");
								}else {
									//System.out.println("TTL BAD");
									Node newVizinho;
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

									//System.out.println("test: " + address[1] + "ttl: " + ttl);
									newVizinho = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));

									out = newVizinho.getOutputStream();
									//out.flush();

									out.writeObject(-1);

									//System.out.println("mandei2");

									//System.out.println("test: " + (Integer.parseInt(address[1])+2));

									newVizinho.close();
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
							viewers.add(node);
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
							buffer.add(i);
							idStreamCrashed = i;
							//System.out.println("recebido " + i);
							for(Node n : viewers) {
								ObjectOutputStream out = n.getOutputStream();
								//out.flush();
								out.writeObject(recebido);
							}
							break;
						}
						}
						//in.close();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (SocketException e) { //socket exception
						//e.printStackTrace();
						System.out.println("entrou aqui listen");
						nodeRemoved = node;
						toRemove.add(nodeRemoved);
						String[] addressToRemove = nodeRemoved.getSocket().getRemoteSocketAddress().toString().split(":");

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
				toAdd.removeAll(toAdd);
				toRemove.removeAll(toRemove);
				//viewers.removeAll(toRemove);
				//clients.remove(socketRemoved);
				viewers.remove(nodeRemoved);
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
			Node nodeRemoved = null;
			//int counter = 0;

			for(char c : arrToTransmit) {
				//System.out.println("entre os for's " + viewers.size());
				for(Node viewer : viewers) {
					ObjectOutputStream out = null;
					try {
						out = viewer.getOutputStream();
						//out.flush();
						int val = (int)c;
						System.out.println("enviado: " + val);
						out.writeObject("Stream,"+c+"," + this.streamId);
						//out.writeObject(val+""); //precisas de enviar 1000 bytes
					} catch (IOException e) {
						nodeRemoved = viewer;
						break;
					}
				}
				viewers.remove(nodeRemoved);
				clients.remove(nodeRemoved);
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
			
			Random r = new Random();

			while(true) {
				if(clients.size() > 0 && clients.size() < MAX_CLIENT_SIZE - 15) {
					for (int i = 0; i < 15; i++) {
						Node n = clients.get(r.nextInt(clients.size()));
						String[] info = n.getSocket().getRemoteSocketAddress().toString().substring(1).split(":");
						try {
							randomWalk(info[0], info[1]);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
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