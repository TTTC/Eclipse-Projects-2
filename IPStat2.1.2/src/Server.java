import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Class for running a simple chat-server. Has a small GUI which prints messages about when a client connects, disconnects or sends a message which is to be broadcasted.
 * @author Simpn
 *
 */
public class Server extends JFrame implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String port;
	private ServerSocket serverSocket;
	
	private Set<ClientHandler> clients;
	
	private JTextArea textDisplayArea;
	
	private Server(String port) {
		super("Host: " + "..." + " Port: " + port + " Clients: " + "0"); // ... and 0 will change later
		this.port = port;
	}
	
	 /**
	  * Starts the server. Sets up the socket which clients will request connections to, sets up the GUI and then starts listening for reqeusts.
	  */
	private void startServer() {
		/*
		 * Establish a ServerSocket
		 */
		serverSocket = null;
		try {
			serverSocket = new ServerSocket(Integer.parseInt(port));
			clients = new HashSet<ClientHandler>();
			serverSocket.getInetAddress();
			this.setTitle("Host: " + InetAddress.getLocalHost().getHostName() + " Port: " + port + " Clients: " + clients.size());
		} 
		catch (UnknownHostException e) {
			System.err.println("Unable to create server socket");
			System.exit(11);
		}
		catch (IOException e) { 
			System.err.println("Unable to create server socket");
			System.exit(12);
		}
		catch (NumberFormatException e) {
			System.err.println("Given port is not a number");
			System.exit(13);
		}
		
		/*
		 * Set up the GUI
		 */
		textDisplayArea = new JTextArea(25,50);
		textDisplayArea.setEditable(false);
		textDisplayArea.setLineWrap(true);
		JScrollPane scrollPane = new JScrollPane(textDisplayArea);
		
		this.add(scrollPane);
		this.pack();
		this.setLocation(400,100);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new CloseWindowListener());
		this.setVisible(true);
		
		/*
		 * Things are ready - start accepting connections!
		 */
		new Thread(this).start();
	}
	
	synchronized private void removeClient(ClientHandler clientHandler) {
		clients.remove(clientHandler);
		try {
			serverSocket.getInetAddress();
			this.setTitle("Host: " + InetAddress.getLocalHost().getHostName() + " Port: " + port + " Clients: " + clients.size());
		} catch (UnknownHostException e) { 
			System.err.println("Problem getting host name");
			System.exit(22);
		}
	}
	
	/**
	 * Goes through every ClientHandler in the HashSet clientHandlers, and calls their {@link ClientHandler#sendMessage(String)}-method. Also prints the message and sender unto the text display. This message is called by a ClientHandler when it receives a message from a client.
	 * @param message the message receives from a client that is to be broadcasted
	 * @param sender the client that sent the message
	 */
	synchronized private void broadcastMessage(String message, String sender) {
			
		textDisplayArea.append(sender + ": "+ message + System.lineSeparator());
		textDisplayArea.setCaretPosition(textDisplayArea.getDocument().getLength());
		for(ClientHandler clientHandler : clients) {
			clientHandler.sendMessage(message);
		}
	}
	
	/**
	 * Continously listens for clients that want to connect to the server. Upon receiving a request, creates a new ClientHandler class which takes care of listening and sending between the client and host. This method also prints that a client has connected into the text display.
	 */
	public void run() {
		while(true) {
			try {
				Socket clientSocket = serverSocket.accept();
				ClientHandler clientHandler = new ClientHandler(clientSocket);
				clients.add(clientHandler);
				serverSocket.getInetAddress();
				this.setTitle("Host: " + InetAddress.getLocalHost().getHostName() + " Port: " + port + " Clients: " + clients.size());
				textDisplayArea.append("CLIENT CONNECTED: " + clientSocket.getInetAddress().getHostName() + System.lineSeparator());
				textDisplayArea.setCaretPosition(textDisplayArea.getDocument().getLength());
				new Thread(clientHandler).start();
			} catch (IOException e) {
			
			}
		}
	}
	/**
	 * Class for listening for and sending messages from or to each client.
	 * @author Simpn
	 *
	 */
	private class ClientHandler implements Runnable{
		private Socket clientSocket;
		private BufferedReader clin;
		private PrintWriter clout;
		
		/**
		 * Constructor. Sets up the socket this ClientHandler is supposed to handle, and the streams, and their encoding, involved.
		 * @param clientSocket the socket of the connection between this server and a client that is produced when accepting a request to connect to this server.
		 */
		private ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
			try {
				this.clout = new PrintWriter(new OutputStreamWriter(this.clientSocket.getOutputStream(), "ISO-8859-1"), true);
				this.clin = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			} catch (UnsupportedEncodingException e) {
			} catch (IOException e) {
			}
		}
		/**
		 * Properly closes the socket and its streams.
		 */
		private void closeConnection() {
			try {
				clout.close();
				clin.close();
				clientSocket.close();
			} catch (IOException e) { }
		}
		/**
		 * Sends a message to the client this class' socket is connected to.
		 * @param message
		 */
		private void sendMessage(String message) {
			clout.println(message);
		}
		
		/**
		 * Continously listens for messages to broadcast to all connected clients. If a client disconnects, the while-loop will be broken, or a thrown exception will be caught outside the while-loop, and the server will remove the corresponding connection. 
		 */
		public void run() {
			try {
				while(true) {
						String read;
						if((read = clin.readLine()) != null)			
							broadcastMessage(read, clientSocket.getInetAddress().getHostName());
						else
							break;		
				}
			} catch (IOException e) { }
			finally {
				try {
					textDisplayArea.append("CLIENT DISCONNECTED: " + clientSocket.getInetAddress().getHostName() + System.lineSeparator());
					textDisplayArea.setCaretPosition(textDisplayArea.getDocument().getLength());
					clout.close();
					clin.close();
					clientSocket.close();
					removeClient(this);
				} catch (IOException e) {}
			}	
		}
		
	}
	 /**
	  * Listening class for application window's closing button.
	  * @author Simpn
	  *
	  */
	private class CloseWindowListener implements WindowListener {
		public void windowActivated(WindowEvent arg0) {}
		public void windowClosed(WindowEvent arg0) {}
		/**
		 * Listens for when the user closes the window, and prepares to close all client connections. Sends a message that the server is closing right before closing the connection.
		 */
		public void windowClosing(WindowEvent arg0) {
			try {
				for(ClientHandler clientHandler : clients) {
					clientHandler.sendMessage("Server closing...");
					clientHandler.closeConnection();
				}
				
				if(serverSocket != null)
					serverSocket.close();
					
			} catch (UnsupportedEncodingException e) {
				System.err.println("Unable to close connection. Force-closing server anyway...");
				System.exit(13);
			} catch (IOException e) { 
				System.err.println("Unable to close connection. Force-closing server anyway...");
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
		Server srv = null;
		if(args.length == 0) 
			srv = new Server("2000");
		
		else if(args.length == 1) 
			srv = new Server(args[0]);
		
		else { //Too many arguments
			System.err.println("Too many arguments. Only supply <host>, or <host> AND <port>, or neither.");
			System.exit(5);
		}
		srv.startServer();
	}
}
