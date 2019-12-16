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
import java.time.LocalTime;

public class Client {

	private static int status = 1;
	private static boolean run = true;
	private static final long TIME_BETWEEN_FRAMES = 50;
	private static final int TTL = 5;
	private static List<Node> viewers = new ArrayList<>();
	private static List<Node> toAdd = new ArrayList<>();
	private static List<Node> toRemove = new ArrayList<>();
	private static List<Node> clients = new ArrayList<>();
	private static Map<Integer, List<String>> tabela = new HashMap<>();
	private static final int MAX_CLIENT_SIZE = 1;
	private static final int THREASHOLD_VIZINHOS = 5;
	private static String localIp;
	private final static int probToGossip = 70;
	private static final long DELAY_TIME = 2000;
	private static final int TIMEOUT = 30;
	private static List<Integer> streams = new ArrayList<>();
	private static int TIME_TO_GOSSIP = 15000; //5s por cada gossip
	private static LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>();

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException, InterruptedException {

		Scanner sc = new Scanner(System.in);
		//System.out.println("1 - Watch Stream\n2 - Host Stream \nChoose your action: ");

		if(args.length > 0) {
			String initialIp = args[0];
			String initialPort = args[1];

			for(int i = 0; i < MAX_CLIENT_SIZE; i++){
				try {
					//System.out.println("random: " + i);
					randomWalk(initialIp, initialPort);
				} catch (NumberFormatException e) {
					//e.printStackTrace();
				}
			}
			if(clients.size() == 0) {
				Socket temp = new Socket(initialIp,Integer.parseInt(initialPort));
				Node entradaNaRede = new Node(temp);
				entradaNaRede.randomWalk("RandomWalk," + TTL + "," + temp.getLocalSocketAddress().toString().substring(1) + ",urgent");
				ServerSocket server = new ServerSocket(temp.getLocalPort()+2);
				entradaNaRede.close();
				Node newNode = new Node(server.accept());
				
				int response = (int)newNode.getInputStream().readObject();
				
				synchronized(clients) {
					clients.add(newNode);
				}
				new Listen(newNode).start();
				server.close();
				
				
			}
			SporadicGossip gossipTemporal = new SporadicGossip(initialIp, Integer.parseInt(initialPort), true);//Faz gossip esporadico para avisar novos cliente que stream ele pode transmitir ou retransmitir
			gossipTemporal.start();
		}else {
			SporadicGossip gossipTemporal = new SporadicGossip("", -1, false);//Faz gossip esporadico para avisar novos cliente que stream ele pode transmitir ou retransmitir
			gossipTemporal.start();
		}
		SimpleServer server = new SimpleServer(12345);
		server.start();


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
						Thread.sleep(DELAY_TIME);
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

		run = false;
		sc.close();
	}


	private static void verStream(int escolhaVis) throws InterruptedException {
		do{
			if(!buffer.isEmpty()) {
				System.out.println("Recebi " + buffer.remove());
				//buffer.remove();
				//System.out.println("Viewers: " + viewers.size());
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
						//e.printStackTrace();
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
					streamer = new Node(new Socket(streamerEscolhido, 12345), true);
					clients.add(streamer);
					new Listen(streamer).start();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
			//System.out.println("Stream: " + streamer);
			try {
				ObjectOutputStream out = streamer.getOutputStream();
				for(int i = 0; i < 3; i++) {
					out.writeObject("Visualizar,");
				}
				System.out.println("pedi a este: " + streamer.getSocket());
				streamer.setWatching(true);
				streamer.updateTimer();
			} catch (IOException e) {
				//e.printStackTrace();
			}

			//avisar vizinhos que tb tranmitirei esta stream
			informaVizinhos("Gossip," + idCanalVis + "," + localIp + "," + TTL);

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
				node.close();
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

					clients.add(newNode);
					new Listen(newNode).start();
					System.out.println("entrou node adicionado no randomWalk = " + newNode.getSocket());

				}
			}else {
				System.out.println("Errors");
				newNode.close();

			}
			server.close();
		} catch (NumberFormatException | IOException e) {
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
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
				//e.printStackTrace();
				//System.out.println("Erro na criacao da server socket");
			}
		}

		@Override
		public void run() {

			Node nodeAceite = null;
			Node nodeRemoved = null;
			int idStreamCrashed = 0;

			while(run) {
				try {
					//System.out.println("antes de ligar: " + this.socket);
					nodeAceite = new Node(this.socket.accept());
					localIp = nodeAceite.getSocket().getLocalAddress().toString().substring(1);

					//System.out.println("ligou");

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



										//System.out.println("adicionar");
										Node newNode = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
										new Listen(newNode).start();
										clients.add(newNode);
										int port = (Integer.parseInt(address[1])+2);
										System.out.println("Accept vizinho no simple server " + newNode.getSocket());
										//System.out.println(newVizinho.getSocket().getRemoteSocketAddress().toString());


										//System.out.println("tenho fome: " + newVizinho.getRemoteSocketAddress().toString().substring(1));

										newNode.accepted();

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
								if(info.length == 3) {
									out.writeObject("RandomWalk," + ttl + "," + info[2]);
								}else {
									out.writeObject("RandomWalk," + ttl + "," + info[2] + "," + info[3]);
								}
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
								if(info.length == 4) {
									System.out.println("adicionar");
									Node newNode = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
									new Listen(newNode).start();
									clients.add(newNode);
									newNode.accepted();
								}else {
									newVizinho = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
									out = newVizinho.getOutputStream();
									//out.flush();
	
									out.writeObject(-1);
	
									newVizinho.close();
								}
							}

						}
						break;

					case "Visualizar":
						synchronized (viewers) {
							if(!viewers.contains(nodeAceite)) {
								viewers.add(nodeAceite);
							}
						}
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
					//e.printStackTrace();
				} catch (SocketException e) {
					//System.out.println("entrou aqui simple server");
					nodeRemoved = nodeAceite;
					String[] addressToRemove = nodeRemoved.getSocket().getRemoteSocketAddress().toString().split(":");
					removeStreamerTable(idStreamCrashed, addressToRemove[0]);
					visualizarStream(idStreamCrashed);//em teoria vai buscar outro streamer
					break;
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
			try {
				this.socket.close();
			} catch (IOException e) {
				//e.printStackTrace();
			}
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

		private Node node;
		private ObjectInputStream in;
		private int idStreamRemoved;

		public Listen(Node n) throws IOException {
			this.node = n;
			this.in = node.getInputStream();
			idStreamRemoved = -1;
		}



		@Override
		public void run() {

			boolean crash = true;
			
			while(run && crash) {

				if(node.isWatching()) {
					if(LocalTime.now().getSecond() - node.getStartTime().getSecond() > TIMEOUT) {
						try {
							node.getOutputStream().writeObject("Bad,");
						} catch (IOException e) {
							//e.printStackTrace();
						}
						node.updateTimer();
					}
				}

				try {

					String recebido = (String) in.readObject();
					System.out.println("we got: " + recebido);
					String[] info = new String[1];

					if(recebido.contains(",")) {
						info = recebido.split(",");
					}else {
						int val = (int)recebido.charAt(0);
						info[0] = val+"";
					}

					switch (info[0]) {
					case "RandomWalk":
						//System.out.println("random time");

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
										Node node = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
										clients.add(node);
										System.out.println("accept no listen " + node.getSocket());
										new Listen(node);
										node.accepted();
									}else {
										result = 0;
									}
								}
							}
						}
						if(result >= 0 || clients.size() >= MAX_CLIENT_SIZE) {

							int ttl = Integer.parseInt(info[1]) - 1;
							//System.out.println("TTL: "+ ttl);
							if(ttl > 0 && clients.size() > 1) {
								//System.out.println("TTL GOOD");
								Random r = new Random();
								Node reencaminhar = null;
								do {
									reencaminhar = clients.get(r.nextInt(clients.size()));
								}while(reencaminhar.getSocket().getInetAddress().getHostAddress().equals(address[0]));
								System.out.println("porta de resposta port=" + info[2]);
								if(info.length == 3) {
									reencaminhar.randomWalk("RandomWalk," + ttl + "," + info[2]);
								}else {
									reencaminhar.randomWalk("RandomWalk," + ttl + "," + info[2] + "," + info[3]);
								}
								//System.out.println("mandei");
							}else {
								//System.out.println("TTL BAD");
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
								Node newVizinho;
								ObjectOutputStream out;
								if(info.length == 4) {
									System.out.println("adicionar: " + address[0] + ":" + address[1]);
									Node newNode = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
									new Listen(newNode).start();
									clients.add(newNode);
									newNode.accepted();
									break;
								}else {
									if(localIp.equals(address[0])) {
										break;
									}
									newVizinho = new Node(new Socket(address[0], Integer.parseInt(address[1])+2));
									out = newVizinho.getOutputStream();
									//out.flush();
	
									out.writeObject(-1);
	
									newVizinho.close();
								}
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
						System.out.println("Este adicionou me :" + node.getSocket());
						synchronized (viewers) {
							if(!viewers.contains(node)) {
								viewers.add(node);
							}
						}
						System.out.println("viewers: " + viewers.size());
						node.setWatching(true);
						node.updateTimer();
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
						synchronized (viewers) {
							for(Node n : viewers) {
								n.setWatching(false);
							}
						}

						break;
					case "Bad":
						status--;
						if(status == -1) {
							run = false;
						}
						break;
					default:
						//Caso que recebe dados de uma transmissao
						int i = info[1].charAt(0);
						buffer.add(i);
						idStreamRemoved = i;
						System.out.println("recebido " + i);
						for(Node n : viewers) {
							ObjectOutputStream out = n.getOutputStream();
							out.writeObject(recebido);
						}
						node.updateTimer();
						break;
					}

					//in.close();
				} catch (ClassNotFoundException e) {
					//e.printStackTrace();
				} catch (SocketException e) { //socket exception
					//e.printStackTrace();
					System.out.println("entrou aqui listen" + node.getSocket());
					clients.remove(node);
					String[] addressToRemove = node.getSocket().getRemoteSocketAddress().toString().split(":");

					viewers.remove(node);
					if(removeStreamerTable(idStreamRemoved, addressToRemove[0])) {
						visualizarStream(idStreamRemoved); //em teoria vai buscar outro streamer
					}

					break;
				} catch (IOException e) {
					//e.printStackTrace();
					crash = false;
					
				}

			}
			try {
				this.node.close();
			} catch (IOException e) {
				//e.printStackTrace();
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

			//arrToTransmit[0]++;
			Node nodeRemoved = null;
			//int counter = 0;

			System.out.println("Viewers: " + viewers.size());

			for(char c : arrToTransmit) {
				//traSystem.out.println("entre os for's " + viewers.size());
				for(Node viewer : viewers) {
					ObjectOutputStream out = null;
					try {
						out = viewer.getOutputStream();
						//out.flush();
						//int val = (int)c;
						//System.out.println("enviado: " + val);
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
					//e.printStackTrace();
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

		private String ip;
		private int port;
		private boolean aval;
		
		public SporadicGossip(String ip, int port, boolean avaliador){
			this.ip = ip;
			this.port = port;
			this.aval = avaliador;
		}

		@Override
		public void run() {

			try {

				Random r = new Random();

				while(run) {

					Thread.sleep(TIME_TO_GOSSIP); //faz gossip a cada 15s

					if(clients.size() > 0 && clients.size() < MAX_CLIENT_SIZE) {
						for (int i = 0; i < MAX_CLIENT_SIZE-clients.size(); i++) {
							Node n = clients.get(r.nextInt(clients.size()));
							String[] info = n.getSocket().getRemoteSocketAddress().toString().substring(1).split(":");
							try {
								randomWalk(info[0], info[1]);
							} catch (NumberFormatException e) {
								//e.printStackTrace();
							}
						}
					}
					
					if(clients.size() == 0 && aval) {
						Socket temp = new Socket(this.ip,this.port);
						Node entradaNaRede = new Node(temp);
						entradaNaRede.randomWalk("RandomWalk," + TTL + "," + temp.getLocalSocketAddress().toString().substring(1) + ",urgent");
						ServerSocket server = new ServerSocket(temp.getLocalPort()+2);
						entradaNaRede.close();
						Node newNode = new Node(server.accept());
						
						int response = (int)newNode.getInputStream().readObject();
						
						synchronized(clients) {
							clients.add(newNode);
						}
						new Listen(newNode).start();
						server.close();
					}
					for(int idStream : streams) {
						informaVizinhos("Gossip," + idStream + "," + localIp + "," + TTL);
					}
				}
			} catch (InterruptedException | IOException e) {
				//e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

	}
}