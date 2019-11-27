package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;

public class encryptionAlgorithms {
	
	/**
	 * Devolve uma linha no formato proposto mas com a palavra pass depois do hash j� com salt
	 * @param userData - password
	 * @throws NoSuchAlgorithmException 
	 */
	public static String hashingDados(String userData) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA");
		Random rnd = new Random();
		int salt = rnd.nextInt();
		String nPW = salt + userData;
		byte[] hashed = md.digest(nPW.getBytes());
		String pwHashed = DatatypeConverter.printBase64Binary(hashed);
		System.out.println(pwHashed);
		return salt + ":" + pwHashed;
	}

	public static byte[] geraMAC(String managerPW) {
		byte[] salt = {(byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, 
				(byte) 0x3e, (byte) 0xea, (byte) 0xf2};
		PBEKeySpec keySpec	= 	new	PBEKeySpec(managerPW.toCharArray(), salt, 20);  
		SecretKeyFactory kf;
		SecretKey key;
		Mac mac = null;
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File("users.txt")))){
			mac	= Mac.getInstance("HmacSHA1");
			kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
			key = kf.generateSecret(keySpec);
			mac.init(key);

			String linha;
			while((linha = br.readLine()) != null) {
				mac.update(linha.getBytes());
			}

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mac.doFinal();
	}

	public static void atualizaMAC(byte[] mac) {
		try {

			File f = new File("mac.txt");
			if(f.exists()) {
				f.delete();
			}
			f.createNewFile();
			FileWriter fw = new FileWriter("mac.txt");
//			ObjectOutputStream	oos	= new ObjectOutputStream(fos);
			System.out.println("mac : " + mac.length);
			fw.write(DatatypeConverter.printBase64Binary(mac));
			fw.close();
			//oos.close();
			System.out.println(f.length());

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean validMAC(String pass) throws IOException {

		File f = new File("mac.txt");

		if(f.exists()) {
			
			//BufferedInputStream in = new BufferedInputStream(new FileInputStream("mac.txt"));
			BufferedReader br = new BufferedReader(new FileReader("mac.txt"));
			

			byte[] mac = geraMAC(pass);
			String macNovo = DatatypeConverter.printBase64Binary(mac);
			String macAntigo = br.readLine();
			

			if(macNovo.equals(macAntigo)) {
				br.close();
				return true;
			}else {
				br.close();
				return false;
			}
//			for(int i = 0; i<mac.length; i++) {
//				if(mac[i] != senderMacCode[i]) {
//					in.close();
//					return false;
//				}
//			}
//			in.close();
//			return true;	
		}else {
			atualizaMAC(geraMAC(pass));
			return true;
		}
	}

}
