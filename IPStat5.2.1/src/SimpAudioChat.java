import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
 * Class for an application which allows users to communicate by audio with each other. When starting this application, the main-method expects zero or one arguments (see {@link #main(String[])} for details). This application uses TCP to comminucate, but is based upon its UDP-variant SimpAudioChatUDP. To actually start a session, the host and port values of the other application needs to be entered into this application's fields, and the user must press the connect-button. There also needs to be an open application on the other side. While the other application needn't actually attempt to connect yet, it still will need to be open, otherwise the attempt to connect will be failed due to ICMP-error unreachable port.
 * 
 * While connected this application will continously capture audio in another thread, and send the captured audio to the other application. Audio received from the other application will also be played, in yet another thread. 
 * 
 * The audio has a sample rate of 8 kHz, sample size of 8 bits, 1-channeled (mono), and is stored as signed, big-endian data. The captured and retreived audio is in a buffer of 400 bytes, which should give an audio latency of about 50 ms (400 / 8000 = 0.05). The lines used for capture and playback of audio are received by the {@link AudioSystem#getTargetDataLine(AudioFormat)} and {@link AudioSystem#getSourceDataLine(AudioFormat)}. If there are no available lines for this format, the application terminates. 
 * 
 * Any time during the session a user may disconnect by pressing the disconnect-button. This causes the other application to sense it, so it will disconnect too. Then the state of the application is brought back to that of an unconnected application - also freeing the used lines for audio capture and playback. This application also disconnects if the other application timed out, or sent a message that it is disconnecting, or if there were some kind of problem.
 * @author simon
 *
 */
public class SimpAudioChat extends JFrame{
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
	
	private ServerSocket serverSocket;
	private Socket receiveSocket;
	private Socket sendSocket;
	private InputStream in;
	private OutputStream out;
	
	private TargetDataLine lineIn;
	private SourceDataLine lineOut;
	private AudioFormat audioFormat;
	/**
	 * Constructor. Attempts to start this application with the default port number 51302. Calls the method {@link #setupApp} in the end to complete the constructing. If the default port number 51302 is already occupied, the application will attempt to connect with another port above it until a free one is found. If no free port is found up to and including 65535, the ports below 51302 will be tried, starting with 51301 and going down to 1. If no port will be found there either a message will be shown to the user, and the application will terminate.
	 */
	public SimpAudioChat() {
		/*
		 * Set up socket. 
		 */
		this.thisPort = 51302;
		boolean loop = true;
		while(loop) {
			try {
				this.serverSocket = new ServerSocket(this.thisPort);
				this.serverSocket.setSoTimeout(30000);
				loop = false;
			} catch (IOException e) {
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
	 * Constructor. Attempts to start this application with the given port number. Calls the method {@link #setupApp} in the end to complete the constructing. If it fails to start a message is shown to the user, and then the application is terminated (or not started at all if you want to see it like that).
	 * @param thisPort the port this application is supposed to listen to.
	 */
	public SimpAudioChat(int thisPort) {
		/*
		 * Set up socket. If port number is already occupied, the application will terminate.
		 */
		this.thisPort = thisPort;
		try {
			this.serverSocket = new ServerSocket(this.thisPort);
			this.serverSocket.setSoTimeout(30000);
		} catch (IOException e) {
			this.showErrorDialog("Unable to start application", "Port specied is already occupied. Try another port.");
		}
		
		this.setupApp();
	}
	/**
	 * Sets up this application. This method is called by both constructors to actually complete them. This is because both constructors initially shared so much code that it looked nicer this way. Mostly handles the GUI (fields, buttons, listeners, and so on), but also finds lines for the capturing and playback of the audio. If no available lines could be found, this application will close, after displaying a message to the user about it.
	 * @see #SimpAudioChat()
	 * @see #SimpAudioChat(int)
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
		this.audioFormat = new AudioFormat(SimpAudioChat.SAMPLERATE, 
										   SimpAudioChat.SAMPLESIZE, 
										   SimpAudioChat.CHANNELS, 
										   SimpAudioChat.SIGNED, 
										   SimpAudioChat.BIGENDIAN);
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
	 * Attempts to connect this application with another application (or possibly itself) by using the host and port values specified by the user in the {#hostField} and {portField} JTextFields of this application. If either of these fields are empty, or contain bad input (such as non-numbers for the port), or if the sockets involved cannot be created, a dialog box is shown and explains the problem. If it succeds in creating the sockets, the fields and the connect button are disabled. If the connection is established, then the capturing and playback lines of this application are opened. If the opening of these lines fails, a message is given to the user indicating what failed, and this application disconnects. If the opening of the lines succeded, the lines are started and then the capturing and sending of audio, as well as the receiving and playback of audio, are started by creating two new threads: {@link AudioCapture#run()} and {@link AudioPlayback#run()}. 
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
			this.sendSocket = new Socket(this.host, this.port);
			this.sendSocket.setSoTimeout(30000);
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
		} catch (IOException e) {
			this.showErrorDialog("Unable to connect", "Problems creating socket. Check host and port field, adn then try reconnecting.");
			this.host = null;
			this.port = -1;
			return;
		}
		
		//Disable text fields and connect button
		this.hostField.setEnabled(false);
		this.portField.setEnabled(false);
		this.connectButton.setEnabled(false);
		//
		try {
			this.receiveSocket = this.serverSocket.accept();
			this.receiveSocket.setSoTimeout(30000);
			this.in = this.receiveSocket.getInputStream();
			this.out = this.sendSocket.getOutputStream();
		} catch (IOException e1) { 
			this.disconnect("Problem setting up the connection with the other application.");
			this.showErrorDialog("Unable to connect", "There were problems setting up the connections with the other side. Try re-connecting.");
		}
		
		//Connected
		this.disconnectButton.setEnabled(true);
		this.printToLog("Successfully connected with " + this.host + ":" + this.port + ".");
		this.isRunning = true;
		
		/* Start microphone and speaker */
		try {
			this.lineIn.open(this.audioFormat, SimpAudioChat.AUDIOBUFFERSIZE);
		} catch (LineUnavailableException e) {
			this.disconnect("No available microphone found.");
			this.showErrorDialog("Unable to connect", "No microphone line available for this application. Try closing other applications using sound and then try running this application again.");
		}
		try {
			this.lineOut.open(this.audioFormat, SimpAudioChat.AUDIOBUFFERSIZE);
		} catch (LineUnavailableException e) {
			this.disconnect("No available speaker found.");
			this.showErrorDialog("Unable to connect", "No speaker line available for this application. Try closing other applications using sound and then try running this application again.");
		}
	
		this.lineIn.start();
		this.lineOut.start();
		
		new Thread(new AudioPlayback()).start();
		new Thread(new AudioCapture()).start();
	}
	
	/**
	 * Disconnects this application from the other application. First, there is a check to see if the application is actually connected. This check, along with this method being synchronized will prevent the application from disconnecting twice (which causes problems with fields being null, and such), which may be caused if the application was connected to itself.
	 * 
	 * The actual disconnection is done by clearing the audio capturing and playback lines from data with {@link TargetDataLine#flush()} and {@link SourceDataLine#flush()}. Then a message is printed to the log of this application, to show that it is disconnecting. Then the sockets are closed, and the host and port values of this application are resetted. Lastly, the buttons and fields are enabled and disabled.
	 * @param reason the reason this application is disconnecting, as in, what caused it to want to disconnect - the source. Examples: "User pressed disconnect-button.", "Other application disconnected."
	 */
	synchronized public void disconnect(String reason) {
		if(this.isRunning) {//This check, along with this method being "synchronized" will prevent one application from disconnecting twice (which causes problems). An application might disconnect twice if it was connect with itself (disconnecting once as per request of the user, and disconnecting twice because it sensed the "other side" disconnected.
			//Stop the audio capture and playback
			this.isRunning = false;
			this.lineIn.flush();
			this.lineOut.flush();
			//
			this.printToLog("Disconnected from " + this.host + ":" + this.port + ". Reason: " + reason);
			//
			try {
				this.sendSocket.close();
				this.receiveSocket.close();
			} catch (IOException e) { }
			this.host = null;
			this.port = -1;
			this.in = null;
			this.out = null;
			//Enable / Disable buttons
			this.disconnectButton.setEnabled(false);
			this.connectButton.setEnabled(true);
			this.hostField.setEnabled(true);
			this.portField.setEnabled(true);
		}
	}
	/**
	 * Closes this application. First check if this application is connected to another, and if it is calls the {@link #disconnect(String)}-method to end this session. Then, and regardless if this application was connected, the application is terminated.
	 */
	public void exit() {
		if(this.isRunning) {
			//Stop this application from capturing and playing audio, also disconnect it from other application.
			this.disconnect("Exiting application.");
		}
		//Exit application
		System.exit(0);
	}
	/**
	 * Attempts to retreive the data stored in this application's receiving socket's input stream, which contains the audio sent by the other application. The data is read from the socket, and then returned as an array of bytes.
	 * @return the data received in this application's receiving socket's input stream, as an array of bytes.
	 */
	public byte[] receiveData() {
		byte[] data = new byte[SimpAudioChat.AUDIOBUFFERSIZE];
		try {
			this.in.read(data, 0, data.length);
		} catch (SocketTimeoutException ste) {
			if(this.isRunning) {
				this.showErrorDialog("Disconnecting", "No data received from other application for 30 seconds. Try reconnecting.");
				this.disconnect("Other application timed out.");
			}
		} catch (IOException e1) {
			System.err.println("Unexpected problem occured. Exiting...");
			this.exit();
		}
		return data;
	}
	/**
	 * Creates a dialog box using {@link JOptionPane} of the error-type where an error message can be viewed. The usage may be because the user has entered some faulty information (non-number as port, no information at all, and so on), or due because there were problems connecting with the other application (such as disconnection). The text has line-wrap to avoid the dialog box being far too wide.
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
			new SimpAudioChat();
		}
		else if(args.length == 1)
			try {
				new SimpAudioChat(Integer.parseInt(args[0]));
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
	 * @see AudioPlayback
	 */
	public class AudioCapture implements Runnable{
		/**
		 * Captures audio from a microphone, represented by {@link SimpAudioChat#lineIn}, and sends that data to the other application via this application's sending socket's output stream. A buffer is created of the size specified by {@link SimpAudioChat#AUDIOBUFFERSIZE}, which will hold the captured data. It then enters a loop where the actual capturing takes place. If there was audio captured, it will send that audio using the output stream of this application's sending socket, by writing the data stored in the buffer to the stream. This is the repeated. The loop keeps going until {@link SimpAudioChat#isRunning} turns false, upon which the line will be stopped and closed. It is worth pointing out that there might be an exception thrown when the data is written t the stream, which is probably thrown because the other side disconnected. Right now, this method does nothing to the exception other than catching it. The disconnection is handled in the method that captures the data. This method runs in its own thread.
		 */
		public void run() {
			byte[] buffer = new byte[SimpAudioChat.AUDIOBUFFERSIZE];
			while(isRunning) {
				int readBytes = lineIn.read(buffer, 0, buffer.length);
				if(readBytes > 0) {
					//Data was read from microphone
					try {
						out.write(buffer, 0, buffer.length);
					} catch (IOException e) {
						/* 
						 * This exception is probably thrown because something happened to the other side, and the connection has been lost. If so, the thread that receives audio from the other application will pick up on this, and disconnect from the other application. Since the capturing and receiving of audio is done in two different threads, this method may have realized that the other side disconnect before the other method. But it is not a problem, just continue as normal, and if the other method handles the disconnection from the other application, this method will stop looping when checking if isRunning is true.
						 */
					}
				}
			}
			lineIn.stop();
			lineIn.close();
		}
	}
	/**
	 * Class for playing the data received from the other application. Implements Runnable as it has to do the playback in its own thread to avoid stuttering or a frozen application.
	 * @author simon
	 * @see AudioCapture
	 */
	public class AudioPlayback implements Runnable{
		/**
		 * Plays the data received from the other application in its own thread. A buffer is created of the size specified by {@link SimpAudioChat#AUDIOBUFFERSIZE}, which will hold the data received in the stream that receives the audio. It is assumed that it was audio that was sent. The audio is then sent to, and actually played by, the line specified by {@link SimpAudioChat#lineOut}. The the reading of received data and audio playback is done in a while loop which loops as long as the boolean {@link SimpAudioChat#isRunning} is true. When the loop finally ends, upon disconnection, the line is stopped and closed. This method runs in its own thread.
		 */
		public void run() {
			byte[] buffer = new byte[SimpAudioChat.AUDIOBUFFERSIZE];
			int readBytes = 0;
			while(isRunning) {
					try {
						readBytes = in.read(buffer, 0, buffer.length);
					} catch (IOException e) {
						//Hm. Let's ignore this, and try to read again in the next loop-iteration. Leave a message though.
						printToLog("Problem reading audio from other application.");
					}
					if(readBytes > 0) {
						lineOut.write(buffer, 0, readBytes);				
					}
					else if(readBytes == -1) {
						//Other application disconnected
						if(isRunning) {
							disconnect("Other application disconnected.");
							showErrorDialog("Disconnecting", "Other application disconnected.");
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
		 * Calls the {@link SimpAudioChat#connect()}-method. This method runs in its own thread.
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
		 * Calls the {@link SimpAudioChat#disconnect(String)}-method. This method runs in its own thread.
		 */
		public void run() {
			disconnect("User pressed disconnect-button.");
		}
	}
}