Correr no eclipse:
	
	No UserManager:
		1. Apenas correr o programa
		2. inserir a ManagerPassword (password = grupo20) e de seguida a keyStore password (password = 123456789)
		
	No servidor (MsgFileServer):
		1. adicionar a run configurations o argumento '12345' que corresponde ao porto do servidor
		2. adicionar a run configurations o argumento 'grupo20' que corresponde a ManagerPassword
		3. adicionar a run configurations o argumento '123456789' que corresponde a keyStore password
		4. adicionar a VM arguments (tambem em run configurations) "-Djava.security.manager -Djava.security.policy=server.policy"
		5. correr o servidor
		
	No cliente (MsgFile):
		1.adicionar a run configurations os argumentos "127.0.0.1:12345 <user> <password>"
		2.adicionar a VM arguments (tambem em run configurations) "-Djava.security.manager -Djava.security.policy=client.policy"
		3.correr o cliente
	
	
	
Correr na consola:

	No UserManager:
		1. escrever na consola "javac UserManager.java" (para compilar o ficheiro)
		2. Apenas correr o programa
		3. inserir a ManagerPassword (password = grupo20) e de seguida a keyStore password (password = 123456789)
		
	No servidor (MsgFileServer):
		1.escrever na consola "javac MsgFileServer.java" (para compilar o ficheiro)
		2.escrever na consola "java -Djava.security.manager -Djava.security.policy=server.policy MsgFileServer 12345 grupo20 123456789" (para correr o server)
		
	No cliente (MsgFile):
		1.escrever na consola "javac MsgFile.java" (para compilar o ficheiro)
		2.escrever na consola "java -Djava.security.manager -Djava.security.policy=client.policy MsgFile 127.0.0.1:12345 <user> <password>" (para correr o server)