import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A client that allows users to chat with chat-servers using StreamSockets. Has a quite minimalist GUI - a text area for the chat, a text area to handle input form the user, and a send-button.
 * @author Simpn
 *
 */
public class Client extends JFrame implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String host;
	private String port;
	private Socket chatSocket;
	private BufferedReader in;
	private PrintWriter out;
	
	private JPanel panel;
	private JTextArea textDisplayArea;
	private JTextArea enterTextArea;
	private JButton sendButton; 
	
	private Client(String host, String port) {
		super("Host: " + host + " Port: " + port);
		this.host = host;
		this.port = port;
	}
	
	/**
	 * Establishes the client. Starts a connection with the host and initiates the output- and input-streams, sets up a text area to show chat messages, a text area for the user to write in, as well as a button for the user to send messages with. Lastly, starts the loop which reads messages from the host.
	 */
	private void startClient() {
		/*
		 * Establish connection first
		 */
		chatSocket = null;
		try {
			chatSocket = new Socket(host, Integer.parseInt(port));
			out = new PrintWriter(new OutputStreamWriter(chatSocket.getOutputStream(), "ISO-8859-1"), true);
			in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
		} 
		catch (UnknownHostException e) {
			System.err.println("Unable to connect to host");
			System.exit(11);
		}
		catch (IOException e) { 
			System.err.println("Unable to connect to host");
			System.exit(12);
		}
		catch (NumberFormatException e) {
			System.err.println("Given port is not a number");
			System.exit(13);
		}
		
		/*
		 * Set up the GUI
		 */
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		textDisplayArea = new JTextArea(10,50);
		textDisplayArea.setEditable(false);
		textDisplayArea.setLineWrap(true);
		JScrollPane displayScrollPane = new JScrollPane(textDisplayArea);
		
		enterTextArea = new JTextArea(5,40);
		enterTextArea.setEditable(true);
		enterTextArea.setLineWrap(true);
		JScrollPane enterScrollPane = new JScrollPane(enterTextArea);
		
		sendButton = new JButton("Send");
		sendButton.addActionListener(new SendButtonListener());
		
		GridBagConstraints gblc = new GridBagConstraints();
		gblc.gridheight = 1;
		gblc.gridwidth = 1;
		gblc.gridx = 0;
		gblc.gridy = 0;
		gblc.fill = GridBagConstraints.HORIZONTAL;
		gblc.weightx = 1.0;
		gblc.insets = new Insets(5,5,5,5);
		panel.add(enterScrollPane, gblc);
		gblc.gridwidth = 1;
		gblc.gridx = 1;
		gblc.gridy = 0;
		gblc.weightx = 0.0;
		gblc.insets = new Insets(0,0,0,0);
		sendButton.setAlignmentY(BOTTOM_ALIGNMENT);
		panel.add(sendButton, gblc);
		
		this.add(displayScrollPane, BorderLayout.CENTER);
		this.add(panel, BorderLayout.PAGE_END);
		this.pack();
		this.setLocation(400,100);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new CloseWindowListener());
		this.setVisible(true);
		
		/*
		 * Things are ready - enable reading!
		 */
		new Thread(this).start();
	}
	
	/**
	 * Continously reads if there is any message sent from the host to the socket by reading the client's socket's input-stream, and adding it to the chat text area. If there was nothing to read, waits 25 ms before looking again.
	 */
	public void run() {
		try {
			while(true) {
					String read;
					if((read = in.readLine()) != null) {			
						textDisplayArea.append(read + System.lineSeparator());
						textDisplayArea.setCaretPosition(textDisplayArea.getDocument().getLength());
					}
					else {
						System.out.println("Server disconnected! Closing client...");
						this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)); //Basically makes the code close the window as if the user did it.
					}
			}
		} catch (IOException e) { }
	}
	
	/**
	 * Class that listens for when the Send-button in the client GUI is clicked. Sends the users message to the socket output-stream when this occurs.
	 * @author Simpn
	 *
	 */
	private class SendButtonListener implements ActionListener {
		/**
		 * This method is called when the user clicks the send-button. Reads what is written in the text-area that the user can write in. If the user has written something it will send it to the sockets output-stream. Trailing white space will be removed, and if the user wrote only whitespaces, it will be ignored.
		 */
		public void actionPerformed(ActionEvent ae){
			String message = enterTextArea.getText().trim();
			if(!message.equals("")) {
				out.println(message);
				enterTextArea.setText("");
			}
		}
	}
	/**
	 * Class that specifically listens for when the user closes the window, at which point the client is supposed to terminate - closing the in and out streams, as well as the socket.
	 * @author Simpn
	 *
	 */
	private class CloseWindowListener implements WindowListener {
		public void windowActivated(WindowEvent arg0) {}
		public void windowClosed(WindowEvent arg0) {}
		/**
		 * Called when the user exits the window, terminates the client by closing the in and out stream and the socket connection, and then closing the program. The only thing this class is supposed to handle.
		 */
		public void windowClosing(WindowEvent arg0) {
			try {
				if(chatSocket != null)
					out.close();
					in.close();
					chatSocket.close();
			} catch (UnsupportedEncodingException e) {
				System.err.println("Unable to close connection. Force-closing client anyway...");
				System.exit(13);
			} catch (IOException e) { 
				System.err.println("Unable to close connection. Force-closing client anyway...");
				System.exit(14);
			}
			finally {
				System.exit(0);
			}
		}
		public void windowDeactivated(WindowEvent arg0) {}
		public void windowDeiconified(WindowEvent arg0) {}
		public void windowIconified(WindowEvent arg0) {}
		public void windowOpened(WindowEvent arg0) {}
		
	}
	
	public static void main(String[] args) {
		Client cl = null;
		if(args.length == 0) 
			cl = new Client("127.0.0.1", "2000");
		
		else if(args.length == 1) 
			cl = new Client(args[0], "2000");
		
		else if(args.length == 2) 
			cl = new Client(args[0], args[1]);
		
		else { //Too many arguments
			System.err.println("Too many arguments. Only supply <host>, or <host> AND <port>, or neither.");
			System.exit(5);
		}
		cl.startClient();
	}
}
