package es.um.redes.nanoChat.directory.server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class DirectoryThread extends Thread {

	//Tamaño máximo del paquete UDP
	private static final int PACKET_MAX_SIZE = 128;
	public final int PORT = 6868;
	//Estructura para guardar las asociaciones ID_PROTOCOLO -> Dirección del servidor
	protected HashMap<Integer,InetSocketAddress> servers;

	//Socket de comunicación UDP
	protected DatagramSocket socket = null;
	
	//Probabilidad de descarte del mensaje
	protected double messageDiscardProbability;

	// Opcodes
	private static final byte OPCODE_REGISTER = 0x01;
	private static final byte OPCODE_OK_REGISTER = 0x02;
	private static final byte OPCODE_QUERY = 0x04;
	private static final byte OPCODE_SERVER_INFO = 0x05;
	private static final byte OPCODE_SERVER_NOT_FOUND = 0x06;
		
		
	public DirectoryThread(String name, int directoryPort,
			double corruptionProbability)
			throws SocketException {
		super(name);
		// Anotar la dirección en la que escucha el servidor de Directorio
 		// Crear un socket de servidor

		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		socket = new DatagramSocket(serverAddress);
		
		messageDiscardProbability = corruptionProbability;
		//Inicialización del mapa
		servers = new HashMap<Integer,InetSocketAddress>();
	}

	public void run() {
		byte[] buf = new byte[PACKET_MAX_SIZE];
		System.out.println("Directory starting...");
		boolean running = true;
		while (running) {

				// 1) Recibir la solicitud por el socket
				// 2) Extraer quién es el cliente (su dirección)
				// 3) Vemos si el mensaje debe ser descartado por la probabilidad de descarte
			DatagramPacket pckt = new DatagramPacket(buf, buf.length);	
			
			try {
				socket.receive(pckt);
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
			InetSocketAddress clientAddress = (InetSocketAddress) pckt.getSocketAddress();
			
				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED corrupt request from... ");
					continue;
				}
				
				// (Solo Boletín 2) Devolver una respuesta idéntica en contenido a la solicitud
				
				
				
				// 4) Analizar y procesar la solicitud (llamada a processRequestFromCLient)
				try {
					processRequestFromClient(buf, clientAddress);
				} catch (IOException e) {
					//  Auto-generated catch block
					e.printStackTrace();
				}
				// 5) Tratar las excepciones que puedan producirse
		}
		socket.close();
	}

	//Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// 1) Extraemos el tipo de mensaje recibido
		
		ByteBuffer ret = ByteBuffer.wrap(data); //Toma como entrada data 
		byte opcode = ret.get(); //Obtiene un campo de 1 byte
		
		
		
		// 2) Procesar el caso de que sea un registro y enviar mediante sendOK
		int  port, protocol;
		
		
		if (opcode == OPCODE_REGISTER) {
			
			
			port = ret.getInt();
			protocol = ret.getInt();
			servers.put(protocol, new InetSocketAddress(clientAddr.getAddress(),port));
			sendOK(clientAddr);
			
			
		}
			
		
		// 3) Procesar el caso de que sea una consulta
		// 3.1) Devolver una dirección si existe un servidor (sendServerInfo)
		// 3.2) Devolver una notificación si no existe un servidor (sendEmpty)
		
		if (opcode == OPCODE_QUERY) {
			protocol = ret.getInt();
			if (servers.containsKey(protocol)) {
				InetSocketAddress serverAddress = servers.get(protocol);
				sendServerInfo(serverAddress, clientAddr);
			} else sendEmpty(clientAddr);//empty pq ahora es noEncontrado
		}
			
			
		
		

		
		
	}

	//Método para enviar una respuesta vacía (no hay servidor)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		// Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(1); 
		bb.put(OPCODE_SERVER_NOT_FOUND); 
		byte[] men = bb.array(); 
		
		// Enviar respuesta
		DatagramPacket packet = new DatagramPacket(men, men.length, clientAddr);
		socket.send(packet);
	}
	

	//Método para enviar la dirección del servidor al cliente
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {
		// Obtener la representación binaria de la dirección
		
		
		int port = serverAddress.getPort();
		
		// Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(9); 
		bb.put(OPCODE_SERVER_INFO);
		bb.put(serverAddress.getAddress().getAddress());
		bb.putInt(port);
		byte[] men = bb.array(); 
		
		// Enviar respuesta
		DatagramPacket packet = new DatagramPacket(men, men.length, clientAddr);
		socket.send(packet);
	}

	//Método para enviar la confirmación del registro
	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		// Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(1); 
		bb.put(OPCODE_OK_REGISTER); 
		byte[] men = bb.array(); 
		
		
		// Enviar respuesta
		DatagramPacket packet = new DatagramPacket(men, men.length, clientAddr);
		socket.send(packet);
	}
}
