import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Class for posting comments in a guestbook-database. Posts them in the database located at atlas.dsv.su.se on the account of the author of this application. The user can supply at most 255 characters as a message, and 50 characters for the other fields. This application expects the drivers com.mysql.jdbc.Driver to be available.
 * @author simon
 *
 */
public class SimpGuestBook extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextField nameField;
	private JTextField emailField;
	private JTextField websiteField;
	private JTextArea messageArea;
	
	public static void main(String[] args){
		new SimpGuestBook();
	}
	/**
	 * Constructor. Sets up the GUI - fields, buttons and listeners.
	 */
	public SimpGuestBook(){
		super("SimpGuestBook");
		
		JLabel nameLabel = new JLabel("Name: ");
		JLabel emailLabel = new JLabel("Email: ");
		JLabel websiteLabel = new JLabel("Website: ");
		JLabel messageLabel = new JLabel("Message");
		
		nameField = new JTextField();
		emailField = new JTextField();
		websiteField = new JTextField();
		messageArea = new JTextArea();
		messageArea.setLineWrap(true);
		
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new SendButtonListener());
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		this.add(nameLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
		this.add(nameField, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		this.add(emailLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		this.add(emailField, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		this.add(websiteLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
		this.add(websiteField, gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		this.add(messageLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = gbc.gridwidth = 1;
		this.add(new JScrollPane(messageArea), gbc);
		gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.LAST_LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		this.add(sendButton, gbc);
				
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(640,400);
		
		this.setVisible(true);
	}
	/**
	 * Class for listening to the send-button of the {@link SimpGuestBook} application. When pressed, starts a new thread where the actual attempt to post the comment will be made. 
	 * @author simon
	 * @see #run()
	 */
	private class SendButtonListener implements ActionListener, Runnable {
		public void actionPerformed(ActionEvent e) {
			new Thread(this).start();
		}
		/**
		 * Attempts to upload a comment to the database. If the user has left a field empty, it will do nothing. If a user has supplied HTML-code in any field, that code will be replaced by <Censur>. Also, if the user has written a message longer than 255 characters, or text longer than 50 characters in the other fields, the method will do nothing. This method also expects that the required drivers, com.mysql.jdbc.Driver, are present.
		 */
		public void run(){
			String nameText = nameField.getText().trim();
			String emailText = emailField.getText().trim();
			String websiteText = websiteField.getText().trim();
			String messageText = messageArea.getText().trim();
			//Empty strings not allowed!
			if(nameText.equals("")
			|| emailText.equals("")
			|| websiteText.equals("")
			|| messageText.equals(""))
				return;
			//I was too lazy to make an elaborated way of handling maximum character limit.
			if(nameText.length() > 50
			|| emailText.length() > 50
			|| websiteText.length() > 50
			|| messageText.length() > 255)
				return;
			//Checks for HTML-code, not so thoroughly.
			Pattern p = Pattern.compile("<.*>");
			p.matcher(nameText).replaceAll("<Censur>");
			p.matcher(emailText).replaceAll("<Censur>");
			p.matcher(websiteText).replaceAll("<Censur>");
			p.matcher(messageText).replaceAll("<Censur>");
			
			/* Connect to database */
			Connection dbConnection = null;
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				dbConnection = DriverManager.getConnection("jdbc:mysql://atlas.dsv.su.se/db_12236731", "usr_12236731", "236731");				
			} catch (InstantiationException e) {
				System.err.println("Unable to load drivers in com.mysgl.jdbc.Driver. Closing...");
				System.exit(10);
			} catch (IllegalAccessException e) {
				System.err.println("Unable to load drivers in com.mysgl.jdbc.Driver. Closing...");
				System.exit(11);
			} catch (ClassNotFoundException e) {
				System.err.println("Unable to load drivers in com.mysgl.jdbc.Driver. Closing...");
				System.exit(12);
			} catch (SQLException e) {
				System.err.println("Problem connecting with database. Closing...");
				System.exit(13);
			}
			
			/* Check if table already exists. If not, create it */
			DatabaseMetaData dmd = null;
			Statement stmt = null;
			try {
				dmd = dbConnection.getMetaData();
				ResultSet rs = dmd.getTables(null, null, "GUESTBOOKCOMMENT", null);
				if(!rs.next()){
					String statementString = 	"CREATE TABLE GUESTBOOKCOMMENT " +
												"(id INTEGER NOT NULL AUTO_INCREMENT, " +
												" name VARCHAR(50), " + 
												" email VARCHAR(50), " + 
												" website VARCHAR(50), " + 
												" message VARCHAR(255), " +
												" PRIMARY KEY ( id ))";
					stmt = dbConnection.createStatement();
					stmt.execute(statementString);
				}
				
			} catch (SQLException e) {
				System.err.println("Problem interacting with database. Closing...");
				System.exit(14);
			}
			
			/* Add a row for the comment in the table */
			String statementString = "INSERT INTO GUESTBOOKCOMMENT (" +
									  "name, email, website, message) VALUES ('" +
									  nameText + "', '" + 
									  emailText + "', '" + 
									  websiteText + "', '" + 
									  messageText + "')";
			try {
				stmt = dbConnection.createStatement();
				stmt.execute(statementString);
			} catch (SQLException e) {
				System.err.println("Problem interacting with database. Closing...");
				e.printStackTrace();
				System.exit(15);
			}
			
			nameField.setText("");
			emailField.setText("");
			websiteField.setText("");
			messageArea.setText("");
			
			try {
				dbConnection.close();
			} catch (SQLException e) {
				System.err.println("Problem closing database connection. Do nothing...");
			}
		}
	}
}
