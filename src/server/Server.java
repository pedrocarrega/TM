package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

public class Server {

	private final static int DEFAULT_CAPACITY = 12;

	private static Map<Integer, ArrayBlockingQueue<String>> transmiters = new HashMap<Integer, ArrayBlockingQueue<String>>();

	public static void main(String[] args) throws IOException {


		testFillMaps();
		Server server = new Server();
		server.startServer(args[0]);


	}

	private static void testFillMaps() {

		ArrayBlockingQueue<String> test = new ArrayBlockingQueue<>(DEFAULT_CAPACITY, true);
		test.add("666:666");


		transmiters.put(12345, test);

	}

	private void startServer(String port) throws IOException {

		@SuppressWarnings("resource")
		ServerSocket socket = new ServerSocket(Integer.parseInt(port));

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
				if(socket.getPort() == 8000) {
					//ligacao entre servidores
				}
				String stream = null;
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				int action = (int) inStream.readObject();

				switch (action) {
				case 1:
					outStream.writeObject(getStreams());

					stream = (String)inStream.readObject();

					outStream.writeObject(connectTo(stream));
					break;

				case 2:
					addHost(inStream, outStream);
					break;

				case 3:
					String info = (String)inStream.readObject();
					removeClient(info);
					break;

				default:
					break;
				}

				socket.close();
				outStream.close();
				inStream.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		private String connectTo(String stream) {


			int userId = Integer.parseInt(stream);

			ArrayBlockingQueue<String> ips = transmiters.get(userId);
			String result = ips.remove();
			ips.add(socket.getRemoteSocketAddress().toString().substring(1));
			try {
				ips.add(result);
			}catch(IllegalStateException e) {
				ArrayBlockingQueue<String> temp = new ArrayBlockingQueue<>(ips.size()*2, true);
				ips.drainTo(temp);
				temp.add(result);
				ips=temp;
			}

			return result;
		}

		private String getStreams() {

			StringBuilder sb = new StringBuilder();

			for(Entry<Integer, ArrayBlockingQueue<String>> entry : transmiters.entrySet()) {
				sb.append(entry.getKey() + "\n");
			}

			return sb.toString();
		}

		//assumindo k user == "ip:port" de alguem que esta a assistir
		private void removeClient(String info) {
			String[] data = info.split("S//+");

			ArrayBlockingQueue<String> users = transmiters.get(Integer.parseInt(data[0]));
			users.remove(data[1]);

		}

		/*
		//TODO join in removeClient
		private void removeHost(int user) {
			transmiters.remove(user);
			counter.remove(user);
		}
		 */

		private void addHost(ObjectInputStream in, ObjectOutputStream out) throws ClassNotFoundException, IOException {
			int userId = (int)in.readObject();
			boolean unique = uniqueUser(userId);

			while(!unique) {
				out.writeObject(-1);
				userId = (int)in.readObject();
				unique = uniqueUser(userId);
			}

			ArrayBlockingQueue<String> ips = new ArrayBlockingQueue<String>(DEFAULT_CAPACITY, true);
			ips.add(socket.getRemoteSocketAddress().toString().substring(1));
			transmiters.put(userId, ips);
			out.writeObject(socket.getPort()); //devolve o port do client??
		}

		private boolean uniqueUser(int userId) {
			return !transmiters.containsKey(userId);
		}

	}

}
