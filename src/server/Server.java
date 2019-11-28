package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Server {

	private static Map<Integer, List<String>> transmiters = new HashMap<Integer, List<String>>();
	private static Map<Integer, Integer> counter = new HashMap<Integer, Integer>();


	public static void main(String[] args) throws IOException {


		testFillMaps();
		Server server = new Server();
		server.startServer(args[0]);


	}

	private static void testFillMaps() {

		List<String> test = new ArrayList<String>();
		test.add("300:600");


		transmiters.put(12345, test);
		counter.put(12345, 0);
		test.add("666:300");
		transmiters.put(54321, test);
		counter.put(54321, 0);

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

				}
				String stream = null;
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				int action = (int) inStream.readObject();

				switch (action) {
				case 1:
					outStream.writeObject(getStreams());

					stream = (String)inStream.readObject();
					while(stream == null) {} //e preciso?

					outStream.writeObject(connectTo(stream));
					break;

				case 2:
					addHost(inStream, outStream);
					break;

				case 3:
					String info = (String)inStream.readObject();
					removeClient(info);

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

			List<String> ips = transmiters.get(userId);

			String result = ips.get(counter.get(userId));
			transmiters.get(userId).add(socket.getRemoteSocketAddress().toString()+":"+(socket.getPort()+1));
			counter.replace(userId, counter.get(userId)+1);

			return result;
		}

		private String getStreams() {

			StringBuilder sb = new StringBuilder();

			for(Entry<Integer, List<String>> entry : transmiters.entrySet()) {
				sb.append(entry.getKey() + "\n");
			}

			return sb.toString();
		}

		//assumindo k user é o ip:port de alguem que esta a assistir
		private void removeClient(String info) {
			String[] data = info.split("S//+");

			List<String> users = transmiters.get(Integer.parseInt(data[0]));
			users.remove(data[1]);
			counter.replace(Integer.parseInt(data[0]), counter.get(Integer.parseInt(data[0]))-1);

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

			while(unique) {
				out.writeObject(-1);
				userId = (int)in.readObject();
				unique = uniqueUser(userId);
			}

			List<String> ips = new ArrayList<String>();
			ips.add(socket.getRemoteSocketAddress().toString());
			transmiters.put(userId, ips);
			counter.put(userId, 0);
			out.writeObject(1);
		}

		private boolean uniqueUser(int userId) {
			return !transmiters.containsKey(userId);
		}

	}

}
