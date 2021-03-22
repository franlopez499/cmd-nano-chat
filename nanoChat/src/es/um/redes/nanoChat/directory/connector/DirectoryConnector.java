package es.um.redes.nanoChat.directory.connector;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	
	//Tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int PACKET_MAX_SIZE = 128;
	//Puerto en el que atienden los servidores de directorio
	private static final int DEFAULT_PORT = 6868;
	//Valor del TIMEOUT
	private static final int TIMEOUT = 1000;
	
	// Opcodes
	private static final byte OPCODE_REGISTER = 0x01;
	private static final byte OPCODE_OK_REGISTER = 0x02;
	private static final byte OPCODE_QUERY = 0x04;
	private static final byte OPCODE_SERVER_INFO = 0x05;
	private static final byte OPCODE_SERVER_NOT_FOUND = 0x06;
	
	
	
	
	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		// A partir de la dirección y del puerto generar la dirección de conexión para el Socket
		this.directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress),DEFAULT_PORT);
		
		// Crear el socket UDP
		socket = new DatagramSocket();
	}

	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {

		// Generar el mensaje de consulta llamando a buildQuery()
		byte[] message = buildQuery(protocol);
		
		// Construir el datagrama con la consulta
		DatagramPacket pckt = new DatagramPacket(message, message.length, this.directoryAddress);
		
		// Enviar datagrama por el socket
		socket.send(pckt);
		
		// preparar el buffer para la respuesta
		message = new byte [PACKET_MAX_SIZE];
		
		// Establecer el temporizador para el caso en que no haya respuesta
		socket.setSoTimeout(1000);
		
		// Recibir la respuesta
		pckt = new DatagramPacket(message, message.length);
		socket.receive(pckt);
		
		// Procesamos la respuesta para devolver la dirección que hay en ella
		return getAddressFromResponse(pckt);	
		
		
		
	}


	//Método para generar el mensaje de consulta (para obtener el servidor asociado a un protocolo)
	private byte[] buildQuery(int protocol) {
		// Devolvemos el mensaje codificado en binario según el formato acordado
		
		ByteBuffer bb = ByteBuffer.allocate(5); //Crea un buffer de 5 bytes
		bb.put(OPCODE_QUERY); //Inserta un campo de 1 byte (opcode es byte)
		bb.putInt(protocol); //Inserta campo de 4 bytes (parameter es int)
		byte[] men = bb.array(); //Obtiene  el mensaje como byte[]
		return men;
	}

	//Método para obtener la dirección de internet a partir del mensaje UDP de respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		// Analizar si la respuesta no contiene dirección (devolver null)
		// Si la respuesta no está vacía, devolver la dirección (extraerla del mensaje)
		
		
		
		InetSocketAddress in = null;
		byte[] ipAddress = new byte[4];
		ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData());
		byte opcode = byteBuffer.get();
		
		if(opcode == OPCODE_SERVER_INFO) {
			byteBuffer.get(ipAddress);
			int port = byteBuffer.getInt();
			return new InetSocketAddress(InetAddress.getByAddress(ipAddress),port);
		}
		return in;
		
		
	
	}
	
	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {

		// Construir solicitud de registro (buildRegistration)
		byte [] request = buildRegistration(protocol, port);
		// Enviar solicitud
		
		
		DatagramPacket packet = new DatagramPacket(request, request.length, directoryAddress);
		socket.send(packet);
		// Recibe respuesta
		byte[] response = new byte [PACKET_MAX_SIZE];
		packet = new DatagramPacket(response, response.length);
		socket.setSoTimeout(TIMEOUT);
		socket.receive(packet);
		
		// Procesamos la respuesta para ver si se ha podido registrar correctamente
		ByteBuffer receivedResponse = ByteBuffer.wrap(packet.getData());
		
		if ( receivedResponse.get() == OPCODE_OK_REGISTER) 
			return true;
		
			

		return false;
	}


	//Método para construir una solicitud de registro de servidor
	//OJO: No hace falta proporcionar la dirección porque se toma la misma desde la que se envió el mensaje
	private byte[] buildRegistration(int protocol, int port) {
		// Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(9); 
		bb.put(OPCODE_REGISTER);
		
		bb.putInt(port);
		bb.putInt(protocol);
		byte[] men = bb.array();
		return men;
	}

	public void close() {
		socket.close();
	}
}
