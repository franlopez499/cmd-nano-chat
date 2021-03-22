package es.um.redes.nanoChat.messageML;



/*
 * 
----

<message>
<operation>operation</operation>
</message>

Operaciones válidas:

Nick_OK

 */

public class NCSimpleMessage extends NCMessage {

	public NCSimpleMessage(byte code) {
		this.opcode = code;
	}



	//Parseamos el mensaje contenido en message con el fin de obtener los distintos campos
	public static NCSimpleMessage readFromString(byte code) {
		return new NCSimpleMessage(code);
	}

	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();

		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE); //Construimos el campo
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);

		return sb.toString(); //Se obtiene el mensaje
	}

}
