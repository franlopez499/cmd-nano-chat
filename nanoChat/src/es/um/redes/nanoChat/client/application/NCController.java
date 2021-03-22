package es.um.redes.nanoChat.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;


import es.um.redes.nanoChat.client.comm.NCConnector;
import es.um.redes.nanoChat.client.shell.NCCommands;
import es.um.redes.nanoChat.client.shell.NCShell;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;
import es.um.redes.nanoChat.messageML.NCDoubleMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCTripleMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCController {
	//Diferentes estados del cliente de acuerdo con el autómata
	private static final byte PRE_CONNECTION = 1;
	private static final byte PRE_REGISTRATION = 2;
	private static final byte REGISTERED = 3;
	private static final byte IN_ROOM = 4;
	//Código de protocolo implementado por este cliente
	// Cambiar para cada grupo
	private static final int PROTOCOL = 97484746;
	//Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;
	//Conector para enviar y recibir mensajes con el servidor de NanoChat
	private NCConnector ncConnector;
	//Shell para leer comandos de usuario de la entrada estándar
	private NCShell shell;
	//Último comando proporcionado por el usuario
	private byte currentCommand;
	//Nick del usuario
	private String nickname;
	//Sala de chat en la que se encuentra el usuario (si está en alguna)
	private String room;
	//Mensaje enviado o por enviar al chat
	private String chatMessage;
	private String userDst;
	private String newRoomName;
	//Dirección de internet del servidor de NanoChat
	private InetSocketAddress serverAddress;
	//Estado actual del cliente, de acuerdo con el autómata
	private byte clientStatus = PRE_CONNECTION;

	//Constructor
	public NCController() {
		shell = new NCShell();
	}

	//Devuelve el comando actual introducido por el usuario
	public byte getCurrentCommand() {		
		return this.currentCommand;
	}

	//Establece el comando actual
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	//Registra en atributos internos los posibles parámetros del comando tecleado por el usuario
	public void setCurrentCommandArguments(String[] args) {
		//Comprobaremos también si el comando es válido para el estado actual del autómata
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				nickname = args[0];
			break;
		case NCCommands.COM_ENTER:
			room = args[0];
			for(int i=1; i<args.length;i++)
				room += " "+args[i];
			
			break;
		case NCCommands.COM_SEND:
			chatMessage = args[0];
			break;
		case NCCommands.COM_DM:
			String str = args[0];
			String[] splitStr = str.trim().split("\\s+");
			userDst = splitStr[0];
			chatMessage = splitStr[1];
			for(int i = 2 ; i< splitStr.length;i++)
				chatMessage += " "+splitStr[i];
			break;
		case NCCommands.COM_RENAME:
			String strs = args[0];
			String[] splitStrs = strs.trim().split("\\s+");
			newRoomName = splitStrs[0];
			for(int i = 1 ; i< splitStrs.length;i++)
				newRoomName += " "+splitStrs[i];
			break;
		default:
			break;
		}
	}

	//Procesa los comandos introducidos por un usuario que aún no está dentro de una sala
	public void processCommand() {
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				registerNickName();
			else
				System.out.println("* You have already registered a nickname ("+nickname+")");
			break;
		case NCCommands.COM_ROOMLIST:
			// LLamar a getAndShowRooms() si el estado actual del autómata lo permite
			// Si no está permitido informar al usuario
			if(clientStatus == REGISTERED) {
				getAndShowRooms();
			}else {
				System.out.println("No esta permitido obtener las salas. Debe estar fuera de una sala y tener un nick registrado.");
			}
			break;
		case NCCommands.COM_ENTER:
			// LLamar a enterChat() si el estado actual del autómata lo permite
			// Si no está permitido informar al usuario
			if(clientStatus == REGISTERED) {
				enterChat();
			}else {
				System.out.println("No esta permitido entrar a una sala sino te has registrado con un nick.");
			}
			break;
		case NCCommands.COM_QUIT:
			//Cuando salimos tenemos que cerrar todas las conexiones y sockets abiertos
			ncConnector.disconnect();			
			directoryConnector.close();
			break;
		default:
		}
	}
	
	//Método para registrar el nick del usuario en el servidor de NanoChat
	private void registerNickName() {
		try {
			//Pedimos que se registre el nick (se comprobará si está duplicado)
			boolean registered = ncConnector.registerNickname(nickname);
			// Cambiar la llamada anterior a registerNickname() al usar mensajes formateados 
			if (registered) {
				//Si el registro fue exitoso pasamos al siguiente estado del autómata
				clientStatus = REGISTERED;
				System.out.println("* Your nickname is now "+nickname);
			}
			else
				//En este caso el nick ya existía
				System.out.println("* The nickname is already registered. Try a different one.");			
		} catch (IOException e) {
			System.out.println("* There was an error registering the nickname");
		}
	}

	//Método que solicita al servidor de NanoChat la lista de salas e imprime el resultado obtenido
	private void getAndShowRooms() {
		// Lista que contendrá las descripciones de las salas existentes
		// Le pedimos al conector que obtenga la lista de salas ncConnector.getRooms()
		// Una vez recibidas iteramos sobre la lista para imprimir información de cada sala
		
		try {
			
			for(NCRoomDescription descripcion : ncConnector.getRooms()) {
	
				System.out.println(descripcion.toPrintableString());
			}
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Método para tramitar la solicitud de acceso del usuario a una sala concreta
	private void enterChat() {
		// Se solicita al servidor la entrada en la sala correspondiente ncConnector.enterRoom()
		// Si la respuesta es un rechazo entonces informamos al usuario y salimos
		// En caso contrario informamos que estamos dentro y seguimos
		// Cambiamos el estado del autómata para aceptar nuevos comandos
		try {
			if(ncConnector.enterRoom(room)) {
				System.out.println("Has entrado en la sala : "+room);
				clientStatus = IN_ROOM;
				do {
					//Pasamos a aceptar sólo los comandos que son válidos dentro de una sala
					
					readRoomCommandFromShell();
					processRoomCommand();
				} while (currentCommand != NCCommands.COM_EXIT);
				
			}else System.out.println("Error, no has podido entrar en la sala, posiblemente esa sala no exista. Pruebe de nuevo.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(currentCommand == NCCommands.COM_EXIT) System.out.println("* Your are out of the room");
		// Llegados a este punto el usuario ha querido salir de la sala, cambiamos el estado del autómata
		clientStatus = REGISTERED;
		}

	//Método para procesar los comandos específicos de una sala
	private void processRoomCommand() {
		switch (currentCommand) {
		case NCCommands.COM_ROOMINFO:
			//El usuario ha solicitado información sobre la sala y llamamos al método que la obtendrá
			getAndShowInfo();
			break;
		case NCCommands.COM_SEND:
			//El usuario quiere enviar un mensaje al chat de la sala
			sendChatMessage();
			break;
		case NCCommands.COM_DM:
			//El usuario quiere enviar un mensaje privado a un usuario de la sala
			sendDmChatMessage(userDst);
			break;
		case NCCommands.COM_RENAME:
			//El usuario quiere enviar un mensaje al chat de la sala
			renameRoom();
			break;
		case NCCommands.COM_SOCKET_IN:
			//En este caso lo que ha sucedido es que hemos recibido un mensaje desde la sala y hay que procesarlo
			processIncommingMessage();
			break;
		case NCCommands.COM_EXIT:
			//El usuario quiere salir de la sala
			exitTheRoom();
			break;
			
		}		
	}

	//Método para solicitar al servidor la información sobre una sala y para mostrarla por pantalla
	private void getAndShowInfo() {
		// Pedimos al servidor información sobre la sala en concreto
		// Mostramos por pantalla la información
		
		try {
			System.out.println(ncConnector.getRoomInfo(room).toPrintableString()); 
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Método para notificar al servidor que salimos de la sala
	private void exitTheRoom() {
		// Mandamos al servidor el mensaje de salida
		// Cambiamos el estado del autómata para indicar que estamos fuera de la sala
		try {
			ncConnector.leaveRoom(room);
			clientStatus = REGISTERED;
			
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	//Método para enviar un mensaje al chat de la sala
	private void sendChatMessage() {
		// Mandamos al servidor un mensaje de chat
		ncConnector.sendChatMessage(nickname,chatMessage);
		
	}
	private void sendDmChatMessage(String userDst) {
		// Mandamos al servidor un mensaje de chat
		ncConnector.sendDmMessage(nickname, userDst, chatMessage);
		
	}
	//Método para procesar los mensajes recibidos del servidor mientras que el shell estaba esperando un comando de usuario
	private void processIncommingMessage() {		
		// Recibir el mensaje
		NCMessage message = ncConnector.receiveChatMessage();
		

		String chatMessage = "";	
		// En función del tipo de mensaje, actuar en consecuencia
		switch (message.getOpcode()) {
		// (Ejemplo) En el caso de que fuera un mensaje de chat de broadcast devolvemos la información
			case NCMessage.OP_RECEIVED_MSG:
			{
				String nombre = ((NCTripleMessage) message).getName();
				String data = ((NCTripleMessage) message).getMessage();
				String fecha = ((NCTripleMessage) message).getDate();
				
				chatMessage = "("+fecha+")"+"["+nombre+"]: "+data;
				break;
			}
			case NCMessage.OP_DM_OK:
			{
				String userSrc = ((NCTripleMessage) message).getName();
				String data = ((NCTripleMessage) message).getMessage();
				String fecha = ((NCTripleMessage) message).getDate();
				
				chatMessage = "("+fecha+") Direct Message From : "+"["+userSrc+"]: "+data;
				break;
			}
			case NCMessage.OP_DM_ERROR:
			{
				chatMessage = "No existe el usuario al que estas enviando el mensaje";
				break;
			}
			case NCMessage.OP_RENAME_ROOM_OK:
			{
				String u = ((NCDoubleMessage) message).getName();
				String newRoomName = ((NCDoubleMessage) message).getMessage();
				chatMessage = "El usuario "+u+" ha cambiado el nombre de la sala a "+newRoomName;
				setRoom(newRoomName);
				break;
			}
			case NCMessage.OP_ROOM_NOTIFICATION:
			{							
				chatMessage = ((NCRoomMessage) message).getName();
			
			}
		}
		
		
		
		System.out.println(chatMessage);
		
		
		// (Ejemplo) En el caso de que fuera un mensaje de chat de broadcast mostramos la información de quién envía el mensaje y el mensaje en sí
	}
	
	
	
	
	private void renameRoom() {
		ncConnector.sendRenameRoom(nickname,newRoomName);
		System.out.println("Has cambiado el nombre de la sala a : "+newRoomName);
		room = newRoomName;
	}
	
	
	
	

	//MNétodo para leer un comando de la sala 
	public void readRoomCommandFromShell() {
		//Pedimos un nuevo comando de sala al shell (pasando el conector por si nos llega un mensaje entrante)
		shell.readChatCommand(ncConnector);
		//Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		//Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para leer un comando general (fuera de una sala)
	public void readGeneralCommandFromShell() {
		//Pedimos el comando al shell
		shell.readGeneralCommand();
		//Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		//Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para obtener el servidor de NanoChat que nos proporcione el directorio
	public boolean getServerFromDirectory(String directoryHostname) {
		//Inicializamos el conector con el directorio y el shell
		System.out.println("* Connecting to the directory...");
		//Intentamos obtener la dirección del servidor de NanoChat que trabaja con nuestro protocolo
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			serverAddress = directoryConnector.getServerForProtocol(PROTOCOL);
		} catch (IOException e1) {
			//  Auto-generated catch block
			serverAddress = null;
		}
		//Si no hemos recibido la dirección entonces nos quedan menos intentos
		if (serverAddress == null) {
			System.out.println("* Check your connection, the directory is not available.");		
			return false;
		}
		else return true;
	}
	
	//Método para establecer la conexión con el servidor de Chat (a través del NCConnector)
	public boolean connectToChatServer() {
			try {
				//Inicializamos el conector para intercambiar mensajes con el servidor de NanoChat (lo hace la clase NCConnector)
				ncConnector = new NCConnector(serverAddress);
			} catch (IOException e) {
				System.out.println("* Check your connection, the game server is not available.");
				serverAddress = null;
			}
			//Si la conexión se ha establecido con éxito informamos al usuario y cambiamos el estado del autómata
			if (serverAddress != null) {
				System.out.println("* Connected to "+serverAddress);
				clientStatus = PRE_REGISTRATION;
				return true;
			}
			else return false;
	}

	//Método que comprueba si el usuario ha introducido el comando para salir de la aplicación
	public boolean shouldQuit() {
		return currentCommand == NCCommands.COM_QUIT;
	}

	public void setRoom(String room) {
		this.room = room;
	}
}
