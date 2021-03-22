package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import es.um.redes.nanoChat.messageML.NCDoubleMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCSimpleMessage;
import es.um.redes.nanoChat.messageML.NCTripleMessage;
import es.um.redes.nanoChat.messageML.NCroomListMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;
import es.um.redes.nanoChat.server.roomManager.NCSimpleRoom;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {

	private Socket socket = null;
	//Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	//Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	//Usuario actual al que atiende este Thread
	String user;
	//RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	//Sala actual
	String currentRoom;

	//Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	//Main loop
	public void run() {
		try {
			//Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			//En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			//Mientras que la conexión esté activa entonces...
			while (true) {
				// Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {
				// 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
				case NCMessage.OP_ROOMLIST:
					sendRoomList();
					break;
				case NCMessage.OP_ENTER_ROOM:
					NCRoomMessage nuevo = (NCRoomMessage) message;
					roomManager = serverManager.enterRoom(user, nuevo.getName(),socket);
					
					// 2) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de la sala,
					// 2) notificamos al usuario que ha sido aceptado y procesamos mensajes con processRoomMessages()
					// 2) Si el usuario no es aceptado en la sala entonces se le notifica al cliente
					if(roomManager != null) {
						currentRoom = nuevo.getName();
						String noti = "El usuario "+this.user+" ha entrado en la sala.";
						((NCSimpleRoom) roomManager).notificarMiembros(this.user, noti);
						NCSimpleMessage response = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_ENTER_ROOM_OK);
						dos.writeUTF(response.toEncodedString());
						processRoomMessages();
						
						


					}else {
						NCSimpleMessage response = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_ENTER_ROOM_ERROR);
						dos.writeUTF(response.toEncodedString());
					}

					break;

				default:
					System.out.println("Error en el opcode del serverThread");

				}
			}
		} catch (Exception e) {
			//If an error occurs with the communications the user is removed from all the managers and the connection is closed
			System.out.println("* User "+ user + " disconnected.");
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		}
		finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	//Obtenemos el nick y solicitamos al ServerManager que verifique si está duplicado
	private void receiveAndVerifyNickname() {
		//La lógica de nuestro programa nos obliga a que haya un nick registrado antes de proseguir
		// Entramos en un bucle hasta comprobar que alguno de los nicks proporcionados no está duplicado
		// Extraer el nick del mensaje
		// Validar el nick utilizando el ServerManager - addUser()
		// Contestar al cliente con el resultado (éxito o duplicado)

		
		while(true) {
			try {
				if((dis.available() != 0)) {
					NCRoomMessage response = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
					if(!serverManager.addUser(response.getName())) {
						NCSimpleMessage tmp = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_NICK_DUPLICATED);
						String encodedMessage = tmp.toEncodedString();
						dos.writeUTF(encodedMessage);
					}else {
						this.user = response.getName();
						NCSimpleMessage tmp = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_NICK_OK);
						String encodedMessage = tmp.toEncodedString();
						dos.writeUTF(encodedMessage);
						break;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	//Mandamos al cliente la lista de salas existentes
	private void sendRoomList() throws IOException  {
		// La lista de salas debe obtenerse a partir del RoomManager y después enviarse mediante su mensaje correspondiente 		

		ArrayList<NCRoomDescription> arrayRooms = serverManager.getRoomList();

		NCroomListMessage tmp = (NCroomListMessage) NCMessage.makeRoomListMessage(NCMessage.OP_ROOMLISTRES,arrayRooms);
		String encodedMessage = tmp.toEncodedString();
		dos.writeUTF(encodedMessage);


	}
	
	
	private void sendRoomInfo(NCRoomDescription description) throws IOException {
		
		ArrayList<NCRoomDescription> roomInfo = new ArrayList<>(1);
		roomInfo.add(description);
		NCroomListMessage tmp = (NCroomListMessage) NCMessage.makeRoomListMessage(NCMessage.OP_ROOMLISTRES,roomInfo);
		dos.writeUTF(tmp.toEncodedString());
		
	}



	private void processRoomMessages()  {
		// Comprobamos los mensajes que llegan hasta que el usuario decida salir de la sala
		boolean exit = false;
		NCMessage message;
		while (!exit) {
			// Se recibe el mensaje enviado por el usuario
			// Se analiza el código de operación del mensaje y se trata en consecuencia
			try {
			if(dis.available() != 0) { // Para eliminar excepciones en el server al cerrar con el cuadrado rojo
				
					
					message = NCMessage.readMessageFromSocket(dis);
					byte opcode = message.getOpcode();
					switch(opcode) {
					case NCMessage.OP_SEND_MSG:
						NCDoubleMessage tmp = (NCDoubleMessage) message;
						String user = tmp.getName();
						String mensaje = tmp.getMessage();
						roomManager.broadcastMessage(user, mensaje);
						
						break;
					case NCMessage.OP_DM:
						NCTripleMessage msg = (NCTripleMessage) message;
						String userSrc = msg.getName();		// source 
						String data = msg.getMessage(); // destinatario
						String  userDst = msg.getDate(); // data (mensaje de texto)
						NCSimpleRoom rm = (NCSimpleRoom)roomManager;
						rm.personalMessage(userSrc, userDst, data);
						break;
					case NCMessage.OP_ROOM_INFO:
						NCRoomMessage nuevo = (NCRoomMessage) message;
						NCRoomDescription description = serverManager.getRoomInfo(nuevo.getName());
						sendRoomInfo(description);
						break;
					case NCMessage.OP_EXIT_ROOM:
						serverManager.leaveRoom(this.user, roomManager.getDescription().roomName);
						String noti = "El usuario "+this.user+" ha salido de la sala.";
						((NCSimpleRoom) roomManager).notificarMiembros(this.user, noti);
						exit = true;
						break;
					case NCMessage.OP_RENAME_ROOM:
						NCDoubleMessage msj = (NCDoubleMessage) message;
						String u = msj.getName();		// source 
						String newRoomName = msj.getMessage(); // NewName
						
						
						serverManager.renameRoom(u,roomManager.getRoomName(),newRoomName);
						NCSimpleRoom rmanager = (NCSimpleRoom)roomManager;	
						rmanager.broadcastMessageRename(u, newRoomName);
						currentRoom = newRoomName;
						
						break;
					default:
						break;
					}

				

			}
			}catch (IOException e) {
				e.printStackTrace();
			}
			 




		}

	}
}
