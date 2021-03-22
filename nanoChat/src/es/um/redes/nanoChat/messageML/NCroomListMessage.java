package es.um.redes.nanoChat.messageML;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCroomListMessage extends NCMessage{
	private static final String RE_ROOM = "<rooms>(.*?)</rooms>";
	private static final String ROOM_MARK = "rooms";
	private static final String RE_TIME = "<time>(.*?)</time>";
	private static final String TIME_MARK = "time";
	private static final String RE_ROOMNAME = "<roomName>(.*?)</roomName>";
	private static final String ROOMNAME_MARK = "roomName";
	private static final String RE_MEMBER = "<member>(.*?)</member>";
	private static final String MEMBER_MARK = "member";
	private static final String RE_MEMBERS = "<members>(.*?)</members>";
	private static final String MEMBERS_MARK = "members";

	private ArrayList<NCRoomDescription> roomList; 

	public NCroomListMessage(byte opcode, ArrayList<NCRoomDescription> rooms) {
		this.opcode = opcode;
		this.roomList = rooms;
	}



	//Parseamos el mensaje contenido en message con el fin de obtener los distintos campos
	public static NCroomListMessage readFromString(byte code, String message) {
		
		Pattern pat_rooms = Pattern.compile(RE_ROOM);
		Matcher mat_rooms = pat_rooms.matcher(message);
		Pattern pat_member = Pattern.compile(RE_MEMBER);
		
		Pattern pat_members = Pattern.compile(RE_MEMBERS);
		Matcher mat_members = pat_members.matcher(message);
		Pattern pat_roomName = Pattern.compile(RE_ROOMNAME);
		Matcher mat_roomName = pat_roomName.matcher(message);
		Pattern pat_time = Pattern.compile(RE_TIME);
		Matcher mat_time = pat_time.matcher(message);
		ArrayList<NCRoomDescription> arrayRooms = new ArrayList<>();
		
		
		
		if(!mat_rooms.find()) {
			System.out.println("Error en roomList : no se ha encontrado parametro rooms.");
			return null;
			
		}
		
		
		Matcher mat_member;
		ArrayList<String> miembrosSala; 
		NCRoomDescription description;
		String roomName_found,time_found;
		while (mat_roomName.find() && mat_time.find() && mat_members.find()) {
			
			miembrosSala = new ArrayList<>();
			mat_member = pat_member.matcher(mat_members.group(1));
			roomName_found = mat_roomName.group(1);
			time_found = mat_time.group(1);
	
			while(mat_member.find()){
				miembrosSala.add(mat_member.group(1));
			}
			description = new NCRoomDescription(roomName_found, miembrosSala, Long.parseLong(time_found));
			arrayRooms.add(description);
			
			
			
		}
		
		
		//System.out.println("array : "+arrayRooms);

		return new NCroomListMessage(code, arrayRooms);
	}


	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();

		sb.append("<"+MESSAGE_MARK+">");
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"); //Construimos el campo
		sb.append("<"+ROOM_MARK+">");
		for(NCRoomDescription room : roomList) {
			sb.append("<"+ROOMNAME_MARK+">"+room.roomName+"</"+ROOMNAME_MARK+">");
			sb.append("<"+TIME_MARK+">"+room.timeLastMessage+"</"+TIME_MARK+">");
			sb.append("<"+MEMBERS_MARK+">");
			for(String member : room.members)
				sb.append("<"+MEMBER_MARK+">"+member+"</"+MEMBER_MARK+">");
			sb.append("</"+MEMBERS_MARK+">");
		}
		sb.append("</"+ROOM_MARK+">");
		sb.append("</"+MESSAGE_MARK+">");
		return sb.toString(); //Se obtiene el mensaje
	}
	
	public ArrayList<NCRoomDescription> getRoomList() {
		return roomList;
	}
	
}
