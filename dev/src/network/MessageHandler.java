package network;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import data.Data;
import data.containers.Chat;
import data.containers.Message;
import data.containers.User;
import general.exceptions.InvalidParameterException;
import general.exceptions.MessageNotSentException;
import network.netMsg.NetMsg;
import network.netMsg.messages.*;
import visual.Client;

public class MessageHandler implements Runnable {

	private Socket sender;
	
	private Client client;
	private Network network;
	private Data data;
	
	public MessageHandler(Socket sender, Client client, Network network, Data data) {
		this.sender = sender;
		
		this.client = client;
		this.network = network;
		this.data = data;
	}
	
	@Override
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(sender.getInputStream());
			Object msg = in.readObject();
			sender.close();
			
			handleMessage((NetMsg) msg);
		} catch(Exception e) {
	    	e.printStackTrace();
	    }
	}

	private void handleMessage(NetMsg msg) throws Exception {
		switch (msg.getMessageType()) {
			default:
			case NetMsg.MessageType.none:
				System.out.println("none message received");
				break;
			case NetMsg.MessageType.connect:
				System.out.println("connect message received");
				connectMsg((ConnectMsg) msg);
				break;
			case NetMsg.MessageType.onConnect:
				System.out.println("onconnect message received");
				onConnectMsg((OnConnectMsg) msg);
				break;
			case NetMsg.MessageType.disconnect:
				System.out.println("disconnect message received");
				disconnectMsg((DisconnectMsg) msg);
				break;
			case NetMsg.MessageType.statusUpdate:
				System.out.println("statusupdate message received");
				statusUpdateMsg((StatusUpdateMsg) msg);
				break;
			case NetMsg.MessageType.add:
				System.out.println("add message received");
				addMsg((AddMsg) msg);
				break;
			case NetMsg.MessageType.onAdd:
				System.out.println("onadd message received");
				onAddMsg((OnAddMsg) msg);
				break;
			case NetMsg.MessageType.includedOnChat:
				System.out.println("includedOnChat message received");
				includedOnChatMsg((IncludedOnChatMsg) msg);
				break;
			case NetMsg.MessageType.message:
				System.out.println("messageMsg message received");
				messageMsg((MessageMsg) msg);
				break;
		}
	}
	
	private boolean isMessageLegit(User sender, String msgToken) {
		if (sender == null)
			return false;
		
		if (sender.equals(data.getLocalUser()))
			return false;
		
		if (!sender.getToken().equals(msgToken))
			return false;
		
		return true;
	}
	
	// Handlers
	private void connectMsg(ConnectMsg msg) {
		User user = data.getUser(msg.getId());

		if (!isMessageLegit(user, msg.getToken()))
			return;
		
		try {
			user.setStatus(msg.getStatus());
			user.setAddress(msg.getAddress());
			user.setPort(msg.getPort());
			
			OnConnectMsg ocmsg = new OnConnectMsg();
			ocmsg.setStatus(data.getLocalUser().getStatus());

			network.sendMessage(user, ocmsg);
			
			network.sendUnsentMessages(user);
			
			if (!user.isHidden())
				client.updateUser(user);
		} catch (MessageNotSentException e) { }
	}
	
	private void onConnectMsg(OnConnectMsg msg) {
		User user = data.getUser(msg.getId());
		
		if (!isMessageLegit(user, msg.getToken()))
			return;

		user.setStatus(msg.getStatus());
		
		network.sendUnsentMessages(user);
		
		client.updateUser(user);
	}
	
	private void disconnectMsg(DisconnectMsg msg) {
		User user = data.getUser(msg.getId());
		
		if (!isMessageLegit(user, msg.getToken()))
			return;
		
		user.setStatus(User.Status.offline);
		
		client.updateUser(user);
	}
	
	private void statusUpdateMsg(StatusUpdateMsg msg) {		
		User user = data.getUser(msg.getId());
		
		if (user == null || !user.getToken().equals(msg.getToken()))
			return;
		
		user.setStatus(msg.getStatus());
		client.updateUser(user);
	}
	
	private void addMsg(AddMsg msg) {
		try {
			User newUser = new User();
			newUser.setId(msg.getId());
			newUser.setAddress(msg.getAddress());
			newUser.setPort(msg.getPort());
			newUser.setToken(msg.getToken());
			
			OnAddMsg oamsg = new OnAddMsg();
			
			if (newUser.equals(data.getLocalUser()))
				oamsg.setMsgStatus(OnAddMsg.Status.tryingToAddLocalUser);
			else if (data.getUsers().contains(newUser))
				oamsg.setMsgStatus(OnAddMsg.Status.userAlreadyAdded);
			else {
				newUser.setStatus(msg.getStatus());
				newUser.setUsername(msg.getUsername());
				
				data.getUsers().add(newUser);
				client.addUser(newUser);
				
				oamsg.setAddress(data.getLocalUser().getAddress());
				oamsg.setPort(data.getLocalUser().getPort());
				oamsg.setUsername(data.getLocalUser().getUsername());
				oamsg.setStatus(data.getLocalUser().getStatus());
				oamsg.setMsgStatus(OnAddMsg.Status.success);
			}
			
			try {
				network.sendMessage(newUser, oamsg);
			} catch (MessageNotSentException e) {
				newUser.getUnsentMessages().add(oamsg);
			}
		} catch (InvalidParameterException e) { }
	}
	
	private void onAddMsg(OnAddMsg msg) {
		User user = data.getUser(msg.getToken());
		
		if (!isMessageLegit(user, msg.getToken()))
			return;

		switch(msg.getMsgStatus()) {
			default:
			case OnAddMsg.Status.unknownError:
				if (user.getId() != null)
					break;
				
				data.getUsers().remove(user);
				client.removeUser(user);
				break;
			case OnAddMsg.Status.success:
				try {
					user.setId(msg.getId());
					user.setAddress(msg.getAddress());
					user.setPort(msg.getPort());
					user.setUsername(msg.getUsername());
					user.setStatus(msg.getStatus());
					
					client.updateUser(user);
				} catch (Exception e) {  }		
				break;
			case OnAddMsg.Status.tryingToAddLocalUser:
				data.getUsers().remove(user);
				client.removeUser(user);
				break;
			case OnAddMsg.Status.userAlreadyAdded:
				break;
		}
	}

	private void includedOnChatMsg(IncludedOnChatMsg msg) {
		User user = data.getUser(msg.getId());

		if (!isMessageLegit(user, msg.getToken()))
			return;

		try {
			Chat newChat = new Chat(msg.getName());
			newChat.setId(msg.getChatId());
			newChat.setStart(new Date(msg.getDate()));

			List<User> members = newChat.getMembers();
			
			for (int i = 0; i < msg.getMembersId().size(); i++) {
				String id = msg.getMembersId().get(i);
				
				if (id.equals(data.getLocalUser().getId())) {
					members.add(data.getLocalUser());
					continue;
				}
				
				User member = data.getUser(id);
				
				if (member == null) {
					String fullAddress = msg.getMembersAddress().get(i);
					
					String[] aux = fullAddress.split(":");
					String address = aux[0];
					int port = Integer.parseInt(aux[1]);
					
					member = new User();
					member.setId(id);
					member.setToken(newChat.getId());
					member.setAddress(address);
					member.setPort(port);
					member.setStatus(User.Status.unknown);
					member.setHidden(true);
					member.getChats().add(newChat);
					
					members.add(member);
					
					data.getUsers().add(member);
				} else {
					member.getChats().add(newChat);
					members.add(member);
				}
			}

			data.getChats().add(newChat);
			client.addChat(newChat);
		} catch (InvalidParameterException e) { }
	}
	
	private void messageMsg(MessageMsg msg) {
		User user = data.getUser(msg.getId());

		if (!isMessageLegit(user, msg.getToken()))
			return;
		
		Chat chat = data.getChat(msg.getChatId());

		if (chat == null)
			return;
		
		if (!chat.getMembers().contains(user))
			return;
		
		Message message = new Message();
		message.setChat(chat);
		message.setSender(user);
		message.setTime(new Date(msg.getTime()));
		message.setContent(msg.getContent());
		
		chat.getMessages().add(message);
		
		client.addMessage(message, chat);
	}
}
