package client;

import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client {
	
	private List<Integer> clientes = new ArrayList<Integer>();
	
	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException {
		
		conectionServer(args[0], Integer.parseInt(args[1]));
	
	}
	public static void conectionServer(String ip, int port) throws UnknownHostException, IOException, ClassNotFoundException{
		Socket cliente = new Socket(ip, port);

		ObjectOutputStream saida = new ObjectOutputStream(cliente.getOutputStream());
		ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
		
		String opcoes = (String) entrada.readObject();
		System.out.println(opcoes);
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Escreva o canal que pretende visualizar das opções acima listadas");
		
		String opcao = sc.nextLine();
		
		saida.writeObject(opcao);
		String idk = (String) entrada.readObject();
		
		System.out.println(idk);		
		
		cliente.close();
		saida.close();
		sc.close();
		//byte [] b = InetAddress.getByName("localhost"). getAddress();
		entrada.close();
	}
}
