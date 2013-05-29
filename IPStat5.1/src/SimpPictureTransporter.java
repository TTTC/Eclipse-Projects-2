import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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


public class SimpPictureTransporter extends JFrame implements Runnable {
	ServerSocket serverSocket;
	JTextField sendHost;
	JTextField sendPort;
	JTextField sendPicture;
	JTextArea log;//Displays information about pictures received and sent.
	
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
	
	public class SendButtonListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent e) {
			if(sendHost.getText().trim().equals("")
			|| sendPort.getText().trim().equals("")
			|| sendPicture.getText().trim().equals(""))
				return;
			
			new Thread(this).start();
		}

		public void run() {
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
				try {
					socket.close();
				} catch (IOException e) {
					//Do nothing
				}
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
	
	synchronized public void synchedLogPrintln(String msg){
		log.append("## "); //Shows where a new message begins
		log.append(msg);
		log.append(System.lineSeparator());
		log.setCaretPosition(log.getDocument().getLength());
	}
	
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
