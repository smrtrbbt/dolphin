package visual.panels;

import java.awt.Component;
import java.awt.Dimension;

import data.containers.Message;
import visual.Client;
import visual.VisualConstants;
import visual.components.DLabel;
import visual.components.DPanel;
import visual.popups.MessagePopup;

public class MessagePanel extends DPanel {

	private static final long serialVersionUID = -5194642801471406001L;

	private Client client;
	
	private Message message;
	
	private DLabel lblSender;
	private DLabel lblContent;
	
	public MessagePanel(Client client, Message message) {
		super(VisualConstants.EPSILON_PANEL_COLOR);
		
		this.client = client;
		
		this.message = message;
		
		initializeComponents();
		
		update();
	}

	private void initializeComponents() {
		setLayout(null);
		
		Dimension messageDimension = new Dimension(550, 33);
		
		setMinimumSize(messageDimension);
		setPreferredSize(messageDimension);
		setMaximumSize(messageDimension);
		
		setAlignmentX(Component.CENTER_ALIGNMENT);
		
		setComponentPopupMenu(new MessagePopup(client, message));
		
		lblSender = new DLabel();
		lblSender.setAlignmentX(LEFT_ALIGNMENT);
		lblSender.setForeground(VisualConstants.ALPHA_FORE_COLOR);
		lblSender.setBounds(0, 0, 550, 15);
		
		lblContent = new DLabel();
		lblContent.setAlignmentX(LEFT_ALIGNMENT);
		lblContent.setForeground(VisualConstants.BETA_FORE_COLOR);
		lblContent.setBounds(0, 15, 550, 15);
		
		DPanel panSeparator = new DPanel();
		panSeparator.setBounds(0, 30, 550, 3);
		
		add(lblSender);
		add(lblContent);
		add(panSeparator);
	}
	
	public void update() {
		lblSender.setText(message.getSender().getUsername());
		lblContent.setText(message.getContent());
	}
	
	public Message getMessage() {
		return message;
	}
}