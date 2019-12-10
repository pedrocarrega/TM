package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Node {
	
	public Socket socket;
	public ObjectOutputStream out;
	public ObjectInputStream in;
	
	public Node(Socket socket) throws IOException {
		this.socket = socket;
		System.out.println("socket criado");
		this.in = new ObjectInputStream(this.socket.getInputStream());
		System.out.println("input");
		this.out = new ObjectOutputStream(this.socket.getOutputStream());
		System.out.println("output");
	}
	
	public Node(Socket socket, ObjectOutputStream out, ObjectInputStream in){
		this.socket = socket;
		this.in = in;
		this.out = out;
	}
	
	public ObjectOutputStream getOut() {
		return out;
	}
	
	public ObjectInputStream getIn() {
		return in;
	}
	
	public Socket getSocket() {
		return socket;
	}

}
