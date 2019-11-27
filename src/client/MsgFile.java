package client;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;

/**
 * 
 * @author Francisco Rodrigues, n50297; Pedro Carrega, n49480; Vasco Ferreira, n49470
 *
 */
public class MsgFile {
	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		
		System.setProperty("javax.net.ssl.trustStore", "myClient.keyStore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456789");
		
		StringBuilder argumentos = new StringBuilder(args[0]);
		String port = null;
		String adress = null;

		for(int i = 0; i < argumentos.length(); i++) {
			if(argumentos.charAt(i) == ':') {
				adress = argumentos.substring(0, i);
				port = argumentos.substring(adress.length() + 1);
				break;
			}
		}
		try {
			Socket sf = SSLSocketFactory.getDefault().createSocket(adress, Integer.parseInt(port));
			String user = args[1];
			String password = null;
			Scanner sc = new Scanner(System.in);

			if(args.length == 3) {
				password = args[2];
			} else {
				System.out.print("Password:");
				password = sc.nextLine();
			}

			ObjectInputStream in = new ObjectInputStream(sf.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(sf.getOutputStream());

			out.writeObject(user);
			out.writeObject(password);

			int successLog = (Integer)in.readObject();
			if(successLog == -2) {
				System.out.println("Mac invalido, ficheiro foi alterado sem permissao. O programa vai fechar");
				sf.close();
				sc.close();
				return;
			}else if(successLog == -1) {
				System.out.println("Passe incorreta, o programa vai fechar");
				sf.close();
				sc.close();
				return;
			} else if(successLog == 0) {
				System.out.println("Este utilizador nao existe. O programa vai fechar");
				sf.close();
				sc.close();
				return;
			} else {
				System.out.println("Sessao iniciada com sucesso");
			}

			String comando = "";
			boolean endFlag = false;//verifica se o user quer fechar o programa ou nao

			while(!endFlag) {
				menu(); //escreve o menu na consola

				System.out.print("Comando: ");
				comando = sc.nextLine();
				trataComando(comando, out, in);//trata o comando se tiver errado, se tiver certo eniva mensagem para o servidor

				if(comando.equals("quit")){
					endFlag = true;
				}
			}
			
			sc.close();
			sf.close();
			in.close();
			out.close();
		} catch (ConnectException e){
			System.out.println("O servidor esta desligado");
			System.out.println("O programa vai fechar, tentar novamente mais tarde");
		}
	}
	/**
	 * Este metodo trata o comando inserido pelo cliente na consola
	 * @param comando comando que o cliente inseriu
	 * @param out stream para enviar dados para o servidor
	 * @param in stream para receber dados do servidor
	 * @throws IOException 
	 * @throws ClassNotFoundException
	 * @requires comando, out, in != null
	 */
	private static void trataComando(String comando, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {

		String[] splited = comando.split("\\s+");

		switch(splited[0]) {
		case("store"):
			if(splited.length < 2) {
				System.out.println("Comando mal escrito, tente novamente");
			} else {
				store(comando, splited, in, out);
			}
		break;
		case("list"):
			if(splited.length > 1){
				System.out.println("Comando mal escrito, tente novamente");
			} else {
				list(comando, in, out);
			}
		break;
		case("remove"):
			if(splited.length < 2) {
				System.out.println("Comando mal escrito, tente novamente");
			}else{
				remove(comando, splited, in, out);
			}
		break;
		case("users"):
			if(splited.length != 1) {
				System.out.println("comando errado");
			} else {
				users(comando, in, out);
			}
		break;
		case("trusted"):
			if(splited.length < 2) {
				System.out.println("Comando errado");
			} else {
				trusted(comando, splited, in, out);
			}
		break;
		case("untrusted"):
			if(splited.length < 2) {
				System.out.println("Comando errado");
			} else {
				untrusted(comando, splited, in, out);
			}
		break;
		case("msg"):
			if(splited.length < 3) {
				System.out.println("Comando errado");
			}else {
				msg(comando, splited, in, out);
			}
		break;
		case("collect"):
			if(splited.length > 1) {
				System.out.println("Comando errado");
			} else {
				collect(comando, in, out);
			}
		break;
		case ("download"):
			if(splited.length != 3){
				System.out.println("Comando errado");
			} else {
				download(comando, in, out);
			}
		break;
		case ("quit"):
			quit(comando, out);
		break;
		default:
			System.out.println("Este comando nao existe, tente novamente");
			break;
		}
	}
	/**
	 * Este metodo envia ficheiros para o servidor para que este os guarde na conta do cliente
	 * @param comando comando inserido pelo cliente
	 * @param splited todas as palavras que constituem o comando
	 * @param in stream para receber dados do servidor
	 * @param out stream para enviar dados para o servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, splited, in, out != null
	 */
	private static void store(String comando, String[] splited, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);

		for(int i = 1; i < splited.length; i++ ) {

			File f = new File(splited[i]);

			boolean fileServer = (boolean)in.readObject();//recebe true se o ficheiro nao existe no servidor, false caso contrario

			if(!fileServer) {
				System.out.println("O ficheiro " + splited[i] + " ja existe no servidor e nao pode ser substituido");
			} else if (!f.exists()) {
				out.writeObject(new Boolean (false));//envia false se o ficheiro n existe no cliente
				System.out.println("O ficheiro " + splited[i] + " nao existe. Assim nao foi enviado");
			} else if (fileServer && f.exists()) {

				out.writeObject(new Boolean (true));
				File ficheiro = new File(splited[i]);
				FileInputStream fileStream = new FileInputStream(ficheiro);
				byte[] fileByte = new byte[1024];
				int aux;

				while((aux = fileStream.read(fileByte)) != -1){
					out.writeObject(new Boolean (true)); //envia true enqt o ciclo esta a correr
					out.writeObject(aux);
					out.write(fileByte, 0, aux);
					out.flush();
				}

				out.writeObject(new Boolean (false));//envia false qd sai do ciclo

				System.out.println("O ficheiro " + splited[i] + " foi adicionado com sucesso");

				fileStream.close();
			}
		}

	}	
	/**
	 * Este metodo escreve o nome de todos os ficheiros q este cliente tem guardado no servidor
	 * @param comando comando inserido pelo cliente
	 * @param in stream para receber dados do servidor
	 * @param out stream para enviar dados para o servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, in, out != null 
	 */
	private static void list(String comando, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);
		List<String> result = new ArrayList<String>();
		result = (List<String>)in.readObject();

		if(result.size() == 0) {
			System.out.println("Nao existem ficheiros no servidor");
		} else {
			System.out.println("------------ Ficheiros -----------------");
			for(int i = 0; i < result.size(); i++) {
				System.out.println(result.get(i));
			}
			System.out.println("----------------------------------------");
		}
	}
	/**
	 * Este comando remove do servidor ficheiros q o cliente guardou 
	 * no servidor, dado os seu nomes no comando
	 * @param comando comando inserido na consola que contem todos os nomes dos ficheiros q 
	 * 				  vao ser removidos
	 * @param splited todas as palavras que constituem o comando
	 * @param in stream para enviar dados ao servidor
	 * @param out stream para receber dados do servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, splited, in, out != null
	 */
	private static void remove(String comando, String[] splited, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);

		for(int i = 1; i < splited.length; i++){
			if((boolean)in.readObject()){
				System.out.println(splited[i] + "foi removido com sucesso.");
			}else{
				System.out.println(splited[i] + "nao foi removido pq nao existe no servidor");
			}
		}
	}
	/**
	 * Este metodo indica todos os utilizadores que tem conta no servidor
	 * @param comando comando inserido pelo utilizador 
	 * @param in stream para enviar dados para o servidor
	 * @param out stream para receber dados do servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, in, out != null
	 */
	private static void users(String comando, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);
		List<String> result = new ArrayList<String>();
		result = (List<String>)in.readObject();
		if(result.size() == 0) {
			System.out.println("Nao existem users no servidor");
		} else {
			System.out.println("------------ Users -----------------");
			for(int i = 0; i < result.size(); i++) {
				System.out.println(result.get(i));
			}
			System.out.println("------------------------------------");
		}
	}
	/**
	 * Este metodo adiciona um ou mais users dado pelo comando aos amigos do cliente
	 * @param comando comando inserido que contem os nomes 
	 *                dos users que querem ser adicionados a lista de amigos
	 * @param splited todas as palavras que constituem o comando
	 * @param in stream para enviar dados ao servidor
	 * @param out stream para receber dados do servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static void trusted(String comando, String[] splited, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);
		for (int i = 1; i < splited.length; i++) {
			int sucesso = (int)in.readObject();
			if(sucesso == -1) {
				System.out.println("O utilizador" + splited[i] + "ja esta na sua lista de amigos");
			} else if (sucesso == 1){
				System.out.println("O utilizador " + splited[i] + " foi adicionado com sucesso");
			} else {
				System.out.println("O utilizador " + splited[i] + " nao existe no servidor");
			}
		}
	}
	/**
	 * Este comando retira um ou mais users da lista de amigos do cliente, 
	 * dados os seus nomes no comando
	 * @param comando comando inserido na consola que contem os nomes que serao retirados 
	 * 			      da lista
	 * @param splited todas as palavras que constituem o comando
	 * @param in stream para receber dados do servidor
	 * @param out stream para enviar dados para o servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, splited, in, out != null
	 */
	private static void untrusted(String comando, String[] splited, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);
		for (int i = 1; i < splited.length; i++) {
			int sucesso = (int)in.readObject();
			if(sucesso == -1) {
				System.out.println("O utilizador" + splited[i] + "nao existe nos Trusted Users");
			} else if (sucesso == 1){
				System.out.println("O utilizador " + splited[i] + " foi removido com sucesso");
			} else {
				System.out.println("Erro!");
			}
		}
	}
	/**
	 * Este metodo envia uma mensagem a um utilizador dado o seu nome
	 * @param comando comando que contem a mensagem e o user a q vai ser enviada
	 * @param splited todas as palavras que constituem o comando
	 * @param in stream para enviar dados ao servidor
	 * @param out stream para receber dados do servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, splited, in, out != null
	 */
	private static void msg(String comando, String[] splited, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);
		int sucesso = (int)in.readObject();
		if(sucesso == -1) {
			System.out.println("O utilizador" + splited[1] + "nao existe");
		} else if (sucesso == 1){
			System.out.println("A mensagem foi enviada para " + splited[1] + " com sucesso");
		} else if(sucesso == -2){
			System.out.println("O utilizador " + splited[1] + "nao e seu amigo");
		}else {
			System.out.println("Não é trusted user do destinatario");
		}		
	}
	/**
	 * Este metodo escreve todas as mensagens que foram enviadas ao cliente na consola
	 * @param comando comando inserido na consola
	 * @param in stream para enviar dados ao servidor
	 * @param out stream para receber dadods do servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @requires comando, in, out != null
	 */
	private static void collect(String comando, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
		out.writeObject(comando);

		String resultado = (String)in.readObject();

		System.out.println(resultado);

	}
	/**
	 * 
	 * @param comando
	 * @param in
	 * @param out
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static void download(String comando, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {

		out.writeObject(comando);
		String[] splited = comando.split("\\s+");

		int sucesso = (int)in.readObject();

		if(sucesso == 1){

			FileOutputStream newFile = new FileOutputStream(splited[2]);
			byte[] fileByte = new byte[1024];
			int tamanho;
			int quantos;

			while((boolean)in.readObject()){//qd recebe false sai do ciclo
				tamanho = (int)in.readObject();
				quantos = in.read(fileByte, 0, tamanho);
				newFile.write(fileByte);	
			}

			newFile.close();
			System.out.println("O ficheiro " + splited[2] + " foi extraido com sucesso");
		} else if(sucesso == 0) {
			System.out.println("O ficheiro " + splited [2] + " nao existe no servidor");
		} else if(sucesso == -1) {
			System.out.println("O utilizador " + splited[1] + " nao o tem adicionado como amigo");
		} else {
			System.out.println("O utilizador " + splited[1] + " nao existe no servidor" );
		}
	}

	private static void quit(String comando, ObjectOutputStream out) throws IOException {
		out.writeObject(comando);
		System.out.println("O programa vai fechar");
	}

	private static void menu() {
		System.out.println("-----------------------------------------------------------------------------");
		System.out.println("store <files> - envia um ou mais ficheiros para o servidor");
		System.out.println("list - lista os ficheiros que tem no servidor");
		System.out.println("remove <files> - remove um ou mais ficheiros guardados no servidor");
		System.out.println("users - lista todos os utilizadores que estao registados no servidor");
		System.out.println("trusted <trustedUserIDs> - adiciona os users trustedUserIDs como seus amigos");
		System.out.println("untrusted <untrustedUserIDs> - remove os utilizadores untrustedUserIDs como amigos");
		System.out.println("download <userID> <file> - obtem do servidor o ficheiro file guardado na conta do user userID");
		System.out.println("msg <userID> <msg> - envia uma mensagem para o userID");
		System.out.println("collect - recebe do servidor todas as mensagens existentes na caixa de mensagens e limpa a caixa");
		System.out.println("quit - sai do programa");
		System.out.println("-----------------------------------------------------------------------------");
	}
}
