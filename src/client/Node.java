package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Node {
	
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public Node(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new ObjectInputStream(this.socket.getInputStream());
		this.out = new ObjectOutputStream(this.socket.getOutputStream());
	}
	
	public Node(Socket socket, ObjectOutputStream out, ObjectInputStream in){
		this.socket = socket;
		this.in = in;
		this.out = out;
	}

	public void close() throws IOException {
		out.close();
		in.close();
		socket.close();
		
	}
	
	public Socket getSocket() {
		return socket;
	}

	public ObjectOutputStream getOutputStream() {
		return out;
	}

	public ObjectInputStream getInputStream() {
		return in;
	}

}
