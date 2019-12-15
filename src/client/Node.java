package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Node {
	
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean watching;

	public Node(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new ObjectOutputStream(this.socket.getOutputStream());
		this.in = new ObjectInputStream(this.socket.getInputStream());
		this.watching = false;
	}
	
	public Node(Socket socket, boolean watch){
		this.socket = socket;
		this.watching = watch;
	}

	public void close() throws IOException {
		out.close();
		in.close();
		socket.close();
	}
	

	public boolean isWatching() {
		return watching;
	}

	public void randomWalk(String msg) throws IOException {
		out.writeObject(msg);
	}
	
	public void accepted() throws IOException {
		out.writeObject(1);
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

	public void setWatching(boolean b) {
		this.watching = b;
	}

}