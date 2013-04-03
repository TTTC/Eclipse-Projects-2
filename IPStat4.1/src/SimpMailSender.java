import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class SimpMailSender extends JFrame{
	JTextField srvField;
	JTextField fromField;
	JTextField toField;
	JTextField subjField;
	JTextArea msgArea;
	
	public SimpMailSender(){
		super("SimpMailSender");
		
		JLabel srvLabel = new JLabel("Mail server: ");
		JLabel fromLabel = new JLabel("From: ");
		JLabel toLabel = new JLabel("To: ");
		JLabel subjLabel = new JLabel("Subject: ");
		JLabel msgLabel = new JLabel("Message: ");
		
		srvField = new JTextField();
		fromField = new JTextField();
		toField = new JTextField();
		subjField = new JTextField();
		msgArea = new JTextArea();
		msgArea.setLineWrap(true);
		
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new SendButtonListener());
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = gbc.BOTH;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		this.add(srvLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
		this.add(srvField, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		this.add(fromLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		this.add(fromField, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		this.add(toLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
		this.add(toField, gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		this.add(subjLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1;
		this.add(subjField, gbc);
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
		this.add(msgLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = gbc.gridwidth = 1;
		this.add(new JScrollPane(msgArea), gbc);
		gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = gbc.LAST_LINE_END;
		gbc.fill = gbc.NONE;
		this.add(sendButton, gbc);
				
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(640,400);
		
		this.setVisible(true);
		
	}
	public static void main(String[] args){
		new SimpMailSender();
	}
	public class SendButtonListener implements ActionListener, Runnable{
		Message msg;
		public void actionPerformed(ActionEvent e) {
			if(srvField.getText().trim().equals("")
			|| toField.getText().trim().equals("")
			|| fromField.getText().trim().equals("")
			|| subjField.getText().trim().equals("")
			|| msgArea.getText().trim().equals(""))
				return;
			
			
			Properties props = new Properties();
			props.put("mail.host", srvField.getText().trim());
			Session mailConnection = Session.getInstance(props, null);
			msg = new MimeMessage(mailConnection);
			Address from, to;
			try {
				from = new InternetAddress(fromField.getText().trim());
				to = new InternetAddress(toField.getText().trim());
				msg.setFrom(from);
				msg.setRecipient(Message.RecipientType.TO, to);
				msg.setSubject(subjField.getText().trim());
				msg.setContent(msgArea.getText().trim(), "text/plain");
				new Thread(this).start();
			} catch (AddressException e1) {
				System.err.println("Error setting up sender or receiver addresses.");
				return;
			} catch (MessagingException e1) {
				System.err.println("Error creating message.");
				return;
			}
			
		}
		public void run() {
			try {
				Transport.send(msg);
			} catch (MessagingException e) {
				System.err.println("Error sending message." + e.getMessage());
				return;
			}
			System.exit(0); //Closing when sent.
		}
		
	}
}
