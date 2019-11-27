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
		server.startServer(args[1]);
		

	}

	private static void testFillMaps() {
		
		List<String> test = new ArrayList<String>();
		test.add("300");


		transmiters.put(12345, test);
		counter.put(12345, 0);
		test.add("666");
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
				String stream = null;
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				
				//TODO menu
				
				outStream.writeObject(getStreams());
				
				stream = (String)inStream.readObject();
				
				while(stream == null) {}
				
				outStream.writeObject(connectTo(stream));				
				
				socket.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		private String connectTo(String stream) {
			
			int userId = Integer.parseInt(stream);
			
			List<String> ips = transmiters.get(userId);
			
			String result = ips.get(counter.get(userId));
			transmiters.get(userId).add(socket.getRemoteSocketAddress().toString());
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
		
	}

}
