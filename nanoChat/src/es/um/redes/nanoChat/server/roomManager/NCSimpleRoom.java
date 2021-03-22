package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import es.um.redes.nanoChat.messageML.NCDoubleMessage;
import es.um.redes.nanoChat.messageML.NCMessage;

import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCSimpleMessage;
import es.um.redes.nanoChat.messageML.NCTripleMessage;

public class NCSimpleRoom extends NCRoomManager{

	public long timeLastMessage;
	public HashMap<String,Socket> mapaMiembros;

	
	public NCSimpleRoom(String roomName) {
		this.roomName = roomName;
		this.timeLastMessage = 0;
		this.mapaMiembros = new HashMap<>();

	}

	@Override
	public boolean registerUser(String u, Socket s) {
		if(!mapaMiembros.containsKey(u)) {
			mapaMiembros.put(u, s);
			return true;
		}else return false;


	}

	@Override
	public void broadcastMessage(String u, String message) throws IOException {
		DataOutputStream outstream;
		timeLastMessage = System.currentTimeMillis();
		for(Socket socket : mapaMiembros.values()) {
			outstream = new DataOutputStream(socket.getOutputStream());
			
			NCTripleMessage tmp = (NCTripleMessage) NCMessage.makeTripleMessage(NCMessage.OP_RECEIVED_MSG, u, new Date(timeLastMessage).toString().replaceAll("CEST ",""), message);
			outstream.writeUTF(tmp.toEncodedString());
		}
		
		
		
		
		

	}
	
	public void notificarMiembros(String u, String message) throws IOException {
		DataOutputStream outstream;
		for(String usuario : mapaMiembros.keySet()){
			Socket socket;
			if(!usuario.equals(u)){
				socket = mapaMiembros.get(usuario);
				outstream = new DataOutputStream(socket.getOutputStream());
				NCRoomMessage tmp = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ROOM_NOTIFICATION, message);
				outstream.writeUTF(tmp.toEncodedString());
			}
		}
	}
	
	public void broadcastMessageRename(String u, String newRoomName) throws IOException {
		DataOutputStream outstream;
		
		for(String usuario : mapaMiembros.keySet()){
			Socket socket;
			if(!usuario.equals(u)){
				socket = mapaMiembros.get(usuario);
				outstream = new DataOutputStream(socket.getOutputStream());
				NCDoubleMessage tmp = (NCDoubleMessage) NCMessage.makeDoubleMessage(NCMessage.OP_RENAME_ROOM_OK, u,newRoomName);
				outstream.writeUTF(tmp.toEncodedString());
			}
		}
		

	}
	
	
	
	
	public void personalMessage(String userSource, String userDst, String message) throws IOException {
		
		
		if(mapaMiembros.containsKey(userDst)) {
			timeLastMessage = System.currentTimeMillis();
			Socket socket = mapaMiembros.get(userDst);
			DataOutputStream outstream = new DataOutputStream(socket.getOutputStream());
			
			NCTripleMessage tmp = (NCTripleMessage) NCMessage.makeTripleMessage(NCMessage.OP_DM_OK, userSource, new Date(timeLastMessage).toString().replaceAll("CEST ",""), message);
			outstream.writeUTF(tmp.toEncodedString());
			
			
			
		}else {
			Socket socket = mapaMiembros.get(userSource);
			DataOutputStream outstream = new DataOutputStream(socket.getOutputStream());
			NCSimpleMessage tmp = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_DM_ERROR);
			outstream.writeUTF(tmp.toEncodedString());
		}
		
		
	}
	

	@Override
	public void removeUser(String u) {
		mapaMiembros.remove(u);
	}

	@Override
	public void setRoomName(String roomName) {
		this.roomName = roomName;

	}

	@Override
	public NCRoomDescription getDescription() {
		ArrayList<String> arrayUsers = new ArrayList<>();
		for(String user : mapaMiembros.keySet())
			arrayUsers.add(user);

		return new NCRoomDescription(roomName, arrayUsers, timeLastMessage);
	}

	@Override
	public int usersInRoom() {
		return mapaMiembros.keySet().size();
	}


}
