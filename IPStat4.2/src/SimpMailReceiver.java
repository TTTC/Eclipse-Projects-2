import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Class for receiving emails. Follows the POP3 protocol, or rather uses the JavaMail with POP3 properties.
 * @author simon
 *
 */
public class SimpMailReceiver extends JFrame{
	private static final long serialVersionUID = 1L;
	private JTextField srvField;
	private JTextField userField;
	private JTextField passwField;
	private JTextArea msgArea;
	/**
	 * Constructor. Sets up the GUI.
	 */
	public SimpMailReceiver(){
		super("SimpMailSender");
		
		JLabel srvLabel = new JLabel("Mail server: ");
		JLabel userLabel = new JLabel("Username: ");
		JLabel passwLabel = new JLabel("Password: ");
		JLabel msgLabel = new JLabel("Messages: ");
		
		this.srvField = new JTextField();
		this.userField = new JTextField();
		this.passwField = new JTextField();
		this.msgArea = new JTextArea();
		this.msgArea.setEditable(false);
		
		JButton fetchButton = new JButton("Fetch");
		fetchButton.addActionListener(new SendButtonListener());
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = GridBagConstraints.BOTH;
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
		gbc.anchor = GridBagConstraints.LAST_LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		this.add(fetchButton, gbc);
				
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(640,400);
		this.setVisible(true);
		
	}
	public static void main(String[] args){
		new SimpMailReceiver();
	}
	/**
	 * Class for listeing on the send-button in the {@link SimpMailReceiver}-application. When the button is pressed, it starts a new thread wherein the emails are retreived.
	 * @author simon
	 *
	 */
	public class SendButtonListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent e) {
			if(srvField.getText().trim().equals("")
			|| userField.getText().trim().equals("")
			|| passwField.getText().trim().equals(""))
				return;
			
			new Thread(this).start();
		}
		/**
		 * Called when the send-button is pressed. Attempts to retreive the emails on the mail-server supplied by the user, on the account supplied by the user. If the information given leads to no emails, or if the user has not supplied any information, nothing will be presented. Follows POP3 protocl. Retreival will get quite slow, because of technicalities regarding the subjects of the emails, where the application will have to retreive them again and again for each email, I couldn't work out a nice way around this. This method runs in its own thread.
		 */
		public void run() {
			String srv = srvField.getText().trim();
			String user = userField.getText().trim();
			String passw = passwField.getText().trim();
			
			/* Establish connection and technicalities of email fetching... */
			Properties prop = new Properties();
			prop.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	        prop.setProperty("mail.pop3.socketFactory.fallback", "false");
	        prop.setProperty("mail.pop3.port",  "995");
	        prop.setProperty("mail.pop3.socketFactory.port", "995");
			
			Session session = Session.getDefaultInstance(prop, new CustomMailAuthenticator(user, passw));
			Store store = null;
			Folder inbox = null;
			Message[] emails = null;
			try {
				store = session.getStore("pop3");
				store.connect(srv, user, passw);
				inbox = store.getFolder("INBOX");
				if(inbox == null){
					System.err.println("No inbox found.");
					return;
				}
				inbox.open(Folder.READ_ONLY);
				emails = inbox.getMessages();
				
				/* Write messages to JTextArea, separated by some dashes and an empty line in between. */
				for(int i = 0; i < emails.length; i++){
					String emailText = "";
					try {
						emailText += "--- Message " + (i+1) + " ---" + System.lineSeparator();
						emailText += "From: " + InternetAddress.toString(emails[i].getFrom()) + System.lineSeparator();
						emailText += "Subject: " + emails[i].getSubject() + System.lineSeparator();
						emailText += "Content: " + emails[i].getContent() + System.lineSeparator() + System.lineSeparator();
					} catch (IOException e) {
						emailText += "Content: Problem retreiving email." + System.lineSeparator() + System.lineSeparator();
					}
					msgArea.insert(emailText, 0);
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
	 * Originally borrowed from n002213f of StackOverflow. A custom class for authentication. Used when retreiving the emails as part of the authentication.
	 * @author simon
	 *
	 */
	class CustomMailAuthenticator extends Authenticator {
	     String user;
	     String passw;
	     public CustomMailAuthenticator (String user, String passw)
	     {
	        super();
	        this.user = user;
	        this.passw = passw;
	     }
	    public PasswordAuthentication getPasswordAuthentication()
	    {
	       return new PasswordAuthentication(user, passw);
	    }
	}
}
