package es.um.redes.nanoChat.messageML;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NCTripleMessage extends NCMessage{
	private static final String RE_NAME = "<name>(.*?)</name>";
	private static final String NAME_MARK = "name";
	private static final String RE_DATE = "<date>(.*?)</date>";
	private static final String DATE_MARK = "date";
	private static final String RE_DATA = "<data>(.*?)</data>";
	private static final String DATA_MARK = "data";

	private String name;
	private String message;
	private String date;


	public NCTripleMessage(byte opcode, String name,String date, String message) {
		this.opcode = opcode;
		this.name = name;
		this.date = date;
		this.message = message;
	}




	//Parseamos el mensaje contenido en message con el fin de obtener los distintos campos
	public static NCTripleMessage readFromString(byte code, String message) {
		String found_name = null;
		String found_data = null;
		String found_date = null;
		// Tienen que estar los campos porque el mensaje es de tipo RoomMessage
		Pattern pat_name = Pattern.compile(RE_NAME);
		Matcher mat_name = pat_name.matcher(message);
		Pattern pat_data = Pattern.compile(RE_DATA);
		Matcher mat_data = pat_data.matcher(message);
		Pattern pat_date = Pattern.compile(RE_DATE);
		Matcher mat_date = pat_date.matcher(message);

		if (mat_name.find()) {
			// Name found
			found_name = mat_name.group(1);
		} else {
			System.out.println("Error en mensaje doble : no se ha encontrado parametro nombre.");
			return null;
		}

		if (mat_data.find()) {
			// Name found
			found_data = mat_data.group(1);
		} else {
			System.out.println("Error en mensaje doble : no se ha encontrado parametro data.");
			return null;
		}
		if (mat_date.find()) {
			// Name found
			found_date = mat_date.group(1);
		} else {
			System.out.println("Error en mensaje triple : no se ha encontrado parametro date.");
			return null;
		}

		
		return new NCTripleMessage(code, found_name,found_date, found_data);
	}






	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();

		sb.append("<"+MESSAGE_MARK+">");
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"); //Construimos el campo
		sb.append("<"+NAME_MARK+">"+name+"</"+NAME_MARK+">");
		sb.append("<"+DATE_MARK+">"+date+"</"+DATE_MARK+">");
		sb.append("<"+DATA_MARK+">"+message+"</"+DATA_MARK+">");
		sb.append("</"+MESSAGE_MARK+">");

		return sb.toString(); //Se obtiene el mensaje
	}




	public String getMessage() {
		return message;
	}
	public String getDate() {
		return date;
	}

	public String getName() {
		return name;
	}
}
