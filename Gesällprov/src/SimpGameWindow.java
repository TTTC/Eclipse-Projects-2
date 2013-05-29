import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class SimpGameWindow extends JPanel/*JFrame*/ implements Runnable {
	private static final long serialVersionUID = 1L;//Default auto-generated
	
	private Canvas playableArea;
	private ConcurrentHashMap<String, Tank> playerTanks; //Keeps track of player's tanks
	private Set<SimpGameEntity> entities; //Keeps track of all other entities (including player projectiles)

	private JTextArea log;
	
	private int player; //This player
	private int weapon; //Weapon for this player
	private int[] ammo; //Keeps track of the ammunation of each weapon.
	
	//pressedKeys is used to check which movement and rotatement keys are held down (W, A, S, D).
	private boolean[] pressedKeys = {false, false, false, false};
	
	private boolean isRunning; //Used to stop the game loop.
	
	/**
	 * Constructor. This construtor is supposed to be called by SimpGameServer as return the SimpGameWindow, the playing field, that the server uses. This constructor is also part of the other constructor {@link #SimpGameWindow(int, SimpGame)}, in other to reduce some lines of code. 
	 * @see #SimpGameWindow(int)
	 */
	public SimpGameWindow(){
		//Create a thread-safe HashSet by basing it on ConcurrentHashMap.
		this.entities = Collections.newSetFromMap(new ConcurrentHashMap<SimpGameEntity, Boolean>());
		
		this.playerTanks = new ConcurrentHashMap<String, Tank>();
		this.player = this.weapon = -1; //-1 signals a server
		this.isRunning = true;
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		
		this.playableArea = new Canvas();
		this.playableArea.setPreferredSize(new Dimension(800, 600));
		this.playableArea.setMinimumSize(new Dimension(800, 600));
		this.playableArea.setBackground(Color.WHITE);
		
		this.add(playableArea, gbc);
		
		this.log = new JTextArea();
		this.log.setSize(300,1);//Width starts at 300, height will fill out to whatever.
		this.log.setLineWrap(true);
		this.log.setWrapStyleWord(true);
		this.log.setEditable(false);
		
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.BOTH;
		this.add(new JScrollPane(log), gbc);
		//this.setSize(1800, 600);


	}
	/**
	 * Constructor. This constructor is supposed to be called by the SimpGame-class, the client class of the game. This constructor also calls the {@link #SimpGameWindow()}, in order to save a couple of lines of code.
	 * @param player the number, or slot, associated with the player that the calling {@link SimpGame} game-client has. Used in this class to keep track of which tank is the player's.
	 */
	public SimpGameWindow(int player){
		this();//Calling the other constructor
		this.player = player;
		this.weapon = 1; //Not a server, is a player, set default weapon.
		this.ammo = new int[] {0,0,0,0,0};//Currently only five weapons
		
		/* Add a listener for the keyboard, and allow the playable area to be focusable*/
		this.playableArea.setFocusable(true);
		this.playableArea.addMouseListener(new GameFocusListener());
		this.playableArea.addKeyListener(new GameKeyListener());
	}
	
	/**
	 * Used by the client or server classes to create the buffer-strategy, which is needed to display the playing field.
	 * @param num
	 */
	public void playableAreaBuffer(int num){
		this.playableArea.createBufferStrategy(num);
	}
	
	public ConcurrentHashMap<String, Tank> getPlayerTanks(){
		return playerTanks;
	}
	
	/**
	 * Method that, based upon a string, creates a game-entity and returns it. These entities may be player tanks, projectiles, items, and so on. This method is used when a message is received from the server and needs to be converted into the entities in the game, such as when a player has connected and the server sent information about the current state of the game. Will do nothing if the string is not formatted as expected. 
	 * @param entityInfo the string that is to be converted into a game-entity.
	 * @return the resulted game-entity based upon the string, or null if the string is ill-formatted.
	 * @see #entitiesToMessage()
	 */
	public SimpGameEntity stringToEntity(String entityInfo){
		String[] data = entityInfo.split(" ");
		switch(data[0]){
			case "Tank":
				if(data.length == 7)
					return new Tank(Double.parseDouble(data[1]), 
									Double.parseDouble(data[2]), 
									Integer.parseInt(data[3]), 
									Integer.parseInt(data[4]), 
									Integer.parseInt(data[5]), 
									Integer.parseInt(data[6]));
				if(data.length == 8)
					return new Tank(Double.parseDouble(data[1]), 
									Double.parseDouble(data[2]), 
									Integer.parseInt(data[3]), 
									Integer.parseInt(data[4]), 
									Integer.parseInt(data[5]), 
									Integer.parseInt(data[6]),
									Integer.parseInt(data[7]));
			case "Shell":
				return new Shell(Double.parseDouble(data[1]), Double.parseDouble(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]), Integer.parseInt(data[5]));
			case "SuperShell":
				return new SuperShell(Double.parseDouble(data[1]), Double.parseDouble(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]));
			case "Mortar":
				return new Mortar(Double.parseDouble(data[1]), Double.parseDouble(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]), Integer.parseInt(data[5]));
			case "Missile":
				return new Missile(Double.parseDouble(data[1]), Double.parseDouble(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]));
			case "Mine":
				return new Mine(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
			case "SuperShellItem":
				return new SuperShellItem(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
			case "MortarItem":
				return new MortarItem(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
			case "MissileItem":
				return new MissileItem(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
			case "MineItem":
				return new MineItem(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
		}
		return null;
	}
	
	/**
	 * Adds a player's tank into the game, or ConcurrentHashMap, in order to be included in the game.
	 * @param playerNum the number associated with the player whose tank is being added.
	 * @param entityInfo the string representation of this player's tank.
	 */
	public void addPlayerEntity(String playerNum, String entityInfo){
		this.playerTanks.put(playerNum, (Tank)this.stringToEntity(entityInfo));
	}
	
	/**
	 * Adds an entity into the game, or Set, in order to be included in the game.
	 * @param entityInfo
	 */
	public void addEntity(String entityInfo){
		this.entities.add(this.stringToEntity(entityInfo));
	}
	/**
	 * Creates a string where the string-representation of every entity in the game is contained. This is done by calling the {@link SimpGameEntity#toMessage()}-method. Player entities, the tanks, come first and are seperated by a "P". Non-player entities, such as projectiles, items, mines, come afterwards, and are separated from eachother by a "E". The tank and entity-portions are also separated by an asterix. An example of the returned string would be "P0 Tank 100 200 359 75 0 1 444*ESuperShellItem 400 800 999" - in which would be resulted if the game contained one player, occupying slot 0, with a tank at x-coord 100, y 200, facing angle 359, not moving, but rotating left, at 75 hp, and a SuperShellItem at x-coord 400, y 800. The 444 and 999 values are a number that is used as hash-value and used to identify objects across the server and clients.
	 * @return A string containing the string-representations of every entity in the game.
	 * @see #stringToEntity(String)
	 * @see SimpGameEntity#toMessage()
	 */
	public String entitiesToMessage(){
		String msg = "";
		
		for(String playerNum : playerTanks.keySet())
			msg += "P" + playerNum + " " + playerTanks.get(playerNum).toMessage();
		
		msg += "*";
		for(SimpGameEntity entity : entities)
			msg += "E" + entity.toMessage();

		
		return msg;
	}
	
	/**
	 * TODO
	 * @param cmd
	 */
	public void applyCommand(String cmd){
		String[] data = cmd.split(" ");
		Tank tank;
		switch(data[0]){
			case "M"://Move
				tank = playerTanks.get(data[1]);
				int movement = Integer.parseInt(data[2]);
				tank.setMovement(movement);
				if(movement == 0){
					tank.setX(Double.parseDouble(data[3]));
					tank.setY(Double.parseDouble(data[4]));
					tank.setAngle(Integer.parseInt(data[5]));
				}
				break;
			case "R"://Rotate
				tank = playerTanks.get(data[1]);
				int rotatement = Integer.parseInt(data[2]);
				tank.setRotatement(rotatement);
				if(rotatement == 0){
					tank.setX(Double.parseDouble(data[3]));
					tank.setY(Double.parseDouble(data[4]));
					tank.setAngle(Integer.parseInt(data[5]));
				}
				break;
			case "A"://Attack / Shoot
				Weapon weap = null;
				switch(data[2]){
					case "1"://Shell
						weap = new Shell(Double.parseDouble(data[3]), Double.parseDouble(data[4]), Integer.parseInt(data[5]));
						break;
					case "2"://SuperShell
						weap = new SuperShell(Double.parseDouble(data[3]), Double.parseDouble(data[4]), Integer.parseInt(data[5]));
						break;
					case "3"://Mortar
						weap = new Mortar(Double.parseDouble(data[3]), Double.parseDouble(data[4]), Integer.parseInt(data[5]));
						break;
					case "4"://Missile
						weap = new Missile(Double.parseDouble(data[3]), Double.parseDouble(data[4]), Integer.parseInt(data[5]));
						break;
					case "5"://Mine
						weap = new Mine(Double.parseDouble(data[3]), Double.parseDouble(data[4]));
						break;
				}
				/*
				 * I actually encountered situations where it couldn't add because of similiar hash-values.
				 * However, I will, for now, not do anything about it. The player will simply have to deal
				 * with attacks sometimes not working. 
				 */
				if(!entities.contains(weap)){
					entities.add(weap);
					playerTanks.get(data[1]).setCooldown(60);
				}
				break;
			case "N"://New player
				switch(data[1]){
					case "Tank":
						this.addPlayerEntity(data[5], 
										data[1] + " " +
										data[2] + " " +
										data[3] + " " +
										data[4] + " 100 0 0");
						break;
					default:
						this.addEntity(cmd.substring(cmd.indexOf(" ")+1, cmd.length()));
						break;
				}
				break;
			case "D"://Disconnect
				playerTanks.remove(data[1]);
				break;
			case "H"://Collision
				//First get the objects that collided
				Tank tnk = playerTanks.get(data[1]);
				SimpGameEntity ent = null;
				for(SimpGameEntity entity : entities)
					if(entity.hashCode() == Integer.parseInt(data[2]))
						ent = entity;
				//Check what kind of entity the tank collided with
				if(ent instanceof Weapon){
					tnk.setHP(tnk.getHP() - ((Weapon)ent).getDamage());
					if(player == -1 && tnk.getHP() < 1){
						Random rand = new Random();
						int randX = rand.nextInt(750) + 50;
						int randY = rand.nextInt(550) + 50;
						int randAngle = rand.nextInt(359) + 1;
						String respawnString = data[1] + " " + randX + " " + randY + " " + randAngle;//String for the tank of the new player
						((SimpGameServer)this.getTopLevelAncestor()).sendCommand("RS " + respawnString);
						this.applyCommand("RS " + respawnString);
					}
				}
				else if(ent instanceof WeaponItem && Integer.parseInt(data[1]) == player){
					this.ammo[((WeaponItem)ent).ammoType()] += 10;
				}
				//Maybe add collision with other tanks in the future
				
				//Remove the entity that the tank collided with
				entities.remove(ent);
				break;
			case "RS":
				new Thread(new RespawnHandler(cmd)).start();
				break;
		}
	}
	/**
	 * TODO
	 */
	public void run(){
		int lastMovementSent = 0, lastRotatementSent = 0;
		/* ,.-*'/// The Game loop \\\'*-., */
		while(isRunning){
			long currTime = System.currentTimeMillis();
			/* Mechanics */
			
			//Add items
			//TODO Better way of adding items
			if(player == -1){
				Random randItem = new Random();
				if(randItem.nextInt(1000) == 1){
					int x = randItem.nextInt(600)+100;
					int y = randItem.nextInt(400)+100;
					int wep = randItem.nextInt(4);
					String weaponType = "";
					if(wep == 0)
						weaponType = "SuperShellItem";
					else if(wep == 1)
						weaponType = "MortarItem";
					else if(wep == 2)
						weaponType = "MissileItem";
					else if(wep == 3)
						weaponType = "MineItem";
					String newItemString = weaponType + " " + x + " " + y;
					((SimpGameServer)this.getTopLevelAncestor()).sendCommand("N " + newItemString);
					this.applyCommand("N " + newItemString);
				}				
			}
			
			//Check user input if player (server has player set to -1)
			if(player != -1){
				/* The next three if-statements check if the user is 
				 * trying to move or stop, and send the command to 
				 * the server, if it has not already been sent. */
				if(!this.pressedKeys[0] && !this.pressedKeys[2] && lastMovementSent != 0){
					((SimpGame)(getTopLevelAncestor())).sendMessage("M " + player + " 0"); //Stop
					lastMovementSent = 0;
				}
				else if(this.pressedKeys[0] && !this.pressedKeys[2] && lastMovementSent != 1){
					((SimpGame)(getTopLevelAncestor())).sendMessage("M " + player + " 1"); //Forward
					lastMovementSent = 1;
				}
				else if(!this.pressedKeys[0] && this.pressedKeys[2] && lastMovementSent != 2){
					((SimpGame)(getTopLevelAncestor())).sendMessage("M " + player + " 2"); //Backward
					lastMovementSent = 2;
				}
				/* Next three is the same deal, but for rotating */
				if(!this.pressedKeys[1] && !this.pressedKeys[3] && lastRotatementSent != 0){
					((SimpGame)(getTopLevelAncestor())).sendMessage("R " + player + " 0"); //Stop
					lastRotatementSent = 0;
				}
				else if(this.pressedKeys[1] && !this.pressedKeys[3] && lastRotatementSent != 1){
					((SimpGame)(getTopLevelAncestor())).sendMessage("R " + player + " 1"); //Left
					lastRotatementSent = 1;
				}
				else if(!this.pressedKeys[1] && this.pressedKeys[3] && lastRotatementSent != 2){
					((SimpGame)(getTopLevelAncestor())).sendMessage("R " + player + " 2"); //Right
					lastRotatementSent = 2;
				}	
			}
			//Move us
			this.moveEntities();
			//Check if tank is outside border
			for(Tank tank : playerTanks.values()){
				double[][] cornerArr = tank.getCorners();
				for(int o = 0; o < 4; o++){
					if(cornerArr[o][0] < 0.0)
						tank.setX(tank.getXCoord() + Math.abs(cornerArr[o][0]) + 1);
					else if(cornerArr[o][0] > 800.0)
						tank.setX(tank.getXCoord() - (cornerArr[o][0] - 799));
					if(cornerArr[o][1] < 0)
						tank.setY(tank.getYCoord() + Math.abs(cornerArr[o][1]) + 1);
					else if(cornerArr[o][1] > 600.0)
						tank.setY(tank.getYCoord() - (cornerArr[o][1] - 599));
				}
			}
			//Check for missiles whose lifespan has ended or are outside map
			Set<SimpGameEntity> removeUs = new HashSet<SimpGameEntity>();
			for(SimpGameEntity entity : entities)
				if(entity.getXCoord() > 800 || entity.getXCoord() < 0
				|| entity.getYCoord() > 600 || entity.getYCoord() < 0)
					removeUs.add(entity);
				else if(entity instanceof Shell){
					if(((Shell)entity).getFramesToLive() < 1)
						removeUs.add(entity);
				}//Brackets to avoid the elseif below accidently belonging to the if above
				else if(entity instanceof Mortar)
					if(((Mortar)entity).getFramesToLive() < 1)
						removeUs.add(entity);
			for(SimpGameEntity entity : removeUs)
				entities.remove(entity);
			
			/* Check collision */
			//Check if a tank is overlapping an entity
			//This is done for the server only
			/* Begin by creating a polygon for each tank, 
			 * rather than having to re-create them when 
			 * going through each entity.
			 * 
			 * Actually, keeping a list of tank polygons
			 * and adding and removing them when a player
			 * connects or disconnects would probably 
			 * have been a lot better, since this re-
			 * creating will be done so so much if
			 * done in the game loop. But I will leave
			 * it like this for now...
			 * */
			if(player == -1){
				List<Polygon> tankPolys = new ArrayList<Polygon>();
				for(Tank tank : playerTanks.values()){
					double[][] cornerArr = tank.getCorners();
					Polygon tankPoly = new Polygon();
					for(int i = 0; i < cornerArr.length; i++)
						tankPoly.addPoint((int)cornerArr[i][0], (int)cornerArr[i][1]);
					tankPolys.add(tankPoly);
				}
				for(SimpGameEntity entity : entities){
					double[][] cornerArr = entity.getCorners();
					for(int i = 0 ; i < tankPolys.size(); i++)
						for(int o = 0; o < cornerArr.length; o++)
							if(tankPolys.get(i).contains(cornerArr[o][0], cornerArr[o][1])){
								//COLLISION!!!
								String collisionString = i + " " + entity.hashCode();
								((SimpGameServer)this.getTopLevelAncestor()).sendCommand("H " + collisionString);
								this.applyCommand("H " + collisionString);
							}
				}
			}
			/* Paint me, baby */
			this.paintComponent((Graphics2D)this.playableArea.getBufferStrategy().getDrawGraphics());
			
			long timeElapsed = System.currentTimeMillis() - currTime;
			long frameTime = (long) (1000.0 / 60.0); //16.66666...
			if(timeElapsed < frameTime)
				try {
					Thread.sleep(frameTime - timeElapsed);
				} catch (InterruptedException e) {
					//Do nothing. It's ok if one frame is slightly shorter.
				}
		}
	}
	
	/**
	 * Helper method used in the game loop to move every entity.
	 */
	public void moveEntities(){
		for(Tank tank : playerTanks.values())
			tank.moveAndRotate();
		
		for(SimpGameEntity entity : this.entities)
			entity.moveAndRotate();
	}
	/**
	 * Method responsible for painting all objects to the playing field. A buffer strategy is used to avoid flickering.
	 * @param g2 the graphics of the playing field.
	 */
	public void paintComponent(Graphics2D g2){
		g2.setPaint(getBackground());
		g2.fillRect(0,0,playableArea.getWidth(), playableArea.getHeight());
		for(SimpGameEntity entity : this.entities)
			entity.paint(g2);
		for(Tank tank : playerTanks.values())
			tank.paint(g2);
		
		g2.dispose();
		playableArea.getBufferStrategy().show();
		Toolkit.getDefaultToolkit().sync();
	}
	
	/**
	 * Takes a message and prints it in the log. The message is supposed to be quite bare, and only contain the actual message, and not contain any formating, not contain any information ABOUT the message. This method will add time and ">>" before any message, and line-separators after it to separate each message.
	 * @param msg The message to be displayed in the log.
	 */
	synchronized public void printToLog(String msg){
		String currTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
		log.append(currTime + ">> " + msg + System.lineSeparator());
		log.setCaretPosition(log.getDocument().getLength());
	}
	
	/**
	 * Used to stop the game-loop to allow the game to end.
	 */
	public void stopRunning(){
		this.isRunning = false;
	}
	
	
	public int getPlayerNumber(){
		return this.player;
	}
	
	/**
	 * Listening for when the focus is on the playing field, where the user would want to press keys in order to control the tank. However, since I did not want the tank to move if the player has focus on another portion of the game, such as the log, or has focus on another application entirely, this focus-listener is used to avoid the tank from moving if the user does not have focus on this game.
	 * @author simon
	 *
	 */
	public class GameFocusListener implements MouseListener{
		public void mouseClicked(MouseEvent e) {
			e.getComponent().requestFocusInWindow();
		}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
	}
	/**
	 * Listening class for the keyboard, to allow the user to move, attack, and change weapon. 
	 * @author simon
	 * @see GameKeyListener#keyPressed(KeyEvent)
	 */
	public class GameKeyListener implements KeyListener
	{
		/**
		 * Called when the user presses a key on the keyboard, and has focus on the game window and playing field. The keys listened to are W, A, S, D, Space, and numeric keys 1-5. When W, A, S or D are pressed, the client will send a message to the server telling it that this tank wants to move. This listening-class also listens to when these keys are released. This is because the game, upon release of the key, will send another message to the server, indicating that the tank wants to stop moving. Do note, however, that the actual pressing of the key isn't actually what's causing the message to be sent. All that happens is that a variable is set to true. The client's game loop then checks if this variable is true, and if it is sends the message. If the client has already sent the message without the variable changing, no message is sent to avoid sending pointless messages and wasting bandwidth.
		 * @see #keyReleased(KeyEvent)
		 */
		synchronized public void keyPressed(KeyEvent arg0) {
			int key = arg0.getKeyCode();
			switch(key){
				case KeyEvent.VK_1:
					weapon = 1;
					break;
				case KeyEvent.VK_2:
					if(ammo[1] > 0)
						weapon = 2;
					break;
				case KeyEvent.VK_3:
					if(ammo[2] > 0)
						weapon = 3;
					break;
				case KeyEvent.VK_4:
					if(ammo[3] > 0)
						weapon = 4;
					break;
				case KeyEvent.VK_5:
					if(ammo[4] > 0)
						weapon = 5;
					break;
				/*case KeyEvent.VK_6:
					if(ammo[5] > 0)
						weapon = 6;
					break;
				case KeyEvent.VK_7:
					if(ammo[6] > 0)
						weapon = 7;
					break;
				case KeyEvent.VK_8:
					if(ammo[7] > 0)
						weapon = 8;
					break;
				case KeyEvent.VK_9:
					if(ammo[8] > 0)
						weapon = 9;
					break;*/
				case KeyEvent.VK_W:
					pressedKeys[0] = true;
					break;
				case KeyEvent.VK_S:
					pressedKeys[2] = true;
					break;
				case KeyEvent.VK_A:
					pressedKeys[1] = true;
					break;
				case KeyEvent.VK_D:
					pressedKeys[3] = true;
					break;
				case KeyEvent.VK_SPACE:
					if(playerTanks.get("" + player).getCooldown() == 0){
						((SimpGame)(getTopLevelAncestor())).sendMessage("A " + player + " " + weapon);
						ammo[weapon-1] -= 1;
					}
					break;
			}
		}
		/**
		 * Called when the user releases a key on the keyboard. The keys listened to are W, A, S and D. When W, A, S or D are released, the client will send a message to the server telling it that this tank wants to stop moving. This listening-class also listens to when these keys are pressed. This is because the game, upon a key being pressed, will send a message to the server, indicating that the tank wants to move. Do note, however, that the actual releasing of the key isn't actually what's causing the message to be sent. All that happens is that a variable is set to true. The client's game loop then checks if this variable is true, and if it is sends the message. If the client has already sent the message without the variable changing, no message is sent to avoid sending pointless messages and wasting bandwidth.
		 * @see #keyPressed(KeyEvent)
		 */
		synchronized public void keyReleased(KeyEvent arg0) {
			int key = arg0.getKeyCode();
			switch(key){
				case KeyEvent.VK_W:
					pressedKeys[0] = false;
					break;
				case KeyEvent.VK_S:
					pressedKeys[2] = false;
					break;
				case KeyEvent.VK_A:
					pressedKeys[1] = false;
					break;
				case KeyEvent.VK_D:
					pressedKeys[3] = false;
					break;
			}
		}

		public void keyTyped(KeyEvent arg0) {
			//Nothing
		}
	}
	/**
	 * Class that is responsible for handling the respawning of the player, and in particular the timer before the actual respawning. Since the the respawning happens five seconds after dying, there was a need to sleep a thread for 5000 ms, and then respawn. Obviously this could not be done in the game loop, or the loop that listens for messsages. Instead, it was done in a new class, who's only responsability was the respawning. Another solution would have been to have a varaible, such as an int, and reduce it every frame to work as a timer (kinda like how in assembler you make the processor do completely useless things in order to have it "wait").
	 * @author simon
	 *
	 */
	public class RespawnHandler implements Runnable{
		String player;
		double x, y;
		int angle;
		/**
		 * Constructor. The new coords and angle for the tank are actually made up at the server, and sends them to all players when the tank dies. When a client receives this message, a new RespawnHandler object is created, taking the message as an argument to this constructor, and extracts the dying player, the coords and angle. The client then starts this run method.
		 * @param cmd string containing information about the player associated with this RespawnHandler object, aswell as the new coords and angle for the tank.
		 */
		public RespawnHandler(String cmd){
			String[] data = cmd.split(" ");
			this.player = data[1];
			this.x = Double.parseDouble(data[2]);
			this.y = Double.parseDouble(data[3]);
			this.angle = Integer.parseInt(data[4]);
		}
		/**
		 * Waits for five seconds, then sets the values of this tank, associated with this RespawnHandler, to the ones given when this RespawnHandler object was given. Also sets the tank's hp to 100. Notice that if the sleeping thread is interrupted, the tank will be respawned prematurely.
		 */
		public void run(){
			Tank tank = playerTanks.get(this.player);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				//Do nothing. It's ok if the respawn timer is shorter.
			} finally {
				tank.setX(this.x);
				tank.setY(this.y);
				tank.setAngle(this.angle);
				tank.setHP(100);
			}
		}
	}
}
