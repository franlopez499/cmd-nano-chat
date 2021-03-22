import java.io.IOException;

import es.um.redes.nanoChat.directory.connector.DirectoryConnector;


public class DirectoryTester {

	public static void main(String[] args) throws IOException {
		
		DirectoryConnector dc = new DirectoryConnector("localhost");
		System.out.println(dc.registerServerForProtocol(0, 6868));
		System.out.println(dc.getServerForProtocol(0).toString());
		//Direccion IP y puerto del directorio.
		
		
	}

}
