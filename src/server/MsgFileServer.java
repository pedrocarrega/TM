package server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLServerSocketFactory;
import javax.xml.bind.DatatypeConverter;

/**
 * 
 * @author Francisco Rodrigues, n50297; Pedro Carrega, n49480; Vasco Ferreira, n49470
 *
 */

public class MsgFileServer {

	/**
	 * @param args - server port
	 * @throws NumberFormatException
	 * @throws IOException
	 */

	private String pwMan;
	private String pwKs;
	private KeyStore ks;

	public static void main(String[] args) throws NumberFormatException, IOException {

		System.out.println("servidor: main");
		MsgFileServer server = new MsgFileServer();
		server.startServer(args[0], args[1], args[2]);
	}

	/**
	 * Starts the server and opens it to recieve connections
	 * 
	 * @param args - server port
	 * @throws NumberFormatException
	 * @throws IOException
	 */

	private void startServer(String porto, String pwMan, String pwKs) throws NumberFormatException, IOException {


		System.setProperty("javax.net.ssl.keyStore", "myServer.keyStore");
		System.setProperty("javax.net.ssl.keyStorePassword", pwKs);
		SSLServerSocketFactory sslServerSocketFactory = 
				(SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
		ServerSocket sslServerSocket = 
				sslServerSocketFactory.createServerSocket(Integer.parseInt(porto));
		try {
			this.pwMan = pwMan;
			this.pwKs = pwKs;
			ks  = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("myServer.keyStore"), pwKs.toCharArray());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		}

		while(true) {
			new SSLSimpleServer(sslServerSocket.accept()).start();
		}
	}

	class SSLSimpleServer extends Thread {

		private Socket socket = null;

		SSLSimpleServer(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

		/**
		 * Processes client
		 */

		public void run(){
			try {

				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				System.out.println("Client Conected");

				File f = new File("users.txt");

				if(!f.exists()) {
					f.createNewFile();
				}

				String user = (String)inStream.readObject();
				String password = (String)inStream.readObject();


				autenticacao(f, user, password, outStream);

				boolean executa = true;

				while(executa) {
					String comando = (String)inStream.readObject();//leitura do comando do cliente
					System.out.println("Comando recebido: " + comando);
					trataComando(comando, inStream, outStream, user);

					if(comando.equals("quit")){
						executa = false;
					}
				}

				socket.close();

			} catch (IOException e) {
				System.out.println("Cliente disconectou de forma errada");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (SignatureException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Checks if user exists, creates it in case it doesnt exist
		 * 
		 * @param f - file containing users information
		 * @param user - userID
		 * @param password
		 * @param out - to send objects to client
		 * @param pwMan 
		 * @throws FileNotFoundException
		 * @throws IOException
		 * @throws NoSuchAlgorithmException 
		 */

		private void autenticacao(File f, String user, String password, ObjectOutputStream out) throws FileNotFoundException, IOException, NoSuchAlgorithmException {

			if(encryptionAlgorithms.validMAC(pwMan)) {
				BufferedReader br = new BufferedReader(new FileReader(new File("users.txt")));

				while(br.ready()) {
					String[] splited = br.readLine().split(":");
					if(splited[0].equals(user)) {
						String nPW = splited[1] + password;
						MessageDigest md = MessageDigest.getInstance("SHA");
						byte[] hashed = md.digest(nPW.getBytes());
						String pwHashed = DatatypeConverter.printBase64Binary(hashed);
						if(pwHashed.equals(splited[2])) {
							br.close();
							System.out.println("Sessao iniciada");
							out.writeObject(1);//enviar 1 se o cliente existe e a password estiver correta
							return;
						}else {
							br.close();
							System.out.println("Passe incorreta, este cliente vai fechar");
							out.writeObject(-1);//enviar -1 se a password esta incorreta
							this.socket.close();//para fechar o cliente
							return;
						}
					}
				}
				out.writeObject(0);//enviar 0 se o cliente nao existe
				br.close();
			}else {
				System.out.println("MAC invaliado, ficheiro foi alterado sem permissao");
				out.writeObject(-2);
			}
		}

		/**
		 * @param comando
		 * @param inStream
		 * @param outStream
		 * @param user - userID
		 * @throws IOException
		 * @throws ClassNotFoundException
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws IllegalBlockSizeException 
		 * @throws SignatureException 
		 */

		private void trataComando(String comando, ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, SignatureException {

			String[] splited = comando.split("\\s+");

			switch(splited[0]) {
			case "store":
				storeFiles(inStream, outStream, splited, user);
				break;
			case "list":
				list(inStream, outStream, splited, user);
				break;
			case "remove":
				remove(inStream, outStream, splited, user);
				break;
			case "users":
				users(inStream, outStream, splited, user);
				break;
			case "trusted":
				trusted(inStream, outStream, splited, user);
				break;
			case "untrusted":
				untrusted(inStream, outStream, splited, user);
				break;
			case "download":
				download(inStream, outStream, splited, user);
				break;
			case "msg":
				msg(inStream, outStream, splited, user);
				break;
			case "collect":
				collect(inStream, outStream, splited, user);
				break;
			case "quit":
				quit(user);
				break;
			}
		}

		/**
		 * @param inStream
		 * @param out
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws ClassNotFoundException
		 * @throws IOException
		 * @throws NoSuchAlgorithmException 
		 * @throws NoSuchPaddingException 
		 * @throws InvalidKeyException 
		 * @throws IllegalBlockSizeException 
		 */
		//JÃ¡ cifra
		private void storeFiles(ObjectInputStream inStream, ObjectOutputStream out, String[] splited, String user) throws ClassNotFoundException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException {

			SecretKey key = null;
			Cipher c = null;


			for(int i = 1; i < splited.length; i++) {

				File f = new File("users/" + user + "/files/" + splited[i]);

				if(!f.exists()) {

					key = getFileKey("users/" + user + "/files/" + splited[i]);
					c = Cipher.getInstance("AES");
					c.init(Cipher.ENCRYPT_MODE, key);


					out.writeObject(new Boolean(true));//se ficheiro nao existe envia true de sucesso

					boolean fileClientExist = (boolean)inStream.readObject();

					if(fileClientExist) {

						FileOutputStream newFile = new FileOutputStream("users/" + user + "/files/" + splited[i]);
						CipherOutputStream cos = new CipherOutputStream(newFile, c);
						byte[] fileByte = new byte[1024];
						int tamanho;
						//int quantos;

						while((boolean)inStream.readObject()){//qd recebe false sai do ciclo
							tamanho = (int)inStream.readObject();
							inStream.read(fileByte, 0, tamanho);

							cos.write(fileByte, 0, tamanho);	
						}

						cos.close();
						newFile.close();

						System.out.println("O ficheiro " + splited[i] + " foi adicionado com sucesso");
					}
				} else {
					//escrever false para o cliente
					out.writeObject(new Boolean(false));
					System.out.println("O ficheiro " + splited[i] + " ja existe!");
				}
			}
		}

		/**
		 * lists the names of the files stored by the user
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 */

		private void list(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited, String user) throws IOException {

			File folder = new File("users/" + user + "/files");
			File[] listOfFiles = folder.listFiles();
			List<String> result = new ArrayList<String>();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && !listOfFiles[i].getName().contains(".key")) {//tem de se confirmar se ele não devolve o nome dos ficheiros .key
					result.add(listOfFiles[i].getName());
				}
			}
			outStream.writeObject(result);
			System.out.println("Os nomes dos ficheiros foram enviados");
		}

		/**
		 * removes an user file and its key file
		 * 
		 * @param inStream
		 * @param out
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 */

		private void remove(ObjectInputStream inStream, ObjectOutputStream out, String[] splited, String user) throws IOException{
			File apagarFile;
			File apagarKeyFile;

			for(int i = 1; i < splited.length; i++){
				apagarFile = new File("users/" + user + "/files/" + splited[i]);
				apagarKeyFile = new File("users/" + user + "/files/" + splited[i] + ".key");
				out.writeObject(apagarFile.delete() && apagarKeyFile.delete());


			}
		}

		/**
		 * presents all created users
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 */

		private void users(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited, String user) throws IOException {

			File f = new File("users.txt");

			BufferedReader br = new BufferedReader(new FileReader(f.getName()));
			List<String> result = new ArrayList<String>();

			if(encryptionAlgorithms.validMAC(pwMan)) {
				String line = br.readLine();

				while (line != null) {
					String[] userName = line.split(":");
					result.add(userName[0]);
					line = br.readLine();
				}

				br.close();
				outStream.writeObject(result);
			}else {
				System.out.println("MAC invaliado, ficheiro foi alterado sem permissao");
				br.close();
				outStream.writeObject(result);
			}
		}

		/**
		 * adds a trusted userID
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws SignatureException 
		 * @throws IllegalBlockSizeException 
		 */

		private void trusted(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited,
				String user) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, SignatureException, IllegalBlockSizeException {

			File f = null;
			boolean avaliador = true;

			for(int i = 1; i < splited.length; i++) {
				System.out.println(splited[i]);
				boolean teste = verificaSig("users/" + user + "/trustedUsers.txt") && encryptionAlgorithms.validMAC(pwMan);
				if(teste) {
					if(teste = !userExistsServer(splited[i])) {
						System.out.println("O utilizador " + splited[i] + " nao existe no servidor");
						outStream.writeObject(0);//envia 0 se o user a adicionar nao existe no servidor
					} else if(!teste && userExistsTrusted(splited[i], user)){
						outStream.writeObject(-1);//envia -1 se o user a adicionar ja esta nos trusted
						System.out.println("O utilizador " + splited[i] + " ja existe nos trustedIDs");
					}else {
						f = new File("users/" + user + "/trustedUsers.txt");
						FileOutputStream newFile = new FileOutputStream(f, true);
						String print = splited[i] + "\n";

						SecretKey key = getFileKey("users/" + user + "/trustedUsers.txt");					

						Cipher c = Cipher.getInstance("AES");
						c.init(Cipher.ENCRYPT_MODE, key);
						CipherOutputStream cos = new CipherOutputStream(newFile, c);


						cos.write(print.getBytes());
						System.out.println("O utilizador " + splited[i] + " foi adicionado com sucesso");
						cos.close();
						newFile.close();
						outStream.writeObject(1); //envia 1 se e adicionado com sucesso
					}
				} else {
					System.out.println("O ficheiro foi alterado por alguém sem permissões");
					outStream.writeObject(-2);
					avaliador = false;
				}
			}
			if(avaliador) {
				atualizaSig(generateSig(user), user);
			}
		}

		/**
		 * removes a trusted userID
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws SignatureException 
		 * @throws IllegalBlockSizeException 
		 */

		private void untrusted(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited,
				String user) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, SignatureException, IllegalBlockSizeException {

			boolean bol = false;
			boolean integridade;

			for(int i = 1; i < splited.length; i++) {
				if(integridade = (!verificaSig("users/" + user + "/trustedUsers.txt") || !encryptionAlgorithms.validMAC(pwMan))) {
					outStream.writeObject(-2);//envia -1 se o user a adicionar ja esta nos trusted
					System.out.println("Um dos ficheiros foi alterado por alguem sem permissões");
					break;
				}else if(!integridade && (bol = !userExistsServer(splited[i]))) {
					outStream.writeObject(0);//envia -1 se o user a adicionar ja esta nos trusted
					System.out.println("O utilizador" + splited[i] + "nao existe no servidor");
				}else if(!integridade && (bol = !bol && !userExistsTrusted(splited[i], user))) {
					System.out.println("� trusted? " + userExistsTrusted(splited[i], user));

					outStream.writeObject(-1);//envia -1 se o user a adicionar ja esta nos trusted
					System.out.println("O utilizador" + splited[i] + "nao existe nos Trusted Users");

				} else {

					File f = new File("users/" + user + "/trustedUsers.txt");
					File tempFile = new File("users/" + user + "/trustedUsers1.txt");
					Cipher cInput = Cipher.getInstance("AES");
					Cipher cOutput = Cipher.getInstance("AES");
					Key key = getFileKey(f.getPath());

					tempFile.createNewFile();
					cInput.init(Cipher.DECRYPT_MODE, key);
					cOutput.init(Cipher.ENCRYPT_MODE, key);

					FileInputStream fis = new FileInputStream(f);
					FileOutputStream fos = new FileOutputStream(tempFile, true); //se untrusted tiver mal � por causa deste true
					CipherInputStream cis = new CipherInputStream(fis, cInput);
					CipherOutputStream cos = new CipherOutputStream(fos, cOutput);
					StringBuilder sb = new StringBuilder();
					int letra;

					System.out.println("lalala: " + cis.available());
					while((letra = cis.read()) != -1) {
						if(letra != '\n') {
							sb.append((char)letra);
						}else {
							if(!sb.toString().equals(splited[i])) {//Se nao foi encontrado o user a remover
								cos.write(sb.toString().getBytes());//Se é o user a remover ent n entra no if e nao eh escrito no novo ficheiro cifrado
							}
							sb.setLength(0);
						}
					}
					cis.close();
					cos.close();
					f.delete();

					if(tempFile.renameTo(new File("users/" + user + "/trustedUsers.txt"))) {
						atualizaSig(generateSig(user), user);
						outStream.writeObject(1);
					}else {
						tempFile.delete();
						outStream.writeObject(0);
					}
				}
			}
		}

		/**
		 * downloads a file from another user that trusts the logged on user
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws IllegalBlockSizeException 
		 * @throws SignatureException 
		 */

		private void download(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited, String user) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, SignatureException {

			String userID = splited[1];
			boolean integridade;

			if(userID.equals(user)){
				System.out.println("Nao pode fazer o download de um ficheiro da sua conta");
			}

			if(integridade = !verificaSig("users/" + user + "/trustedUsers.txt") || !encryptionAlgorithms.validMAC(pwMan)) {
				System.out.println("Integridade de ficheiros comprometida");
				outStream.writeObject(-3);
			}

			if(!integridade && userExistsServer(userID)){
				if(userExistsTrusted(user, userID)){

					String fileName = splited[2];
					File f = new File("users/" + userID + "/files/" + fileName);

					if(f.exists()) {

						outStream.writeObject(1);

						Cipher cInput = Cipher.getInstance("AES");
						Key key = getFileKey(f.getPath());

						cInput.init(Cipher.DECRYPT_MODE, key);
						FileInputStream fileStream = new FileInputStream(f);
						CipherInputStream cis = new CipherInputStream(fileStream, cInput);
						byte[] fileByte = new byte[1024];
						int aux;

						while((aux = cis.read(fileByte)) != -1){
							outStream.writeObject(new Boolean (true)); //envia true enqt o ciclo esta a correr
							outStream.writeObject(aux);
							outStream.write(fileByte, 0, aux);
							outStream.flush();
						}

						outStream.writeObject(new Boolean(false));
						cis.close();
						fileStream.close();

					} else {
						//caso o ficheiro que vai ser sacado nao exista
						System.out.println("O ficheiro " + fileName + " nao existe");
						outStream.writeObject(0);
					}
				} else {
					//caso o user exista no servidor mas o client n esteja na lista de amigos
					System.out.println("Users em questao nao sao amigos");
					outStream.writeObject(-1);
				}

			} else {
				System.out.println("O user q o cliente procura nao existe");
				outStream.writeObject(-2);
			}
		}

		/**
		 * sends a message to a user that trusts the logged in user
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws IOException 
		 * @throws IllegalBlockSizeException 
		 * @throws SignatureException 
		 */

		private void msg(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited, String user) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, IllegalBlockSizeException, SignatureException {
			if(encryptionAlgorithms.validMAC(pwMan) && verificaSig("users/" + user + "/trustedUsers.txt")) {
				if(userExistsServer(splited[1]) && !splited[1].equals(user)) {
					File mail = new File("users/" + splited[1] + "/inbox.txt");
					StringBuilder msg = new StringBuilder();

					if(userExistsTrusted(user, splited[1])){
						FileWriter fw = new FileWriter(mail,true); //the true will append the new data
						FileOutputStream fos = new FileOutputStream(mail);
						SecretKey key = getFileKey("users/" + splited[1] + "/inbox.txt");
						Cipher cOutput = Cipher.getInstance("AES");
						cOutput.init(Cipher.ENCRYPT_MODE, key);

						CipherOutputStream cos = new CipherOutputStream(fos, cOutput);

						for(int i = 2; i < splited.length; i++) {
							msg.append(splited[i] + " ");
						}
						String msgComplete = "Sent from: " + user + "\nMessage: " + msg.toString() + "\n\n";
						cos.write(msgComplete.getBytes());//appends message to the file
						fw.close();
						cos.close();
						outStream.writeObject(1);
					}else {
						outStream.writeObject(0);
					}
				}else if(userExistsServer(splited[1]) && splited[1].equals(user)){
					outStream.writeObject(-1);
				}else {
					outStream.writeObject(-2);
				}
			}else {
				outStream.writeObject(-3);
				System.out.println("Integridade dos ficheiros compremetida");
			}


		}

		/**
		 * presents on screen all unread messages of the logged in user
		 * 
		 * @param inStream
		 * @param outStream
		 * @param splited - input string splited by spaces
		 * @param user - userID
		 * @throws IOException
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws IllegalBlockSizeException 
		 * @throws InvalidKeyException 
		 * @throws SignatureException 
		 */
		//Em teoria ta feito, falta testar como todos os outros
		private void collect(ObjectInputStream inStream, ObjectOutputStream outStream, String[] splited,
				String user) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, SignatureException {


			//if(verificaSig("users/" + user + "/inbox.txt")) {
			File inbox = new File("users/" + user + "/inbox.txt");
			FileInputStream fis = new FileInputStream(inbox);
			SecretKey key = getFileKey("users/" + user + "/inbox.txt");
			Cipher cInput = Cipher.getInstance("AES");
			cInput.init(Cipher.DECRYPT_MODE, key);
			CipherInputStream cis = new CipherInputStream(fis, cInput);
			StringBuilder sb = new StringBuilder();
			int counter = 0;
			int letra;

			while((letra = cis.read()) != -1) {
				System.out.println(sb.toString());
				sb.append((char)letra);
				counter++;
			}

			cis.close();

			if(counter > 0) {
				outStream.writeObject(sb.toString());
				inbox.delete();
				File newInbox = new File("users/" + user + "/inbox.txt");
				newInbox.createNewFile();				
			}else {
				outStream.writeObject("Nao tem mensagens por ler");
			}
			//			}else {
			//				System.out.println("Integridade dos ficheiros compremetida");
			//				outStream.writeObject("Erro, tente mais tarde");
			//			}
		}

		/**
		 * disconnects from the server
		 * 
		 * @param user - userID
		 */

		private void quit(String user) {
			System.out.println("O client " + user + " vai disconectar");
		}


		/**
		 * checks if the logged in user is trusted
		 * 
		 * @param userAdd - logged in userID
		 * @param userClient - userID
		 * 
		 * @return true in case the logged in user is trusted, false otherwise
		 * 
		 * @throws IOException
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws IllegalBlockSizeException 
		 */

		private boolean userExistsTrusted(String userAdd, String userClient) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException {

			File f = new File("users/" + userClient + "/trustedUsers.txt");
			int letra;
			FileInputStream newFile = new FileInputStream(f);
			Cipher c = Cipher.getInstance("AES");
			SecretKey key = getFileKey("users/" + userClient + "/trustedUsers.txt");
			c.init(Cipher.DECRYPT_MODE, key);
			CipherInputStream cis = new CipherInputStream(newFile, c);
			StringBuilder sb = new StringBuilder();
			System.out.println("available " + cis.available());
			System.out.println("available2 " + newFile.available());

			while((letra = cis.read()) != -1) {
				if((char)letra != '\n') {
					sb.append(((char)letra));
				}else {
					if(sb.toString().equals(userAdd)) {
						cis.close();
						newFile.close();
						return true;
					}
					sb.setLength(0);
				}
				System.out.println("sb: " + sb.toString());
			}
			cis.close();
			newFile.close();
			return false;
		}


		/**
		 * checks if the userID exists on server
		 * 
		 * @param user - userID
		 * 
		 * @return true in case the user exists on server, false otherwise
		 * 
		 * @throws IOException
		 */

		private boolean userExistsServer(String user) throws IOException {

			File f = new File("users.txt");

			try {

				BufferedReader br = new BufferedReader(new FileReader(f.getName()));
				String line = br.readLine();
				String[] userName;

				while(line != null) {
					userName = line.split(":");
					System.out.println("user: " + userName[0]);
					if(userName[0].equals(user)) {
						br.close();
						return true;
					}
					line = br.readLine();
				}
				br.close();
				return false;

			} catch (FileNotFoundException e) {
				System.out.println("Erro em userExists, o ficheiro users nao existe no servidor");
				e.printStackTrace();
			}
			return false;
		}

		/**
		 * 
		 * @return
		 * @throws NoSuchAlgorithmException
		 */
		private SecretKey generateKey() throws NoSuchAlgorithmException {
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			return kg.generateKey();
		}

		/**
		 * 
		 * @return
		 */
		private PrivateKey getPiK(){
			PrivateKey pk = null;
			try {
				pk = (PrivateKey) ks.getKey("myServer", pwKs.toCharArray());
			} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}	
			return pk;
		}

		/**
		 * 
		 * @return
		 */
		private PublicKey getPuK() {
			Certificate cert = null;
			try {
				cert = (Certificate) ks.getCertificate("myServer");
			} catch (KeyStoreException e) {
				e.printStackTrace();
			}
			return cert.getPublicKey();
		}

		/**
		 * 
		 * @param key
		 * @param fileName
		 * @param user
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException 
		 * @throws InvalidKeyException 
		 * @throws IOException 
		 * @throws IllegalBlockSizeException 
		 */
		private void saveFileKey(SecretKey key, String path) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException {

			Cipher c1 = Cipher.getInstance("RSA");
			PublicKey pk = getPuK();
			c1.init(Cipher.WRAP_MODE, pk);
			byte[] wrappedKey = c1.wrap(key);

			File kFile = new File(path);
			kFile.createNewFile();
			FileOutputStream keyOutputFile = new FileOutputStream(kFile);
			System.out.println("path: " + path);
			System.out.println("wrappedKey: " + wrappedKey);
			keyOutputFile.write(wrappedKey);
			keyOutputFile.close();
		}

		/**
		 * 
		 * @param path
		 * @return
		 * @throws InvalidKeyException
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws IOException
		 * @throws IllegalBlockSizeException 
		 */
		private SecretKey getFileKey(String path) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, IllegalBlockSizeException {

			File keyFile = new File(path + ".key");
			if(keyFile.exists()) {
				System.out.println("entra onde nao deve");
				FileInputStream keyFileInput = new FileInputStream(path + ".key");

				byte[] wrappedKey = new byte[keyFileInput.available()];
				Cipher c1 = Cipher.getInstance("RSA");

				keyFileInput.read(wrappedKey);
				PrivateKey pk = getPiK();
				c1.init(Cipher.UNWRAP_MODE, pk);
				keyFileInput.close();

				return (SecretKey)c1.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
			}else {
				keyFile.createNewFile();
				SecretKey key = generateKey();

				saveFileKey(key, path + ".key");

				return key;
			}	
		}

		/**
		 * 
		 * @param path
		 * @return
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeyException
		 * @throws SignatureException
		 * @throws IOException
		 * @throws IllegalBlockSizeException
		 */
		private boolean verificaSig(String path) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, SignatureException, IOException, IllegalBlockSizeException {

			File f = new File(path);
			Cipher cInput = Cipher.getInstance("AES");
			SecretKey key = getFileKey(f.getPath());
			FileInputStream fis = new FileInputStream(f);

			cInput.init(Cipher.DECRYPT_MODE, key);

			CipherInputStream cis = new CipherInputStream(fis, cInput);
			StringBuilder sb = new StringBuilder();
			int letra;
			PrivateKey pk = getPiK();
			Signature s = Signature.getInstance("MD5withRSA");
			byte[] sig;

			s.initSign(pk);

			//faz update ah signature
			while((letra = cis.read()) != -1) {
				if((char)letra != '\n') {
					sb.append((char)letra);
				}else {
					s.update(sb.toString().getBytes());
					sb.setLength(0);
				}
			}

			//Recebe o array de bytes que eh a signature gerada
			sig = s.sign();
			cis.close();
			String pathSig = path.substring(0, path.length() - 4);
			//			f = new File(pathSig);
			//			fis = new FileInputStream(f);
			System.out.println(pathSig);
			BufferedReader br = new BufferedReader(new FileReader(pathSig + ".sig"));
			String sigNovo = DatatypeConverter.printBase64Binary(sig);
			String sigAntigo = br.readLine();

			if(sigNovo.equals(sigAntigo)) {
				br.close();
				return true;
			}else {
				br.close();
				return false;
			}

			//Verifica se as assinaturas sao iguais, se nao entao o ficheiro foi alterado
			//			if(sig.length == f.length()) {
			//				for(int i = 0; i < sig.length; i++) {
			//					if(sig[i] != fis.read()) {
			//						cis.close();
			//						fis.close();
			//						return false;
			//					}
			//				}
			//			}else {
			//				cis.close();
			//				fis.close();
			//				return false;
			//			}
			//cis.close();
			//fis.close();
			//return true;
		}

		/**
		 * 
		 * @param sig
		 * @param user
		 * @throws IOException
		 */
		private void atualizaSig(byte[] sig, String user) throws IOException {
			File sigFile = new File("users/" + user + "/trustedUsers.sig");
			if(sigFile.exists()) {
				sigFile.delete();
			}
			sigFile.createNewFile();
			FileWriter fw = new FileWriter("users/" + user + "/trustedUsers.sig");
			//FileOutputStream newFile = new FileOutputStream(sigFile);
			//ObjectOutputStream oos = new ObjectOutputStream(newFile);
			//oos.write(sig);
			//oos.close();
			//newFile.close();	
			fw.write(DatatypeConverter.printBase64Binary(sig));
			fw.close();
			System.out.println(sigFile.length());
		}

		/**
		 * 
		 * @param user
		 * @return
		 * @throws InvalidKeyException 
		 * @throws NoSuchAlgorithmException 
		 * @throws NoSuchPaddingException 
		 * @throws IOException 
		 * @throws SignatureException 
		 * @throws IllegalBlockSizeException 
		 */
		private byte[] generateSig(String user) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, SignatureException, IOException, IllegalBlockSizeException {
			File f = new File("users/" + user + "/trustedUsers.txt");
			FileInputStream fis = new FileInputStream(f);
			Cipher c = Cipher.getInstance("AES");
			SecretKey key = getFileKey(f.getPath());
			c.init(Cipher.DECRYPT_MODE, key);
			CipherInputStream cis = new CipherInputStream(fis, c);
			PrivateKey pk = getPiK();
			Signature s = Signature.getInstance("MD5withRSA");
			s.initSign(pk);
			int letra;
			StringBuilder sb = new StringBuilder();
			while((letra = cis.read()) != -1) {
				if((char)letra != '\n') {
					sb.append((char)letra);
				}else {
					s.update(sb.toString().getBytes());
					sb.setLength(0);
				}
			}
			cis.close();
			return s.sign();			
		}
	}
}
