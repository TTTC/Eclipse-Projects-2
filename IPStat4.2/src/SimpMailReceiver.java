import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
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


public class SimpMailReceiver extends JFrame{
	JTextField srvField;
	JTextField userField;
	JTextField passwField;
	JTextArea msgArea;
	
	public SimpMailReceiver(){
		super("SimpMailSender");
		
		JLabel srvLabel = new JLabel("Mail server: ");
		JLabel userLabel = new JLabel("Username: ");
		JLabel passwLabel = new JLabel("Password: ");
		JLabel msgLabel = new JLabel("Messages: ");
		
		srvField = new JTextField();
		userField = new JTextField();
		passwField = new JTextField();
		msgArea = new JTextArea();
		msgArea.setEditable(false);
		
		JButton fetchButton = new JButton("Fetch");
		fetchButton.addActionListener(new SendButtonListener());
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = gbc.BOTH;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		this.add(srvLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
		this.add(srvField, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		this.add(userLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		this.add(userField, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		this.add(passwLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
		this.add(passwField, gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		this.add(msgLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = gbc.gridwidth = 1;
		this.add(new JScrollPane(msgArea), gbc);
		gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = gbc.LAST_LINE_END;
		gbc.fill = gbc.NONE;
		this.add(fetchButton, gbc);
				
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(640,400);
		this.setVisible(true);
		
	}
	public static void main(String[] args){
		new SimpMailReceiver();
	}
	public class SendButtonListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent e) {
			if(srvField.getText().trim().equals("")
			|| userField.getText().trim().equals("")
			|| passwField.getText().trim().equals(""))
				return;
			
			new Thread(this).start();
		}
		public void run() {
			/* Establish connection and technicalities of email fetching... */
			Properties prop = new Properties();
			
			Session session = Session.getDefaultInstance(prop, new CustomMailAuthenticator(userField.getText().trim(), passwField.getText().trim()));
			Store store = null;
			Folder inbox = null;
			Message[] emails = null;
			try {
				store = session.getStore("pop3");
				store.connect(srvField.getText().trim(), userField.getText().trim(), passwField.getText().trim());
				inbox = store.getFolder("INBOX");
				if(inbox == null){
					System.err.println("No inbox found.");
					return;
				}
				inbox.open(Folder.READ_ONLY);
				emails = inbox.getMessages();
				
				/* Write messages to JTextArea, separated by some dashes and an empty line in between. */
				for(int i = 0; i < emails.length; i++){
					msgArea.insert("---" + System.lineSeparator() + emails[i] + System.lineSeparator() + System.lineSeparator(), 0);
				}
				
				/* Let's end this... */
				inbox.close(false);
				store.close();
			} catch (NoSuchProviderException e) {
				System.err.println("Unable to get store from POP3 provider.");
				e.printStackTrace();
				System.exit(33);
			} catch (MessagingException e) {
				System.err.println("Unable to get emails from email server. Check given fields.");
				e.printStackTrace();
				return;
			}
		}
		
	}
	/**
	 * Orifinally borrowed from n002213f of StackOverflow.
	 * @author simon
	 *
	 */
	class CustomMailAuthenticator extends Authenticator {
	     String user;
	     String pw;
	     public CustomMailAuthenticator (String user, String pw)
	     {
	        super();
	        this.user = user;
	        this.pw = pw;
	     }
	    public PasswordAuthentication getPasswordAuthentication()
	    {
	       return new PasswordAuthentication(user, pw);
	    }
	}
}
