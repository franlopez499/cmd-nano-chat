package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import es.um.redes.nanoChat.messageML.NCDoubleMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCSimpleMessage;
import es.um.redes.nanoChat.messageML.NCTripleMessage;
import es.um.redes.nanoChat.messageML.NCroomListMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		//  Se crea el socket a partir de la dirección proporcionada
		//  Se extraen los streams de entrada y salida

		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());

		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());

	}


	// Método para registrar el nick en el servidor. Nos informa sobre si la
	// inscripción se hizo con éxito o no.
	public boolean registerNickname(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//Creamos un mensaje de tipo RoomMessage con opcode OP_NICK en el que se inserte el nick
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		//Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		//Escribimos el mensaje en el flujo de salida, es decir, provocamos que se envíe por la conexión TCP
		dos.writeUTF(rawMessage);
		// Leemos el mensaje recibido como respuesta por el flujo de entrada 
		// Analizamos el mensaje para saber si está duplicado el nick (modificar el return en consecuencia)
		
		NCSimpleMessage response = (NCSimpleMessage) NCMessage.readMessageFromSocket(dis);
		return (response.getOpcode() == NCMessage.OP_NICK_OK);
	}

	// Método para obtener la lista de salas del servidor
	public ArrayList<NCRoomDescription> getRooms() throws IOException { 
		// Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		//  completar el método

		NCSimpleMessage message = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_ROOMLIST);

		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);

		
		
		NCroomListMessage response = (NCroomListMessage)NCMessage.readMessageFromSocket(dis);	
		
		return response.getRoomList();
		
			 
		
		
	}

	// Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		// Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or
		// RCV(REJECT)
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER_ROOM, room);
		// Convertimos el mensaje a String que lo represente
		String stringMessage = message.toEncodedString();
		// Escribimos el mensaje en el flujo de salida
		dos.writeUTF(stringMessage);
		// Leemos el mensaje recibido como respuesta y le hacemos un casting
		NCSimpleMessage response = (NCSimpleMessage) NCMessage.readMessageFromSocket(dis);
		// Comprobamos si el usuario ha sido aceptado en la sala)
		return (response.getOpcode() == NCMessage.OP_ENTER_ROOM_OK);
	}

	// Método para salir de una sala
	public void leaveRoom(String room) throws IOException {
		// Funcionamiento resumido: SND(EXIT_ROOM)
		//  completar el método
		NCSimpleMessage message = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_EXIT_ROOM);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		
	}

	// Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}

	// IMPORTANTE!!
	//  Es necesario implementar métodos para recibir y enviar mensajes de chat
	// a una sala+
	public void sendChatMessage(String user,String chatMessage) {
		try {
			NCDoubleMessage mensaje =(NCDoubleMessage)NCMessage.makeDoubleMessage(NCMessage.OP_SEND_MSG,user,chatMessage);
			String rawMessage = mensaje.toEncodedString();
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendDmMessage(String userSrc,String userDst,String dmMessage) {
		try {
			NCTripleMessage mensaje =(NCTripleMessage)NCMessage.makeTripleMessage(NCMessage.OP_DM,userSrc,userDst,dmMessage);
			String rawMessage = mensaje.toEncodedString();
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public void sendRenameRoom(String userSrc,String newRoomName) {
		try {
			NCDoubleMessage mensaje =(NCDoubleMessage)NCMessage.makeDoubleMessage(NCMessage.OP_RENAME_ROOM,userSrc,newRoomName);
			String rawMessage = mensaje.toEncodedString();
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public NCMessage receiveChatMessage() {
		try {
			
			NCMessage message = NCMessage.readMessageFromSocket(dis);
			
			return message;
			
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	// Método para pedir la descripción de una sala
	public NCRoomDescription getRoomInfo(String room) throws IOException {
		// Funcionamiento resumido: SND(GET_ROOMINFO) and RCV(ROOMINFO)
		//  Construimos el mensaje de solicitud de información de la sala
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ROOM_INFO,room);
		dos.writeUTF(message.toEncodedString());
		// específica
		//  Recibimos el mensaje de respuesta
		NCroomListMessage response = (NCroomListMessage) NCMessage.readMessageFromSocket(dis);
		return response.getRoomList().get(0);
		//  Devolvemos la descripción contenida en el mensaje
	}

	// Método para cerrar la comunicación con la sala
	//  (Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}

}
