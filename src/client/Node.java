package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;

public class Node {
	
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean watching;
	private LocalTime startTime;

	public Node(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new ObjectOutputStream(this.socket.getOutputStream());
		this.in = new ObjectInputStream(this.socket.getInputStream());
		this.watching = false;
	}
	
	public Node(Socket socket, boolean watch){
		this.socket = socket;
		this.watching = watch;
		this.startTime = LocalTime.now();
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

	public ObjectOutputStream getOutputStream() throws IOException {
		if(out == null) {
			out = new ObjectOutputStream(this.socket.getOutputStream());
		}
		return out;
	}

	public ObjectInputStream getInputStream() throws IOException {
		if(in == null) {
			in = new ObjectInputStream(this.socket.getInputStream());
		}
		return in;
	}
	
	public LocalTime getStartTime() {
		return startTime;
	}

	public void setWatching(boolean b) {
		this.watching = b;
	}
	
	public void updateTimer() {
		this.startTime = LocalTime.now();
	}

	public void reopenIn() throws IOException {
		this.in.close();
		this.in = new ObjectInputStream(this.socket.getInputStream());
		
	}

}