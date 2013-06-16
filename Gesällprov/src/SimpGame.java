import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The game client of the SimpGame. Handles most things on the client side of the SimpGame. It uses the class {@link SimpGameWindow} to render the game and take care of some game mechanics. The actual communication with the game-server is done through this client class. The main-method in this class will present a window where the user can specify a name to be had when playing, as well as which hostname and port to connect to.
 * @author simon
 *
 */
public class SimpGame extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;//Default auto-generated
	
	private String playerName;
	
	private String host;
	private int port;
	
	private JTextField nameField;
	private JTextField hostField;
	private JTextField portField;
	
	private JButton connectButton;
	private JButton disconnectButton;
	private JButton aboutButton;
	
	private DatagramSocket socket;
	
	private SimpGameWindow gameWindow;
	
	private boolean isRunning;
	
	/**
	 * Constructor for this class. Sets up the GUI, sets up button-listeners. It does not start the game though, which is done when the users presses the connect-button (assuming a valid server has been specified).
	 */
	public SimpGame(){
		/* Set up GUI */
		super("SimpGame");
		
		JLabel nameLabel = new JLabel("Player name: ");
		JLabel hostLabel = new JLabel("Host: ");
		JLabel portLabel = new JLabel("Port: ");
		
		this.playerName = "Player"; //Default name
		this.nameField = new JTextField("Player");
		this.hostField = new JTextField();
		this.portField = new JTextField();
		
		this.connectButton = new JButton("Connect");
		this.disconnectButton = new JButton("Disconnect");
		this.disconnectButton.setEnabled(false);
		this.aboutButton = new JButton("About");
		
		this.connectButton.addActionListener(new StartButtonListener(this));
		this.disconnectButton.addActionListener(new StopButtonListener(this));
		this.aboutButton.addActionListener(new AboutButtonListener(this));
		
		this.setLayout(new BorderLayout());
		
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		content.add(nameLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
		content.add(nameField, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		content.add(hostLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		content.add(hostField, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		content.add(portLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
		content.add(portField, gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		content.add(connectButton, gbc);		
		gbc.insets = new Insets(0,100,0,0);
		content.add(disconnectButton, gbc);
		gbc.insets = new Insets(0,300,0,0);
		content.add(aboutButton, gbc);
		
		//Just an empty game...
		this.gameWindow = new SimpGameWindow();			
		this.add(gameWindow, BorderLayout.CENTER);
		this.gameWindow.paintComponents(this.getGraphics());
		this.revalidate();
		this.repaint();
		this.pack();
		this.paintComponents(this.getGraphics());
		
		this.add(content, BorderLayout.NORTH);
		this.pack();//this.setSize(new Dimension(400, 300));
		this.setVisible(true);
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if(isRunning){
					sendMessage("D " + gameWindow.getPlayerNumber());
					demolishGame();
				}
				System.exit(0);
			}
		});
	}
	public String getPlayerName() {
		return this.playerName;
	}
	public String getHost(){
		return this.host;
	}
	public int getPort(){
		return this.port;
	}
	/**
	 * Sends a message to the server this client is connected to, or wish to connect to. In other words, to the hostname and port specified by the user. This method is for example used whenever the client needs to send data about movement, attacks, etc., from this users actions.
	 * @param message
	 */
	public void sendMessage(String message){
		try {
			byte[] messageData = message.getBytes("UTF-8");
			DatagramPacket packet = new DatagramPacket(messageData, messageData.length, InetAddress.getByName(this.host), this.port);
			socket.send(packet);	
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unable to encode with UTF-8");
			e.printStackTrace();
			System.exit(777);
		} catch (IOException e) {
			//Figure out better handling of these exceptions... later.
			e.printStackTrace();
			System.exit(778);
		}
	}
	/**
	 * Called as a new thread when the client has connected to a server. It's purpose is to continually listen for messages from the server, and then implement them into the game. These messages may be things such as attacks, movement, or the perhaps information about the server disconnecting.
	 */
	public void run(){
		while(true){
			byte[] buffer = new byte[65507];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			String msg = null;
			try {
				socket.receive(packet);
				msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
			} catch (SocketException se) {
				if(isRunning)
					gameWindow.printToLog("Unexpected SocketException: " + se.getMessage());
				else
					return;
			} catch (IOException e1) {
				/*
				 * I couldn't quite understand or figure out why this exception
				 * would be thrown, practically. So I'll just have the game quit
				 * for now. I assume in theory it's because it couldn't encode to UTF-8.
				 */
				
				System.exit(33);
				e1.printStackTrace();
			}
			/*
			 * Move:		"M player direction" direction = 0(stop) | 1(forward) | 2(backward)
			 * Rotate:		"R player direction" direction = 0(stop) | 1(left) | 2(right)
			 * If Move and Rotate are with 0 (stop moving or stop rotating), they should
			 * also contain information about the current x,y-coords and angle. This is 
			 * to avoid out-of-sync-problems.
			 * Attack:		"A player weapon info" weapon = 0-9, info = x, y, angle, etc, depending on weapon
			 * New entity:	"N entity-info"
			 * Chat msg:	"CM*playername*message"
			 */
			if(msg.matches("((M|R) \\d+ (1|2|0 -?\\d+.\\d+ -?\\d+.\\d+ -?\\d+))|(A .*)|(N .+)|(D \\d+)|(H \\d+ \\d+)|(RS \\d+ -?\\d+ -?\\d+ -?\\d+)|(CM\\*.+\\*.+)")){
				gameWindow.applyCommand(msg);
			}
			else if(msg.matches("DD")){
				//Server shut down...
				gameWindow.printToLog("Server shut down. Ending gameplay.");
				this.demolishGame();
			}
		}
	}
	/**
	 * Called by the connect-button-listener {@link StartButtonListener#actionPerformed(ActionEvent)} when the user has pressed the connect button. This method attempts to connect to a server, based upon the user's input. It also sets up the playing field and allows the user to play. If the user has given no or bad input (such as letters where only numbers are allowed), a dialog box will appear to tell the user that bad input has been given.
	 * @see #demolishGame()
	 */
	public void establishGame(){
		/* Consider moving the connection portion to the ButtonListener,
		 * and only have client game-specific things in this method.
		 * */
		String name = nameField.getText().trim();
		String host = hostField.getText().trim();
		String port = portField.getText().trim();
		
		//Check for bad input
		if(name.equals("")
		|| host.equals("")
		|| port.equals("")){
			JOptionPane.showMessageDialog(this, "Name, Host or Port may not be empty.", "Unable to connect", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(name.contains("*")){
			JOptionPane.showMessageDialog(this, "Name may not contain asterix (*).", "Unable to connect", JOptionPane.ERROR_MESSAGE);
			return;//Asterix (*) not allowed, as it is used as a separator for messages.
		}
		try{
			Integer.parseInt(port);
		} catch(IllegalArgumentException iae){
			JOptionPane.showMessageDialog(this, "Port may only contain numbers.", "Unable to connect", JOptionPane.ERROR_MESSAGE);
			return;
		}
		//Input is ok
		
		nameField.setEnabled(false);
		hostField.setEnabled(false);
		portField.setEnabled(false);
		connectButton.setEnabled(false);
		
		this.host = host;
		this.port = Integer.parseInt(port);
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e) {
			//Leave it like this for now.
			System.err.println("Problems creating socket.");
			e.printStackTrace();
		}
		sendMessage("C " + name);
		
		byte[] buffer = new byte[65507];
		DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
		String receivedMessage = null;
		try {
			socket.receive(receivePacket);
			receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");	
		} catch (IOException e1) {
			/*
			 * I couldn't quite understand or figure out why this exception
			 * would be thrown, practically (possibly by not being formatted 
			 * with UTF-8, but I will assume the game is formatting by UTF-8).
			 * So I'll just have the game quit for now. 
			 */
			
			System.exit(33);
			e1.printStackTrace();
		}
		if(receivedMessage.matches("D \\d*")){
			//Connection was denied
			this.host = null;
			this.port = -1;
			this.socket.close();
			this.socket = null;
			switch(receivedMessage.split(" ")[1]){
				case "10":
					JOptionPane.showMessageDialog(this, "Server denied conection. Reason: Server full (10)", "Unable to connect", JOptionPane.ERROR_MESSAGE);
					break;
			}
		}
		else if(receivedMessage.matches("A\\*.*")){
			//Connection was accepted
			
			String[] gameInfo = receivedMessage.split("\\*");
			/*
			 * gameInfo[0] = A
			 * gameInfo[1] = server name
			 * gameInfo[2] = Max number of players
			 * gameInfo[3] = This player's number
			 * gameInfo[4] = Info about players
			 * */
			//serverInfo = "A*server*name*maxp" maxp is unused for now, but I anticipate use for it in the future
			this.setTitle(gameInfo[1] + " (" + this.host + ":" + this.port + ")");
			this.remove(gameWindow);
			this.gameWindow = new SimpGameWindow(Integer.parseInt(gameInfo[3]));
			this.add(gameWindow, BorderLayout.CENTER);
			
			this.gameWindow.paintComponents(this.getGraphics());
			this.revalidate();
			this.repaint();
			this.pack();
			this.paintComponents(this.getGraphics());
			
			//Add player tanks
			if(gameInfo.length > 4){
				String[] playerInfo = gameInfo[4].split("P");
				//playerInfo "player entity-name x y angle hp movement rotatement" for each player
				for(int i = 1; i < playerInfo.length; i++){ 
					String pnumber = playerInfo[i].substring(0, playerInfo[i].indexOf(" "));
					String player = playerInfo[i].substring(playerInfo[i].indexOf(" ")+1, playerInfo[i].length());
					gameWindow.addPlayerEntity(pnumber, player);
				}
			}
			//Add playerless entities (items, projectiles, mines, etc...)
			if(gameInfo.length > 5){
				String[] entityInfo = gameInfo[5].split("E");
				//playerInfo "player entity-name x y angle hp movement rotatement" for each player
				for(int i = 1; i < entityInfo.length; i++){
					gameWindow.addEntity(entityInfo[i]);
				}
			}
			this.gameWindow.printToLog("Connected to server " + gameInfo[1] + "(" + host + ":" + port + ")");
			
			/*
			 * Create a buffer for the rendering to avoid flickering. 
			 * Has to be done when the playableArea is already visible.
			 * But also be done before run-method of the window is 
			 * called, to avoid NullPointerException because the 
			 * strategy did not exist yet, and the run-method tried to 
			 * call its graphics.
			 */
			this.gameWindow.playableAreaBuffer(2);
			
			this.isRunning = true;
			new Thread(this.gameWindow).start();
			new Thread(this).start(); //Start listening
		}
		disconnectButton.setEnabled(true);
	}
	/**
	 * Called by the stop-button-listeners {@link StopButtonListener#actionPerformed(ActionEvent)}-method when the user presses the disconnect-button. This is somewhat of an opposite of the {@link #establishGame()}-method. But instead of conecting and constructing the game, this method stops the game, removing the playing field, and ends the listening for messages from the server. 
	 * @see #establishGame()
	 */
	public void demolishGame(){
		this.isRunning = false;
		this.gameWindow.stopRunning();
		//
		this.socket.close();
	
		//this.host = "";
		//this.port = -1;
		this.setTitle("SimpGame");
		this.gameWindow.printToLog("Disconnected.");
		
		this.gameWindow = new SimpGameWindow();			
		this.add(gameWindow, BorderLayout.CENTER);
		this.gameWindow.paintComponents(this.getGraphics());
		this.revalidate();
		this.repaint();
		this.pack();
		this.paintComponents(this.getGraphics());
		
		this.connectButton.setEnabled(true);
		this.nameField.setEnabled(true);
		this.hostField.setEnabled(true);
		this.portField.setEnabled(true);
		this.disconnectButton.setEnabled(false);
	}
	
	/**
	 * Simple listening class. Handles the listening of the connect-button, which the user can press to connect to a server, if not already connected.
	 * @author simon
	 * @see SimpGame#establishGame()
	 *
	 */
	public class StartButtonListener implements ActionListener{
		SimpGame sg;
		public StartButtonListener(SimpGame sg){
			this.sg = sg;
		}
		
		public void actionPerformed(ActionEvent e) {
			sg.establishGame();
		}	
	}
	
	/**
	 * Simple listening class. Handles the listening of the disconnect-button, which the user can press to disconnect, if connected to a server.
	 * @author simon
	 * @see SimpGame#demolishGame()
	 *
	 */
	public class StopButtonListener implements ActionListener{
		SimpGame sg;
		public StopButtonListener(SimpGame sg){
			this.sg = sg;
		}
		public void actionPerformed(ActionEvent e) {
			sg.sendMessage("D " + sg.gameWindow.getPlayerNumber());
			sg.demolishGame();
		}
	}
	
	public class AboutButtonListener implements ActionListener{
		SimpGame sg;
		public AboutButtonListener(SimpGame sg){
			this.sg = sg;
		}
		public void actionPerformed(ActionEvent e){
			String aboutString = "";
			aboutString += "SimpGame Alpha-version." + System.lineSeparator();
			aboutString += "Created and coded by Simon Wallgren, using Eclipse." + System.lineSeparator();
			aboutString += "Graphics by Simon Wallgren, using GIMP (GNU Image Manipulation Program)." + System.lineSeparator();
			aboutString += "Sound effects not yet developed." + System.lineSeparator();
			aboutString += "Music not yet developed." + System.lineSeparator();
			aboutString += System.lineSeparator();
			aboutString += "This game is heavily influenced by the game Mass Destruction." + System.lineSeparator();
			aboutString += "Mass Destruction developed by NMS Software Ltd., produced by ASC Games and BMG Interactive Entertainment." + System.lineSeparator();
			JOptionPane.showMessageDialog(sg, aboutString, "About SimpGame", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	public static void main(String[] args){
		new SimpGame();
	}
}
