package network;

import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import data.Data;
import data.containers.User;
import general.Helper;
import general.exceptions.MessageNotSentException;
import general.exceptions.NetworkUnableToShutException;
import general.exceptions.NetworkUnableToStartException;
import network.netMsg.NetMsg;
import network.netMsg.messages.ConnectMsg;
import visual.Client;

public class Network {
	
	public static final int BUFFER_SIZE = 2048;
	
	private Client client;
	private Data data;
	private Network instance;
	
	public boolean running;
	
	private ServerSocket serverSocket;
	
	public Network(Client client, Data data) {
		this.client = client;
		this.data = data;
		
		instance = this;
	}
	
	public void start() throws NetworkUnableToStartException {
		if (running)
			return;
		
		try {
			serverSocket = new ServerSocket(0);
			
			String address = Inet4Address.getLocalHost().getHostAddress();
			int port = serverSocket.getLocalPort();
			
			User localUser = data.getLocalUser();
			
			localUser.setAddress(address);
			localUser.setPort(port);
			
			if (localUser.getId() == null)
				localUser.setId(Helper.generateId(localUser.getFullAddress()));

			running = true;

			updateMessagePump();

			ConnectMsg cmsg = new ConnectMsg();
			cmsg.setStatus(localUser.getStatus());
			cmsg.setAddress(address);
			cmsg.setPort(port);
			
			for (User user : data.getAddedUsers())
				user.setStatus(User.STATUS.LOADING);
			for (User user : data.getKnownUsers())
				user.setStatus(User.STATUS.LOADING);
			
			spreadMessage(data.getAddedUsers(), cmsg, false);
			spreadMessage(data.getKnownUsers(), cmsg, false);
			
			System.out.println(String.format("> network running on port %d\n", port));
			System.out.flush();
		} catch (Exception e) {
			throw new NetworkUnableToStartException(e);
		}
	}
	
	public void shut() throws NetworkUnableToShutException {
		if (!running)
			return; 
		
		try {
			running = false;
			
			serverSocket.close();
			
			System.out.println("> network no longer running");
		} catch (Exception e) {
			throw new NetworkUnableToShutException(e);
		}
	}
	
	public void sendMessage(User user, NetMsg msg) throws MessageNotSentException  {
		if (user.equals(data.getLocalUser()))
			throw new MessageNotSentException("receiver is local user");
		if (user.getStatus() == User.STATUS.OFFLINE)
			throw new MessageNotSentException("offline receiver");
		
    	try {
    		Socket socket = new Socket(user.getAddress(), user.getPort());

			msg.setId(data.getLocalUser().getId());
			msg.setToken(user.getToken());
			
			byte[] buffer = Helper.encodeMessage(msg, user.getPublicKey());
			
			socket.getOutputStream().write(buffer);
			
			socket.close();
    	} catch (Exception e) {
    		user.setStatus(User.STATUS.UNKNOWN);
    		throw new MessageNotSentException(e.getMessage());
    	}
	}
	
	public void spreadMessage(List<User> users, NetMsg msg, boolean store) {
		new Thread(new Runnable() {
		    @Override
		    public void run() {
		    	for (User user : users)
			    	try {
						sendMessage(user, msg);
					} catch (MessageNotSentException e) { 
						if (store)
							user.getUnsentMessages().add(msg);
					}
		    }
		}).start();
	}
	
	public void sendUnsentMessages(User user) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<NetMsg> unsent = new ArrayList<NetMsg>();
				
				for (NetMsg msg : user.getUnsentMessages())
					try {
						sendMessage(user, msg);
					} catch (MessageNotSentException e) {
						unsent.add(msg);
					}
				
				user.setUnsentMessages(unsent);			
			}
		}).start();
	}
	
	private void updateMessagePump() {
		new Thread (new Runnable() {
			@Override
			public void run() {
				while(running) {
					try {
						Socket socket = serverSocket.accept();
						Runnable handler = new MessageHandler(socket, client, instance, data);
						new Thread(handler).start();
					} catch (Exception e) { }
				}
			}
		}).start();
	}
}