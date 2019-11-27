package client;

import java.util.List;
import java.util.Scanner;

import javax.print.DocFlavor.INPUT_STREAM;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client {

	private List<Integer> clientes = new ArrayList<Integer>();

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, ClassNotFoundException, IOException {

		Scanner sc = new Scanner(System.in);

		while(true) {
			String input = sc.nextLine();
			
			switch (Integer.parseInt(input)) {
			case 1:
				conectionServer(args[0], Integer.parseInt(args[1]), sc);
				//agora ligar ao stream
				break;
			
			case 2:
				//I want to stream
				break;
				
			default:
				//caso input invalido fechar aplicacao?
				input = "-1";
				break;
			}
			

			if(input.equals("-1")) {
				sc.close();
				break;
			}
		}

	}
	public static String conectionServer(String ip, int port, Scanner sc) throws UnknownHostException, IOException, ClassNotFoundException{
		Socket cliente = new Socket(ip, port);

		ObjectOutputStream saida = new ObjectOutputStream(cliente.getOutputStream());
		ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());

		String opcoes = (String) entrada.readObject();
		System.out.println(opcoes);

		System.out.println("Escreva o canal que pretende visualizar das opções acima listadas");

		String opcao = sc.nextLine();

		saida.writeObject(opcao);
		String result = (String) entrada.readObject();

		System.out.println(result);		

		cliente.close();
		saida.close();
		entrada.close();
		
		return result;
	}
}
