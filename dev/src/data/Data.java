package data;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import data.containers.User;
import general.exceptions.EmptyDataFileException;
import general.exceptions.InvalidUsernameException;
import data.containers.Chat;

public class Data implements Serializable {
	
	private static final long serialVersionUID = -3185108578513027310L;
	
	private User localUser;
	private ArrayList<User> users;
	private ArrayList<Chat> chats;
	
	public Data() { }
	
	public void init(String username) throws InvalidUsernameException {
		setLocalUser(new User(username));
		setUsers(new ArrayList<User>());
		setChats(new ArrayList<Chat>());
	}
	
	public void load(File dataFile) throws IOException, ClassNotFoundException, EmptyDataFileException {
		try (FileInputStream fis = new FileInputStream(dataFile); 
				ObjectInputStream ois = new ObjectInputStream(fis)) {
			Object obj = ois.readObject();
			
			if (obj == null)
				throw new EmptyDataFileException();
				
			Data aux = (Data) obj;
				
			setLocalUser(aux.getLocalUser());
			setUsers(aux.getUsers());
			setChats(aux.getChats());
		    
		    ois.close();
		    fis.close();
		}
	}
	
	public void dump(File dataFile) throws IOException {	
		FileOutputStream fos = new FileOutputStream(dataFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		oos.writeObject(this);
		
		oos.close();
		fos.close();
	}

	public User getLocalUser() {
		return this.localUser;
	}
	public void setLocalUser(User localUser) {
		if (this.localUser != null)
			return;
		
		this.localUser = localUser;
	}
	
	public ArrayList<User> getUsers() {
		return users;
	}
	public void setUsers(ArrayList<User> users) {
		this.users = users;
	}

	public ArrayList<Chat> getChats() {
		return this.chats;
	}
	public void setChats(ArrayList<Chat> chats) {
		if (this.chats != null)
			return;
		
		this.chats = chats;
	}
}