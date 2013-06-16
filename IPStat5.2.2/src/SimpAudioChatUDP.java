import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
/**
 * Class for an application which allows users to communicate by audio with each other. When starting this application, the main-method expects zero or one arguments (see {@link #main(String[])} for details). This application uses UDP to comminucate. To actually start a session, the host and port values of the other application needs to be entered into this application's fields, and the user must press the connect-button. There also needs to be an open application on the other side. While the other application needn't actually attempt to connect yet, it still will need to be open, otherwise the attempt to connect will be failed due to ICMP-error unreachable port. When connecting, this application sends the message "C SimpAudioChat" to the other side, and expects an identical reply as acceptance. In other words, both applications must send the message "C SimpAudioChat" to their respective other side. There are no actual acceptance-phase, but just attempts to connect, which go through if both sides attempted to connect. 
 * 
 * While connected this application will continously capture audio in another thread, and send the captured audio to the other application. Audio received from the other application will also be played, in yet another thread. However, data received from the other side isn't necessarily audio, but could be a message indicating that the other application is disconnecting. If the check to see if it was a message to disconnect failed, it assumes that it is audio and attempts to play it. The audio has a sample rate of 8 kHz, sample size of 8 bits, 1-channeled (mono), and is stored as signed, big-endian data. The captured and retreived audio is in a buffer of 400 bytes, which should give an audio latency of about 50 ms (400 / 8000 = 0.05). The lines used for capture and playback of audio are received by the {@link AudioSystem#getTargetDataLine(AudioFormat)} and {@link AudioSystem#getSourceDataLine(AudioFormat)}. If there are no available lines for this format, the application terminates. 
 * 
 * Any time during the session a user may disconnect by pressing the disconnect-button. This cuases this application to send a message to the other application, telling it that it is disconnecting. Then the state of the application is brought back to that of an unconnected application - also freeing the used lines for audio capture and playback. This application also disconnects if the other application timed out, or sent a message that it is disconnecting, or if there were some kind of problem.
 * @author simon
 *
 */
public class SimpAudioChatUDP extends JFrame{
	private static final long serialVersionUID = 1L;
	/* Audio format values */
	private static final float SAMPLERATE = 8000.0F;
	private static final int SAMPLESIZE = 8;
	private static final int CHANNELS = 1;
	private static final boolean SIGNED = true;
	private static final boolean BIGENDIAN = true;
	private static final int AUDIOBUFFERSIZE = 400;//Should give about 50 ms latency with a sample rate of 8 kHz
	
	private boolean isRunning;//Used when disconnecting or closing to determine if the application is doing something
	
	private int thisPort;//The port this application listens to
	private String host;//Hostname of other application
	private int port;//Port of other application
	
	private JTextField hostField;
	private JTextField portField;
	private JButton connectButton;
	private JButton disconnectButton;
	private JTextArea log;
	
	private DatagramSocket socket;
	
	private TargetDataLine lineIn;
	private SourceDataLine lineOut;
	private AudioFormat audioFormat;
	/**
	 * Constructor. Attempts to start this application with the default port number 51302. Calls the method {@link #setupApp} in the end to complete the constructing. If the default port number 51302 is already occupied, the application will attempt to connect with another port above it until a free one is found. If no free port is found up to and including 65535, the ports below 51302 will be tried, starting with 51301 and going down to 1. If no port will be found there either a message will be shown to the user, and the application will terminate.
	 */
	public SimpAudioChatUDP() {
		/*
		 * Set up socket. 
		 */
		this.thisPort = 51302;
		boolean loop = true;
		while(loop) {
			try {
				this.socket = new DatagramSocket(this.thisPort);
				this.socket.setSoTimeout(30000);
				loop = false;
			} catch (SocketException e) {
				if(this.thisPort >= 51302)
					this.thisPort++;
				else if(this.thisPort < 51302)
					this.thisPort--;
				else if(this.thisPort == 65535)
					this.thisPort = 51301;
				else if(this.thisPort == 1) {
					this.showErrorDialog("Unable to start application", "No free port could be found for this application to listen to. A free port is necessary for this application to work. Try closing some other applications connected to the Internet and then try running this application again.");
					System.exit(209);
				}
			}
		}
		
		this.setupApp();
	}
	/**
	 * Constructor. Attempts to start this application with the given port number. Calls the method {@link #setupApp} in the end to complete the constructing. If it fails a message is shown to the user, and then the application is terminated (or not started at all if you want to see it like that).
	 * @param thisPort the port this application is supposed to listen to.
	 */
	public SimpAudioChatUDP(int thisPort) {
		/*
		 * Set up socket. If port number is already occupied, the application will terminate.
		 */
		this.thisPort = thisPort;
		try {
			this.socket = new DatagramSocket(this.thisPort);
			this.socket.setSoTimeout(30000);
		} catch (SocketException e) {
			this.showErrorDialog("Unable to start application", "Port specied is already occupied. Try another port.");
		}
		
		this.setupApp();
	}
	/**
	 * Sets up this application. This method is called by both constructors to actually complete them. This is because both constructors initially shared so much code that it looked nicer this way. Mostly handles the GUI (fields, buttons, listeners, and so on), but also finds lines for the capturing and playback of the audio. If no available lines could be found, this application will close, after displaying a message to the user about it.
	 * @see #SimpAudioChatUDP()
	 * @see #SimpAudioChatUDP(int)
	 */
	private void setupApp() {
		/* Set up GUI */	
		this.setTitle("SimpAudioChat - listening on port " + this.thisPort);
		
		this.isRunning = false;
		
		this.hostField = new JTextField();
		this.portField = new JTextField();
		
		JLabel hostLabel = new JLabel("Host:");
		JLabel portLabel = new JLabel("Port:");
		
		this.connectButton = new JButton("Connect");
		this.connectButton.addActionListener(new ConnectButtonListener());
		this.disconnectButton = new JButton("Disconnect");
		this.disconnectButton.addActionListener(new DisconnectButtonListener());
		this.disconnectButton.setEnabled(false);
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = 1; 
		gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
		this.add(hostLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 0; gbc.weightx = 1;
		this.add(hostField, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
		this.add(portLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 0; gbc.weightx = 1;
		this.add(portField, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		this.add(connectButton, gbc);
		gbc.insets = new Insets(0,100,0,0);
		this.add(disconnectButton, gbc);
		gbc.insets = new Insets(0,0,0,0);
		this.log = new JTextArea();
		this.log.setLineWrap(true);
		this.log.setWrapStyleWord(true);
		this.log.setEditable(false);
		
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = 1; gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.BOTH;
		this.add(new JScrollPane(this.log), gbc);
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exit();
			}
		});
		/* Set up audio format */
		this.audioFormat = new AudioFormat(SimpAudioChatUDP.SAMPLERATE, 
										   SimpAudioChatUDP.SAMPLESIZE, 
										   SimpAudioChatUDP.CHANNELS, 
										   SimpAudioChatUDP.SIGNED, 
										   SimpAudioChatUDP.BIGENDIAN);
		//Find available lines
		try {
			this.lineIn = AudioSystem.getTargetDataLine(this.audioFormat);
		} catch (IllegalArgumentException iae) {
			this.showErrorDialog("Unable to start application", "Unable to find microphone supporting sound of following properties." + System.lineSeparator() + "Sample rate: 8 kHz" + System.lineSeparator() + "Sample size: 16 bit" + System.lineSeparator() + "Channels: 1 (mono)" + System.lineSeparator() + "With data being stored as signed and big-endian. Check soundcard.");
			System.exit(5021);
		} catch (LineUnavailableException e) {
			this.showErrorDialog("Unable to start application", "No microphone line available for this application. Try closing other applications using sound and then try running this application again.");
			System.exit(5022);
		}
		
		try {
			this.lineOut = AudioSystem.getSourceDataLine(this.audioFormat);
		} catch (IllegalArgumentException iae) {
			this.showErrorDialog("Unable to start application", "Unable to find speaker supporting sound of following properties." + System.lineSeparator() + "Sample rate: 8 kHz" + System.lineSeparator() + "Sample size: 16 bit" + System.lineSeparator() + "Channels: 1 (mono)" + System.lineSeparator() + "With data being stored as signed and big-endian. Check soundcard.");
			System.exit(5023);
		} catch (LineUnavailableException e) {
			this.showErrorDialog("Unable to start application", "No speaker line available for this application. Try closing other applications using sound and then try running this application again.");
			System.exit(5024);
		}
		
		//Show the GUI
		this.setSize(450,250);
		this.setVisible(true);
		this.printToLog("Startup! Listening on port " + this.thisPort + ".");
	}
	/**
	 * Prints a message to this application's log, so that the user may view it. The message is supposed to contain no delimiting from other messages, as this method adds time and a couple of characters as delimiters itself. If this method received "Hello!", the resulting print to the log would be:</br> 15:07:36>> Hello!</br> with a new line at the end.
	 * @param msg the message that is to be printed to this application's log.
	 */
	synchronized public void printToLog(String msg){
		String currTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
		this.log.append(currTime + ">> " + msg + System.lineSeparator());
		this.log.setCaretPosition(this.log.getDocument().getLength());
	}
	/**
	 * Attempts to connect this application with another application (or possibly itself) by using the host and port values specified by the user in the {#hostField} and {portField} JTextFields of this application. If either of these fields are empty, or contain bad input (such as non-numbers for the port), or if the socket cannot be created, a dialog box is shown and explains the problem. If it succeds in creating the socket, the fields and the connect button are disabled. Then the message "C SimpAudioChat" is sent to the host and port, and the same response is waited for. If anything other than "C SimpAudioChat" is received, or if no response is given in 30 seconds, the application does not attempt to connect with the other side, but rather reverts the state to that which it was before the connect-button was pressed. If "C SimpAudioChat was received, then the capturing and playback lines of this application are opened. If the opening fails, a message is sent to the other application telling it that this one is disconnecting. Then a message is given to the user indicating what failed, and this application disconnects. If the opening of the lines succeded, the lines are started and then the capturing and sending of audio, as well as the receiving and playback of audio, are started by creating two new threads: {@link AudioCapture#run()} and {@link AudioPlayback#run()}. 
	 */
	public void connect() {
		//Check for bad input
		String host = this.hostField.getText().trim();
		String port = this.portField.getText().trim();
		
		if(host.equals("")
		|| port.equals("")) {
			this.showErrorDialog("Unable to connect", "Hostname or port number fields may not be empty.");
			return;
		}
		try{
			Integer.parseInt(port);
		} catch(IllegalArgumentException iae) {
			this.showErrorDialog("Unable to connect", "Port number field may only contain numbers.");
			return;
		}
		//Input accepted
		
		this.host = host;
		this.port = Integer.parseInt(port);
		
		//Attempt to connect
		try {
			this.socket.connect(InetAddress.getByName(this.host), this.port);
		} catch (UnknownHostException e) {
			this.showErrorDialog("Unable to connect", "Problems connecting socket with host. Check host field.");
			this.host = null;
			this.port = -1;
			return;
		} catch (IllegalArgumentException iae) {
			this.showErrorDialog("Unable to connect", "Port number must between 1 and 65535. Check port field.");
			this.host = null;
			this.port = -1;
			return;
		}
		
		//Disable text fields and connect button
		this.hostField.setEnabled(false);
		this.portField.setEnabled(false);
		this.connectButton.setEnabled(false);
		//
		this.sendMessage("C SimpAudioChat");
		
		String receivedMessage = this.receiveMessage();
		if(receivedMessage.equals("C SimpAudioChat")) {
			//Connected
			this.disconnectButton.setEnabled(true);
			this.printToLog("Successfully connected with " + this.host + ":" + this.port + ".");
			this.isRunning = true;
			
			/* Start microphone and speaker */
			try {
				this.lineIn.open(this.audioFormat, SimpAudioChatUDP.AUDIOBUFFERSIZE);
			} catch (LineUnavailableException e) {
				this.sendMessage("D SimpAudioChat");
				this.showErrorDialog("Unable to start application", "No microphone line available for this application. Try closing other applications using sound and then try running this application again.");
				this.disconnect("No available microphone found.");
			}
			try {
				this.lineOut.open(this.audioFormat, SimpAudioChatUDP.AUDIOBUFFERSIZE);
			} catch (LineUnavailableException e) {
				this.sendMessage("D SimpAudioChat");
				this.showErrorDialog("Unable to start application", "No speaker line available for this application. Try closing other applications using sound and then try running this application again.");
				this.disconnect("No available speaker found.");
			}
			
			this.lineIn.start();
			this.lineOut.start();
			
			new Thread(new AudioPlayback()).start();
			new Thread(new AudioCapture()).start();
		}
		else {
			//Don't connect
			String badOrNo = "No response from other side within 30 seconds.";
			if(receivedMessage.equals(""))
				badOrNo = "Bad response from other side.";
			this.showErrorDialog("Unable to connect", badOrNo + " Make sure there is a SimpAudioChat running on " + this.host + " that is listening on port " + this.port + ".");	
			this.host = null;
			this.port = -1;
			this.socket.disconnect();
			this.hostField.setEnabled(true);
			this.portField.setEnabled(true);
			this.connectButton.setEnabled(true);
			return;
		}
		
	}
	/**
	 * Disconnects this application from the other application. First, there is a check to see if the application is actually connected. This check, along with this method being synchronized will prevent the application from disconnecting twice (which causes problems with fields being null, and such), which may be caused if the application was connected to itself, or if a message was received to disconnect at the same time as this user decided to disconnect.
	 * 
	 * The actual disconnection is done by clearing the audio capturing and playback lines from data with {@link TargetDataLine#flush()} and {@link SourceDataLine#flush()}. Then a message is printed to the log of this application, to show that it is disconnecting. Then the socket is disconnected, and the host and port values of this application are resetted. Lastly, the buttons and fields are enabled and disabled.
	 * @param reason the reason this application is disconnecting, as in, what caused it to want to disconnect - the source. Examples: "User pressed disconnect-button.", "Other application disconnected."
	 */
	synchronized public void disconnect(String reason) {
		if(this.isRunning) {//This check, along with this method being "synchronized" will prevent one application from disconnecting twice (which causes problems). An application might disconnect twice if it was connect with itself (disconnecting once as per request of the user, and disconnecting twice because it received the message "D SimpAudioChat" from itself).
			//Stop the audio capture and playback
			this.isRunning = false;
			this.lineIn.flush();
			this.lineOut.flush();
			//
			this.printToLog("Disconnected from " + this.host + ":" + this.port + ". Reason: " + reason);
			//
			this.socket.disconnect();
			this.host = null;
			this.port = -1;
			//Enable / Disable buttons
			this.disconnectButton.setEnabled(false);
			this.connectButton.setEnabled(true);
			this.hostField.setEnabled(true);
			this.portField.setEnabled(true);
		}
	}
	/**
	 * Closes this application. First check if this application is connected to another, and if it is, sends a message to the other application that this one is disconnecting, then calls the {@link #disconnect(String)}-method to end this session. Then, and regardless if this application was connected, the socket is closed, and lastly the application is terminated.
	 */
	public void exit() {
		if(this.isRunning) {
			//Let connected application know that we're disconnecting
			this.sendMessage("D SimpAudioChat");
			//Stop this application from capturing and playing audio, also disconnect it from other application.
			this.disconnect("Exiting application.");
		}
		//Close the socket before exiting
		this.socket.close();
		//Exit application
		System.exit(0);
	}
	/**
	 * Attempts to send a message to the other application by using {@link #sendData(byte[])}. The message is being encoded with UTF-8, and then sent as an array of bytes to the {@link #sendData(byte[])}-method. If it was unable to encode to UTF-8, no message will be sent, but rather a message will be printed to the log of this application, explaining that it was unable to encode to UTF-8.
	 * @param message the message that is to be sent.
	 */
	public void sendMessage(String message){
		try {
			this.sendData(message.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			this.printToLog("Unable to encode message being sent as UTF-8.");
		}
	}
	/**
	 * Attempts to send data to the other application using this application's socket. If it was unable to send the data, a message will be printed to this application's log. The data is not manipulated within this method.
	 * @param data the data, probably a message or audio, to be sent.
	 */
	public void sendData(byte[] data) {
		DatagramPacket packet;
		try {
			packet = new DatagramPacket(data, data.length, InetAddress.getByName(this.host), this.port);
			this.socket.send(packet);
		} catch (UnknownHostException e) {
			this.printToLog("Unable to send data to host. Check host name and try reconnecting.");
		} catch (IOException e) {
			//Do nothing
		}	
		
	}
	/**
	 * Attempts to receive a message from the other application by using the packet generated by the{@link #receivePacket()}-method. The data in this packet is than decoded with UTF-8, and the resulting String is returned. If it was unable to decode as UTF-8, a message is printed to the log of this application, and null will be returned. 
	 * @return the UTF-8 decoded data from the received data from the other application.
	 */
	public String receiveMessage() {
		DatagramPacket packet = this.receivePacket();
		String receivedMessage = null;
		try {
			receivedMessage = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.printToLog("Unable to decode received message as UTF-8.");
		}
		return receivedMessage;
	}
	/**
	 * Attempts to receive a packet from the connected application. The message received may be audio, or may be a message indicating that it disconnected, or that it wants to connect. If no message is received within 30 seconds, it is assumed that the other application has disconnected, and this application is disconnected too. If there was nothing listening on the port this socket is connected to, this application disconnects aswell (if it was connected). This method doesn't actually return the data, but rather return the packet after doing {@link DatagramSocket#receive(DatagramPacket)}. It is up to the caller of this method to get the data out of the packet, because some parts wants the data, while other want the packet.
	 * @return the packet containing the received data.
	 * @see #receiveMessage()
	 */
	public DatagramPacket receivePacket() {
		byte[] buffer = new byte[65507];
		DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
		try {
			socket.receive(receivePacket);

		} catch (SocketTimeoutException ste) {
			if(this.isRunning) {
				this.showErrorDialog("Disconnecting", "No data received from other application for 30 seconds. Try reconnecting.");
				this.disconnect("Other application timed out.");
			}
		} catch (PortUnreachableException pue) {
			if(this.isRunning) {
				this.showErrorDialog("Disconnecting",  "Port on other application's machine was unreachable. This may have occured because the application was unexpectedly terminated. Try reconnecting.");
				this.disconnect("Port on other application's machine was unreachable.");
			}
		} catch (IOException e1) {
			this.sendMessage("D SimpAudioChat");
			System.err.println("Unexpected problem occured. Exiting...");
			this.exit();
		}
		return receivePacket;
	}
	/**
	 * Creates a dialog box using {@link JOptionPane} of the error-type where an error message can be viewed. The usage may be because the user has entered some faulty information (non-number as port, no information at all, and so on), or due because there were problems connecting with the other application (such as no response). The text has line-wrap to avoid the dialog box being far too wide.
	 * @param title title of the dialog box.
	 * @param message content of the dialog box, explaining the error.
	 */
	public void showErrorDialog(String title, String message) {
		JOptionPane.showMessageDialog(this, "<html><body><p style='width: 200px;'> " + message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Main-method of this application and class. Expects either zero or one arguments. If no arguments, this application will start and try to find an open port. It starts by looking at port 51302, and then goes up until 65535, and then tries the ports below 51302, until 1. If no open port is found, the application will terminate. If one argument is given, and it is a number, the application will start and attempt to use the given port number. If the port number is occupied, or if the port number WASN'T a number, the application will terminate. If more than one arguments are given, the application will terminate.
	 * @param args arguments given when running this class.
	 */
	public static void main(String[] args) {
		if(args.length == 0){
			new SimpAudioChatUDP();
		}
		else if(args.length == 1)
			try {
				new SimpAudioChatUDP(Integer.parseInt(args[0]));
			} catch (IllegalArgumentException iae) {
				System.err.println("Port specified was not a number. Exiting...");
				System.exit(242);
			}
		else if(args.length > 1) {
			System.err.println("Too many arguments given. Give only one. Exiting...");
			System.exit(700);
		}
	}
	/**
	 * Class for capturing audio from a microphone. Implements Runnable as it has to do the capturing in its own thread to avoid stuttering or a frozen application.
	 * @author simon
	 *
	 */
	public class AudioCapture implements Runnable{
		/**
		 * Captures audio from a microphone, represented by {@link SimpAudioChatUDP#lineIn}, and sends that data to the other application via this application's socket. A buffer is created of the size specified by {@link SimpAudioChatUDP#AUDIOBUFFERSIZE}, which will hold the captured data. It then enters a loop where the actual capturing takes place. It there was an audio captured, it will send that audio using the {@link SimpAudioChatUDP#sendData(byte[])-method. This is the repeated. The loop keeps going until {@link SimpAudioChatUDP#isRunning} turns false, upon which the line will be stopped and closed. This method runs in its own thread.
		 */
		public void run() {
			byte[] buffer = new byte[SimpAudioChatUDP.AUDIOBUFFERSIZE];
			while(isRunning) {
				int readBytes = lineIn.read(buffer, 0, buffer.length);
				if(readBytes > 0) {
					//Data was read from microphone
					sendData(buffer);
				}
			}
			lineIn.stop();
			lineIn.close();
		}
	}
	/**
	 * Class for playing the data received from the other application. Implements Runnable as it has to do the playback in its own thread to avoid stuttering or a frozen application.
	 * @author simon
	 *
	 */
	public class AudioPlayback implements Runnable{
		/**
		 * Plays the data received from the other application in its own thread. A buffer is created of the size specified by {@link SimpAudioChatUDP#AUDIOBUFFERSIZE}, which will hold the data received. This data is then read, and if there was anything to process it will first be checked if the sent data corresponds to the message indicating that the other application disconnected, "D SimpAudioChat", which will mean that this application should disconect too. If this check fails, it is assumed that it was audio that was sent. The audio is then sent to, and actually played by, the line the line specified by {@link SimpAudioChatUDP#lineOut}. The playback is done in a while loop which loops as long as the boolean {@link SimpAudioChatUDP#isRunning} is true. When the loop finally ends, upon disconnection, the line is stopped and closed. This method runs in its own thread.
		 */
		public void run() {
			byte[] buffer = new byte[SimpAudioChatUDP.AUDIOBUFFERSIZE];
			while(isRunning) {
				ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket().getData());
				int readBytes = bais.read(buffer, 0, buffer.length);
				if(readBytes > 0) {
					/*
					 * The received message may actually be a message telling this application that the other application disconnected. If it is so, disconnect this application, otherwise play the sound as usual.
					 */
					try {
						if(new String(buffer, 0, buffer.length, "UTF-8").substring(0,15).equals("D SimpAudioChat")) {
							showErrorDialog("Disconnecting", "Other application disconnected.");
							disconnect("Other application disconnected.");
						}
						else
							lineOut.write(buffer, 0, readBytes);
					} catch (UnsupportedEncodingException e) {
						//Try playing it as sound :^
						lineOut.write(buffer, 0, readBytes);
					}
				}
			}
			lineOut.stop();
			lineOut.close();
		}
	}
	/**
	 * Class for listening on the connect-button.
	 * @author simon
	 *
	 */
	public class ConnectButtonListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent e) {
			new Thread(this).start();
		}
		/**
		 * Calls the {@link SimpAudioChatUDP#connect()}-method. This method runs in its own thread.
		 */
		public void run() {
			connect();
		}
	}
	/**
	 * Class for listening on the disconnect-button.
	 * @author simon
	 *
	 */
	public class DisconnectButtonListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent e) {
			new Thread(this).start();
		}
		/**
		 * Sends a message to the connected application that this application is disconnecting, then calls the {@link SimpAudioChatUDP#disconnect(String)}-method to actually terminate the session. This method runs in its own thread.
		 */
		public void run() {
			//Let connected application know that we're disconnecting
			sendMessage("D SimpAudioChat");
			disconnect("User pressed disconnect-button.");
		}
	}
}