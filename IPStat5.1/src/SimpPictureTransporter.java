import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Class for sending images to another application over the ,.-+*INTERNET*+-.,. . Images are supposed to be sent to and received from other SimpPictureTransporter-applications. This application continously listens for messages, and accepts them and attempts to retreive them and save them as images (which I assume may have some funny and tragic results). It also allows the user to specify a hostname and port, and a path to a file, which is to be sent.
 * @author simon
 *
 */
public class SimpPictureTransporter extends JFrame implements Runnable {

	private static final long serialVersionUID = 1L;
	private ServerSocket serverSocket;
	private JTextField sendHost;
	private JTextField sendPort;
	private JTextField sendPicture;
	private JTextArea log;//Displays information about pictures received and sent.
	
	/**
	 * Sets up the GUI. It also sets up the continous listening for messages to this application. The port listened to should be supplied as parameter. If this port is occupied, the application is ended. If the port is available, the application will start up and the user may interact with it. 
	 * @param listenPort the port to listen to. Default 5130 if user did not supply anything else when running the application.
	 */
	public SimpPictureTransporter(int listenPort){
		/* Start server socket */
		try {
			serverSocket = new ServerSocket(listenPort);
		} catch (IOException e) {
			System.err.println("Unable to establish connection to port " + listenPort);
			System.exit(5);
		}
		
		/* Set up GUI */
		sendHost = new JTextField();
		sendPort = new JTextField();
		sendPicture = new JTextField();
		log = new JTextArea();
		log.setLineWrap(true);
		log.setEditable(false);
		
		JLabel hostLabel = new JLabel("Receiver address: ");
		JLabel portLabel = new JLabel("Receiver port: ");
		JLabel pictureLabel = new JLabel("Path of picture: ");
		JLabel logLabel = new JLabel("Log: ");
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new SendButtonListener());
		
		JPanel sendPanel = new JPanel();
		sendPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		sendPanel.add(hostLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
		sendPanel.add(sendHost, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		sendPanel.add(portLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		sendPanel.add(sendPort, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		sendPanel.add(pictureLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
		sendPanel.add(sendPicture, gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		sendPanel.add(logLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		sendPanel.add(sendButton, gbc);
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.BOTH;
		sendPanel.add(new JScrollPane(log), gbc);
		
		this.add(sendPanel);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(640,480);
		this.setVisible(true);
		
		/* Start listening for requests */
		new Thread(this).start();
	}
	/**
	 * Class for listening to the send-button in {@link SimpPictureTransporter}. When pressed, a new thread is created where an image is attempted to be sent.
	 * @author simon
	 *
	 */
	public class SendButtonListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent e) {
			if(sendHost.getText().trim().equals("")
			|| sendPort.getText().trim().equals("")
			|| sendPicture.getText().trim().equals(""))
				return;
			
			new Thread(this).start();
		}
		/**
		 * Attempts to send a picture by the information give by the user. If the image does not exist, will do nothing. The receiver has to accept the request to send first, and probably won't unless sending to another {@link SimpPictureTransporter}-application.
		 */
		public void run() {
			/* Read the image */
			BufferedImage img = null;
			File file = null;
			try {
				file = new File(sendPicture.getText().trim());
				img = ImageIO.read(file);
			} catch (IOException e) {
				synchedLogPrintln("Unable to send picture; unable to read file.");
				return;
			}
			if(img == null){
				synchedLogPrintln("Unable to load image. Check path.");
				return;
			}
			/* Etablish connection */
			Socket socket = null;
			try {
				socket = new Socket(sendHost.getText().trim(), Integer.parseInt(sendPort.getText().trim()));
				
			} catch (NumberFormatException e) {
				return; //If user inputs non-number as port.
			} catch (UnknownHostException e) {
				synchedLogPrintln("Unable to send picture; unable to connect with receiver.");
				return;
			} catch (IOException e) {
				synchedLogPrintln("Unable to send picture; unable to connect with receiver.");
				return;
			}
			/* Send image to socket */
			OutputStream out = null;
			try {
				out = socket.getOutputStream();
				ImageIO.write(img, "JPG", out);
				out.flush(); //Remove?
				synchedLogPrintln("Successfully sent image " + file.getName() + " in folder " + file.getAbsolutePath().substring(0,file.getAbsolutePath().lastIndexOf(File.separator)) + " to " + socket.getInetAddress() + " (" + socket.getPort() + ")");
			} catch (IOException e) {
				synchedLogPrintln("Unable to send picture; unable to write data to receiver.");
			}
			finally{
				try{
					socket.close();
					out.close();
				}catch(IOException e){
					//Just end
				}
			}
			
		}
		
	}
	/**
	 * Continously listens for images sent to the port listened to, which is supplied when starting the application as an argument. When accepting a sent image, it will be saved in the same folder as this application is located or running from. The image is saved under the name "IMGx.png" where x is first available number, starting from 1.
	 */
	public void run() {
		Socket socket = null;
		InputStream in = null;
		File file = null;
		try {
			socket = serverSocket.accept();
			int i = 1;
			while((file = new File("IMG" + i + ".png")).exists())
				i++;//Repeats until a free filename is found.
			in = socket.getInputStream();
			BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(in));
			ImageIO.write(bi, "PNG", file);
			this.synchedLogPrintln("Successfully received image. Saved as IMG" + i + ".png in folder " + file.getAbsolutePath().substring(0,file.getAbsolutePath().lastIndexOf(File.separator)));
		} catch (IOException e) {
			synchedLogPrintln("Unable to receive image; unable to connect with sender");
		}
		finally{
			new Thread(this).start(); //Start a new thread that is ready to receive.
		}
		
	}
	/**
	 * Prints a message to the log in {@link SimpPictureTransporter}. This method takes care of all the delimiting from other messages by beginning each message with "##", and ending it with a line-break. It also makes sure that the log is scrolled down to view the new message.
	 * @param msg the message that is to be printed. Expected to be the message only, and no delimiting text.
	 */
	synchronized public void synchedLogPrintln(String msg){
		log.append("## "); //Shows where a new message begins
		log.append(msg);
		log.append(System.lineSeparator());
		log.setCaretPosition(log.getDocument().getLength());
	}
	/**
	 * Expects a port as argument to be used to listen to when running the application. If no port is given, default is 5130.
	 * @param args
	 */
	public static void main(String[] args){
		int listenPort = 5130; //Check what might use this port.
		if(args.length == 1)
			try{
				listenPort = Integer.parseInt(args[0]);
			}catch(IllegalArgumentException iae){
				System.err.println("Specified listening port argument is not a number.");
				System.exit(1);
			}
		else if(args.length > 1){
			System.err.println("Too many arguments specified on startup. Supply only listening port number, or nothing.");
			System.exit(2);
		}
		new SimpPictureTransporter(listenPort);
	}
}
