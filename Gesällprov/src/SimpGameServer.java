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
import java.net.UnknownHostException;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The server class for the SimpGame. This class takes care of most server-related parts of the game. It handles the messages sent to and from the server, before passing the message on to either the game mechanic-port of {@link SimpGameWindow} or passing it to all the connected players. The main-class will bring up a window, JFrame, which the user can use to alter the settings - name of the server, which port to start the server on, and the max amount of players. The server also prints messages when a player connects (more messages will be printed in the future). Since this class uses the {@link SimpGameWindow}-class, it it necessary that it is able to find it on the computer. It is not possible to run the server without, since some game-mechanics the server needs are in there. 
 * @author simon
 * @see SimpGame
 * @see SimpGameWindow
 */
public class SimpGameServer extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;//Default auto-generated
	
	private String name; //Name of server
	private int port; //Port which players are supposed to connect to
	private int maxp; //Maximum players allowed on the server
	
	private Player[] playerArr; //Contains all player's currently connected
	private int currNumPlayers; //Number of players currently connected
	private DatagramSocket socket; //The server's socket
	
	private JTextField nameField;
	private JTextField portField;
	private JTextField maxpField;
	
	private JButton startButton;
	private JButton stopButton;
	
	private SimpGameWindow gameWindow;
	
	private boolean isRunning;
	/**
	 * Constructor. Sets up the GUI for the server, sets up button listeners, aswell as setting up a bare playing field, which allocates the space where the gameplay will be, once the server is running. The playing field is represented by the class {@link SimpGameWindow}. The constructor does not start the game.
	 */
	public SimpGameServer(){
		super("Server not running");
		
		JLabel nameLabel = new JLabel("Server name: ");
		JLabel portLabel = new JLabel("Port: ");
		JLabel maxpLabel = new JLabel("Max number of players: ");
		
		nameField = new JTextField("My Awesome Server");
		portField = new JTextField("51302");
		maxpField = new JTextField("8");
		
		startButton = new JButton("Start");
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		
		startButton.addActionListener(new StartButtonListener(this));
		stopButton.addActionListener(new StopButtonListener(this));
		
		
		
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
		content.add(portLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		content.add(portField, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		content.add(maxpLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
		content.add(maxpField, gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		content.add(startButton, gbc);		
		gbc.insets = new Insets(0,100,0,0);
		content.add(stopButton, gbc);
	
		this.gameWindow = new SimpGameWindow();			
		this.add(gameWindow, BorderLayout.CENTER);
		//Rensa upp lite här. Vilka metoder behöver eller behöver inte kallas på?
		//Det får va såhär sålänge.
		this.gameWindow.paintComponents(this.getGraphics());
		this.revalidate();
		this.repaint();
		this.pack();
		this.paintComponents(this.getGraphics());

		this.add(content, BorderLayout.NORTH);
		this.pack();//this.setSize(new Dimension(540, 400));
		this.setVisible(true);
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				sendCommand("DD");
				stopServer();
				System.exit(0);
			}
		});
	}
	
	/**
	 * Method that is supposed to handle all messages sent to the server. These messages may be requests to connect to the server, or a player's command in the game, or player's announcing that they are disconnecting. If a connect or disconnect message is successfully parsed, a method is called to deal with the message correctly, rather than having all that code in the run-method.
	 */
	public void run(){
		while(this.isRunning){
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
				 * would be thrown, practically (possibly by not being formatted 
				 * with UTF-8, but I will assume the game is formatting by UTF-8).
				 * So I'll just have the game quit for now. 
				 */
				
				System.exit(33);
				e1.printStackTrace();
			} 
			
			/*
			 * Connect:		"C name"
			 * Disconect:	"D player"
			 * Move:		"M player direction" direction = 0(stop) | 1(forward) | 2(backward)
			 * Rotate:		"R player direction" direction = 0(stop) | 1(left) | 2(right)
			 * Attack:		"A player weapon" weapon = 0-9
			 */
			if(msg.matches("C .+")){
				//Connect player
				connect(packet.getAddress(), packet.getPort(), msg.split(" ")[1]);
			}
			else if(msg.matches("D \\d+")){
				//Disconnect player
				disconnect(Integer.parseInt(msg.split(" ")[1]));
			}
			else if(msg.matches("A \\d+ \\d")){
				calculateAttack(msg);
			}
			else if(msg.matches("(M|R) \\d+ (1|2)")){
				gameWindow.applyCommand(msg);
				//Send command to rest of the players
				sendCommand(msg);
			}
			else if(msg.matches("M \\d+ 0")){
				/* Stopped moving - send information about current
				 * location of tank aswell, to avoid out-of-sync-problems
				 */
				Tank tank = gameWindow.getPlayerTanks().get(msg.split(" ")[1]);
				msg += " " + tank.getXCoord() + " " + tank.getYCoord() + " " + tank.getAngle();
				gameWindow.applyCommand(msg);
				sendCommand(msg);
			}
			else if(msg.matches("R \\d+ 0")){
				/* Stopped rotating - send information about current
				 * angle of tank aswell, to avoid out-of-sync-problems
				 */
				Tank tank = gameWindow.getPlayerTanks().get(msg.split(" ")[1]);
				msg += " " + tank.getXCoord() + " " + tank.getYCoord() + " " + tank.getAngle();
				gameWindow.applyCommand(msg);
				sendCommand(msg);
			}
		}
	}
	
	/**
	 * Handles a player that is trying to connect. If there is room for another player, the server replies by sending the message "A-" followed by information about the game, such as player's tanks, projectiles flying, and other entities. The new player's tank is also sent to the player's already in the game. If connection is denied, the server replies "D" followed by a code explaining why, such as 10 (server full).
	 * @param sender The address of the player attempting to connect.
	 * @param port The port of the player attempting to connect.
	 * @param name The name which the player is attempting to connect with.
	 */
	synchronized public void connect(InetAddress sender, int port, String name){
		/* Reply either "D 'reason as int'" or "A 'game-info'", 
		 * D = denied, A = accepted */
		if(currNumPlayers >= maxp){
			sendMessage("D 10", sender, port);
		}
		else{
			//Get the new player's slot number
			int i = 0;
			while(playerArr[i] != null)
				i++;//Increase i until empty slot in arr is found
			
			/*
			 * Create a string which represents the new player's tank,
			 * and send that info to all player's already in the game
			*/
			Random rand = new Random();
			int randX = rand.nextInt(750) + 50;
			int randY = rand.nextInt(550) + 50;
			int randAngle = rand.nextInt(359) + 1;
			String newTankString = "Tank " + randX + " " + randY + " " + randAngle;//String for the tank of the new player
			this.sendCommand("N " + newTankString + " " + i);
			
			
			/*
			 * Add the new player's tank, then create a string representation
			 * containing information about new players slot and the state of
			 * the game and of all the players' tanks in the game, and of 
			 * every entity in the game, then send that representation to the
			 * new player.
			 * For example, a tank owned by player 1 will be represented by 
			 * the string "P1*Tank 50 50 25 25 145 100 1 2 0", and the whole
			 * string might be something like: "A*My Server*8*2*'entity-info'"
			 */
			playerArr[i] = new Player(sender, port, name);
			gameWindow.addPlayerEntity("" + i, newTankString + " 100 0 0");
			currNumPlayers++;
			
			String msg = "A*" + this.name + "*" + this.maxp + "*" + i + "*";
			msg += gameWindow.entitiesToMessage();
			sendMessage(msg, sender, port);
			gameWindow.printToLog(sender.getHostAddress() + "(" + port + ") connected as " + name);
		
			try {
				setTitle(this.name + " (" + InetAddress.getLocalHost().getHostAddress() + ":" + this.port + "), " + currNumPlayers + " players");
			} catch (UnknownHostException e1) {
				setTitle(this.name + " (---:" + this.port + "), " + currNumPlayers + " players");
			}
		}
	}
	
	/**
	 * Called when a user sent the message "D 'playernumber'", indicating that that player has disconnected. This method let's all other player's know about the disconnecting player, it also opens a slot for another player to join, aswell as updating the info in the title.
	 * @param player the number of the player, or the player's slot, that disconnected
	 */
	synchronized public void disconnect(int player){
		gameWindow.applyCommand("D " + player);
		playerArr[player] = null;
		currNumPlayers--;
		this.sendCommand("D " + player);
		
		try {
			setTitle(this.name + " (" + InetAddress.getLocalHost().getHostAddress() + ":" + this.port + "), " + currNumPlayers + " players");
		} catch (UnknownHostException e1) {
			setTitle(this.name + " (---:" + this.port + "), " + currNumPlayers + " players");
		}
	}
	/**
	 * Called when a user sent the message "A num num", indicating a request to fire a weapon. This method basically figures out the coordinates and angle for the projectile, or mine, to be spawned, and then sends the calculated values to all players, so the projectiles can be created and displayed on everyone's screen. The calculation of the coordines and the angle are different depending on weapon. For example, the regular Shell have a random angle up to 15 degrees of the shooting tanks angle, and the mine's coordinates are at the back of the tank, rather than the front.
	 * @param msg
	 */
	public void calculateAttack(String msg){
		String[] data = msg.split(" ");
		/*
		 * data[0] = A
		 * data[1] = player
		 * data[2] = weapon
		 */
		Tank tank = gameWindow.getPlayerTanks().get(data[1]);
		if(tank.getHP() > 0 && tank.getCooldown() == 0){
			/*
			 * All weapons but the mines have the same spawn coords, 
			 * that of a pixel in front of the tank, so set these 
			 * coords here, and the mines will replace with the 
			 * correct coords for it, if the fired weapon was a mine.
			 * 
			 * Also, for the weapons that have an angle in which they
			 * travel, the only one that is not initially the same as
			 * the tank's angle is the regular shells. The angle will
			 * be specified here, and the regular shell will replace.
			 */
			/*String x = "" + (tank.getXCoord() + tank.getWidth()/2);
			String y = "" + (tank.getYCoord() + tank.getHeight()/2);*/
			double[][] cornerArr = tank.getCorners();
			String x = "" + ((cornerArr[1][0] + cornerArr[3][0]) / 2);
			String y = "" + ((cornerArr[1][1] + cornerArr[3][1]) / 2);
			String angle = "" + tank.getAngle();
			switch(data[2]){
				case "1"://Shell
					//x and y already set
					/* Random angle as far as 15 degrees off of tank's current angle */
					Random rand = new Random();
					angle = "" + (tank.getAngle() + 15 - rand.nextInt(30));
					break;
				case "2"://SuperShell
					//x, y and angle already set
					break;
				case "3"://Mortar
					//x, y and angle already set
					break;
				case "4"://Missile
					//x, y and angle already set
					break;
				case "5"://Mine
					//Set correct coords for mines
					x = "" + ((cornerArr[0][0] + cornerArr[2][0]) / 2);
					y = "" + ((cornerArr[0][1] + cornerArr[2][1]) / 2);
					//Mines have no angle. Set angle-string as empty string.
					angle = "";
					break;
			}
			sendCommand("A " + data[1] + " " + data[2] + " " + x + " " + y + " " + angle);
			gameWindow.applyCommand("A " + data[1] + " " + data[2] + " " + x + " " + y + " " + angle);
		}
	}
	/**
	 * Sends a message to all player's currently connected to this server. Primarily this will be commands done by players, which should then be applied by each player's game, but it may also be things like a player connecting, or disconnecting. Loops through each player and calls the sendMessage-method.
	 * @param cmd The message to be sent to all players.
	 */
	synchronized public void sendCommand(String cmd){
		for(int i = 0; i < playerArr.length; i++)
			if(playerArr[i] != null)
				sendMessage(cmd, playerArr[i].address, playerArr[i].port);
	}
	
	/**
	 * Sends a message to a particular player. This may be messages indicating that a connection was accepted or denied, some player's command, information about players or anything else.
	 * @param message The message to send.
	 * @param address The address to send to.
	 * @param port The port to send to.
	 */
	public void sendMessage(String message, InetAddress address, int port){
		try {
			byte[] messageData = message.getBytes("UTF-8");
			DatagramPacket packet = new DatagramPacket(messageData, messageData.length, address, port);
			socket.send(packet);	
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unable to encode with UTF-8");
			e.printStackTrace();
			System.exit(777);
		} catch (IOException e) {
			//Figure out better handling of these exceptions... later.
			System.exit(778);
			e.printStackTrace();
		}
	}
	/**
	 * Starts the server, based upon the value of the JFields nameField, portField and maxpField. If some of the fields are empty, or if the port or max number of players cannot be parsed as integers, it will display a dialog box, telling the user what the problem was. Otherwise, it will create a socket, setup a SimpGameWindow so the game can be viewed, and disable all fields and buttons, except for the stop-button which will be enabled. Also prints some information to the log and starts the thread which listens for messages.
	 */
	public void startServer(){
		String name = nameField.getText().trim();
		String port = portField.getText().trim();
		String maxp = maxpField.getText().trim();
		
		//Check for bad input
		if(name.equals("")
		|| port.equals("")
		|| maxp.equals("")){
			JOptionPane.showMessageDialog(this, "Name, Port or Max number of players may not be empty.", "Unable to start server", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(name.contains("*")){
			JOptionPane.showMessageDialog(this, "Name may not contain asterix (*)", "Unable to start server.", JOptionPane.ERROR_MESSAGE);
			return;//Asterix (*) not allowed, as it is used as a separator for messages.
		}
		try{
			Integer.parseInt(port);
			Integer.parseInt(maxp);
		}catch(IllegalArgumentException iae){
			JOptionPane.showMessageDialog(this, "Port or Max number of players may only contain numbers.", "Unable to start server", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			socket = new DatagramSocket(Integer.parseInt(port));
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(this, "Port is not available. Try another.", "Unable to start server", JOptionPane.ERROR_MESSAGE);
			return;
		}
		//Input is ok
		
		this.startButton.setEnabled(false);
		this.nameField.setEnabled(false);
		this.portField.setEnabled(false);
		this.maxpField.setEnabled(false);
		this.stopButton.setEnabled(true);
		
		this.name = name;
		this.port = Integer.parseInt(port);
		this.maxp = Integer.parseInt(maxp);
		this.playerArr = new Player[this.maxp];
		this.currNumPlayers = 0;
		
		try {
			gameWindow.printToLog("Server started!" + System.lineSeparator()
					+  "Name: " + this.name + System.lineSeparator()
					+  "IP-address: " + InetAddress.getLocalHost().getHostAddress() + System.lineSeparator()
					+  "Port: " + this.port + System.lineSeparator()
					+  "Max number of players: " + this.maxp);
		} catch (UnknownHostException e2) {
			gameWindow.printToLog("Server started!" + System.lineSeparator()
					+  "Name: " + this.name + System.lineSeparator()
					+  "IP-address: ---" + System.lineSeparator()
					+  "Port: " + this.port + System.lineSeparator()
					+  "Max number of players: " + this.maxp);
		}
		
		try {
			setTitle(this.name + " (" + InetAddress.getLocalHost().getHostAddress() + ":" + this.port + "), 0 players");
		} catch (UnknownHostException e1) {
			setTitle(this.name + " (---:" + this.port + "), 0 players");
		}
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
		new Thread(this).start();
	}
	/**
	 * Stops the server, and returns to a state as if the window had been closed and then re-opened, except that the values of the JFields will remain.
	 */
	public void stopServer(){
		//
		this.isRunning = false;
		this.gameWindow.stopRunning();
		//
		this.socket.close();
	
		this.currNumPlayers = -1;
		this.maxp = -1;
		this.port = -1;
		this.name = "";
		this.playerArr = null;
		this.setTitle("Server not running");
		this.gameWindow.printToLog("Stopped server.");
		
		this.gameWindow = new SimpGameWindow();			
		this.add(gameWindow, BorderLayout.CENTER);
		this.gameWindow.paintComponents(this.getGraphics());
		this.revalidate();
		this.repaint();
		this.pack();
		this.paintComponents(this.getGraphics());
		
		this.startButton.setEnabled(true);
		this.nameField.setEnabled(true);
		this.portField.setEnabled(true);
		this.maxpField.setEnabled(true);
		this.stopButton.setEnabled(false);
	}
	/**
	 * Listening class for the start button. Must be able to handle some data members in the server's main-class, so the constructor requires a reference to an object of the server's main class, SimpGameServer.
	 * @author simon
	 *
	 */
	public class StartButtonListener implements ActionListener{
		SimpGameServer sgServer;
		/**
		 * Simple constructor.
		 * @param sgServer An object of the server's main-class SimpGameServer, which method {@link SimpGameServer#startServer} will be called upon once the button is clicked.
		 */
		public StartButtonListener(SimpGameServer sgServer){
			this.sgServer = sgServer;
		}
		/**
		 * Called when the start-button is clicked. Calls the method in the SimpGameServer class that starts the server, {@link SimpGameServer#startServer}.
		 */
		public void actionPerformed(ActionEvent e) {
			sgServer.startServer();
		}
	}
	/**
	 * Class for listening on the stop-button in the game, which is used to stop the server.
	 * @author simon
	 *
	 */
	public class StopButtonListener implements ActionListener{
		SimpGameServer sgServer;
		/**
		 * Simple constructor
		 * @param sgServer An object of the server's main-class SimpGameServer, which method {@link SimpGameServer#stopServer} will be called upon once the button is clicked.
		 */
		public StopButtonListener(SimpGameServer sgServer){
			this.sgServer = sgServer;
		}
		/**
		 * Called when the stop-button is clicked. Sends a message to all players, indicating that the server is stopping, then calls the {@link SimpGameServer#stopServer}-method of the server object.
		 */
		public void actionPerformed(ActionEvent e) {
			//Let all players know that the server is stopping.
			sgServer.sendCommand("DD");
			sgServer.stopServer();
		}
	}
	
	public static void main(String[] args){
		new SimpGameServer();
	}
}
