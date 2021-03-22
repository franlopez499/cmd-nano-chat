package es.um.redes.nanoChat.messageML;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;


public abstract class NCMessage {
	protected byte opcode;

	//  IMPLEMENTAR TODAS LAS CONSTANTES RELACIONADAS CON LOS CODIGOS DE OPERACION
	public static final byte OP_INVALID_CODE = 0;
	public static final byte OP_NICK = 1;
	public static final byte OP_NICK_OK = 2;
	public static final byte OP_NICK_DUPLICATED = 3;
	public static final byte OP_ROOMLIST = 4;
	public static final byte OP_RENAME_ROOM = 5;
	public static final byte OP_RENAME_ROOM_OK = 6;
	public static final byte OP_SEND_MSG = 8;
	public static final byte OP_DM = 9;
	public static final byte OP_DM_OK = 10;
	public static final byte OP_DM_ERROR = 11;
	public static final byte OP_EXIT_ROOM = 26;
	public static final byte OP_ENTER_ROOM = 27;
	public static final byte OP_ENTER_ROOM_OK = 28;
	public static final byte OP_ENTER_ROOM_ERROR = 29;
	public static final byte OP_ROOMLISTRES = 30;
	public static final byte OP_RECEIVED_MSG = 31;
	public static final byte OP_ROOM_INFO = 32;
	public static final byte OP_ROOM_INFO_RES = 33;
	public static final byte OP_ROOM_NOTIFICATION = 34;
	
	public static final char DELIMITER = ':';    //Define el delimitador
	public static final char END_LINE = '\n';    //Define el carácter de fin de línea
	
	public static final String OPERATION_MARK = "operation";
	public static final String MESSAGE_MARK = "message";

	/**
	 * Códigos de los opcodes válidos  El orden
	 * es importante para relacionarlos con la cadena
	 * que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = { 
			OP_NICK,
			OP_NICK_OK,
			OP_NICK_DUPLICATED,
			OP_ROOMLIST,
			OP_RENAME_ROOM,
			OP_RENAME_ROOM_OK,
			OP_SEND_MSG,
			OP_DM,
			OP_DM_OK,
			OP_DM_ERROR,
			OP_EXIT_ROOM,
			OP_ENTER_ROOM,
			OP_ENTER_ROOM_OK,
			OP_ENTER_ROOM_ERROR,
			OP_ROOMLISTRES,
			OP_RECEIVED_MSG,
			OP_ROOM_INFO,
			OP_ROOM_INFO_RES,
			OP_ROOM_NOTIFICATION
		};

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_opcodes_str = {
		"nick",
		"NickOK",
		"nickDuplicated",
		"roomlist",
		"renameRoom",
		"renameRoomOK",
		"sendMessage",
		"directMessage",
		"directMessageOK",
		"directMessageERROR",
		"exitRoom",
		"enterRoom",
		"enterRoomOK",
		"enterRoomERROR",
		"roomListResponse",
		"receivedMessage",
		"roomInfo",
		"roomInfoRes",
		"roomNotification"
	};

	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte stringToOpcode(String opStr) {
		//Busca entre los opcodes si es válido y devuelve su código
		for (int i = 0;	i < _valid_opcodes_str.length; i++) {
			if (_valid_opcodes_str[i].equalsIgnoreCase(opStr)) {
				return _valid_opcodes[i];
			}
		}
		//Si no se corresponde con ninguna cadena entonces devuelve el código de código no válido
		return OP_INVALID_CODE;
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToString(byte opcode) {
		//Busca entre los opcodes si es válido y devuelve su cadena
		for (int i = 0;	i < _valid_opcodes.length; i++) {
			if (_valid_opcodes[i] == opcode) {
				return _valid_opcodes_str[i];
			}
		}
		//Si no se corresponde con ningún opcode entonces devuelve null
		return null;
	}
	
	
	
	//Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;

	}

	//Método que debe ser implementado por cada subclase de NCMessage
	protected abstract String toEncodedString();

	//Analiza la operación de cada mensaje y usa el método readFromString() de cada subclase para parsear
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String regexpr = "<"+MESSAGE_MARK+">(.*?)</"+MESSAGE_MARK+">";
		Pattern pat = Pattern.compile(regexpr,Pattern.DOTALL);
		Matcher mat = pat.matcher(message);
		if (!mat.find()) {
			System.out.println("Mensaje mal formado:\n"+message);
			return null;
			// Message not found
		} 
		String inner_msg = mat.group(1);  // extraemos el mensaje

		String regexpr1 = "<"+OPERATION_MARK+">(.*?)</"+OPERATION_MARK+">";
		Pattern pat1 = Pattern.compile(regexpr1);
		Matcher mat1 = pat1.matcher(inner_msg);
		if (!mat1.find()) {
			System.out.println("Mensaje mal formado:\n" +message);
			return null;
			// Operation not found
		} 
		String operation = mat1.group(1);  // extraemos la operación
		
		byte code = stringToOpcode(operation);
		if (code == OP_INVALID_CODE) return null;
		
		switch (code) {
		// Parsear el resto de mensajes 
		case OP_NICK:
		case OP_ENTER_ROOM:
		case OP_ROOM_INFO:
		case OP_ROOM_NOTIFICATION:
		{
			return NCRoomMessage.readFromString(code, message);
		}	
		case OP_RECEIVED_MSG:
		case OP_DM:
		case OP_DM_OK:
		{
			return NCTripleMessage.readFromString(code, message);
		}
		
		case OP_SEND_MSG:
		case OP_RENAME_ROOM:
		case OP_RENAME_ROOM_OK:
		{
			return NCDoubleMessage.readFromString(code, message);
		}
		case OP_ROOMLISTRES:
		case OP_ROOM_INFO_RES:
		{
			return NCroomListMessage.readFromString(code, message);
		}
		
		case OP_NICK_DUPLICATED:
		case OP_NICK_OK:
		case OP_DM_ERROR:
		case OP_EXIT_ROOM:
		case OP_ENTER_ROOM_ERROR:
		case OP_ENTER_ROOM_OK:
		case OP_ROOMLIST:
		
		{
			
			return NCSimpleMessage.readFromString(code);
		}
			
		default:
			System.err.println("Unknown message type received:" + code);
			return null;
		}

	}

	// Programar el resto de métodos para crear otros tipos de mensajes
	
	public static NCMessage makeSimpleMessage(byte code) {
		return (new NCSimpleMessage(code));
	}
	
	public static NCMessage makeRoomListMessage(byte code,ArrayList<NCRoomDescription> rooms) {
		
		return (new NCroomListMessage(code, rooms));
	}
	
	public static NCMessage makeDoubleMessage(byte code,String name, String message) {
		return (new NCDoubleMessage(code, name, message));
	}
	public static NCMessage makeTripleMessage(byte code,String name,String date, String message) {
		return (new NCTripleMessage(code, name,date, message));
	}

	
	public static NCMessage makeRoomMessage(byte code, String room) {
		return (new NCRoomMessage(code, room));
	}
}
