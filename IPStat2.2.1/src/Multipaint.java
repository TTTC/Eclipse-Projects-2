import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Multipaint extends JFrame implements Runnable{
	
	private DatagramSocket socket;
	private Canvas canvas;
	private Pencil pencil;
	private JPanel toolbar;
	private Set<PaintedPoint> paintedPoints;
	
	/**
	 * Constructor for the Multipaint class. Sets up a a DatagramSocket which only listens to the remort host and port specified. Also sets up the GUI for which to paint on. Lastly starts listening for messages from the other Multipaint application.
	 * @param thisPort the port which to listen for incoming DatagramPackets - this should be what the other application sets as its remotePort.
	 * @param remoteHost the host for the other application.
	 * @param remotePort the port for the other application - this should be what the ther application sets as its thisPort.
	 */
	public Multipaint(String thisPort, String remoteHost, String remotePort) {
		/* Establish Socket */
		try {
			socket = new DatagramSocket(Integer.parseInt(thisPort));
			socket.connect(InetAddress.getByName(remoteHost), Integer.parseInt(remotePort));
		} catch (NumberFormatException e) { 
			System.err.println("There were problems creating DatagramSocket. Closing...");
			System.exit(3);
		} catch (SocketException e) { 
			System.err.println("There were problems creating DatagramSocket. Closing...");
			System.exit(4);
		} catch (UnknownHostException e) {
			System.err.println("There were problems creating DatagramSocket. Closing...");
			System.exit(5);
		} catch (IllegalFormatException e) {
			System.err.println("Local port supplied is not a number. Closing...");
			System.exit(6);
		}
		/* Establish GUI */
		
		/*toolbar = new JPanel();
		toolbar.setBackground(Color.black);
		toolbar.setMaximumSize(new Dimension(100, 600));
		toolbar.setMinimumSize(new Dimension(100, 600));
		toolbar.setPreferredSize(new Dimension(100, 600));
		toolbar.setSize(100, 600);*/
		
		canvas = new Canvas();
		canvas.setMaximumSize(new Dimension(700, 600));
		canvas.setMinimumSize(new Dimension(700, 600));
		canvas.setPreferredSize(new Dimension(700, 600));
		canvas.setSize(700, 600);
		
		pencil = new Pencil();

		this.setLayout(new GridLayout());
		this.setSize(800,600);
		//this.add(toolbar);
		this.add(canvas);	

		this.setLocation(400,100);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	    this.setVisible(true);
	    
	    /* Go for it! Listen for packets! */
	    new Thread(this).start();
	}
	
	public static void main(String[] args) {
		if(args.length < 3 || args.length > 3) {
			System.err.println("You need to supply a local port, a remote host and remote port, and nothing else.");
			System.exit(2);
		}
		new Multipaint(args[0], args[1], args[2]);
	}
	/**
	 * Continously listens for packets from the other application. When it is received, the method converts it and extracts the data to create a new point on the canvas that should be painted.
	 */
	public void run() {
		while(true) {
			byte[] buffer = new byte[65507];
			DatagramPacket dgp = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(dgp);
			} catch (IOException e) {
				System.err.println("There were problems getting a packet. Closing...");
				System.exit(17);
			}
			String messageData = null;
			try {
				messageData = new String(dgp.getData(), 0, dgp.getLength(), "ASCII");
			} catch (UnsupportedEncodingException e) {
				System.err.println("There were problems reading message. Closing...");
				System.exit(18);
			}
			String[] data = messageData.split(" ");
			canvas.addPoint(new Point(Integer.parseInt(data[0]), Integer.parseInt(data[1])), new Color(Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4])), 2);
			
		}
	}
	/**
	 * Class for the paint-able area on the window. Handles the adding of points to this application, as well as sending data to the other application
	 * @author Simpn
	 *
	 */
	public class Canvas extends JPanel{
		
		public Canvas() {
			paintedPoints = new HashSet<PaintedPoint>();
			setBackground(Color.white);
		    addMouseListener(new mouseClickListener());
		    addMouseMotionListener(new mouseDraggedListener());
		}
		/**
		 * Adds a {@link PaintedPoint} which should be painted when the {@link Canvas#paintComponent(Graphics)}-method is called. This method is both used by the application when the user paints something, but also when the other application sends data about points to be painted. Synchronized so that it will not add a point while the method {@link Canvas#paintComponent(Graphics)} is iterating over the Hashset.
		 * @param p the point containing which coordinates the point on the canvas has.
		 * @param clr the colour which the pixel should have.
		 * @param sz size of the point.
		 */
		synchronized public void addPoint(Point p, Color clr, int sz) {
			PaintedPoint paintedPoint = new PaintedPoint(p, clr, sz);
			paintedPoints.add(paintedPoint);
			canvas.repaint();
			//toolbar.repaint();
		}
		/**
		 * Sends information about the points the user of this application is painting to the other application. Converts it to a string which is by the format "<code>x y r g b</code>", where x and y are the coordinates, and r, g and b are the amount of each colour to be painted which is taken from the pencil's current colour.
		 * @param p point containing the coordinates for the point to be painted.
		 */
		public void sendPoint(Point p){	
			Color clr = pencil.getColour();
			String message = Integer.toString(p.x) + " " + Integer.toString(p.y) + " " + clr.getRed() + " " + clr.getGreen() + " " + clr.getBlue();
			byte[] messageData = null;
			try {
				messageData = message.getBytes("ASCII");
			} catch (UnsupportedEncodingException e) {
				System.err.println("Problem encoding data packet. Closing...");
				System.exit(16);
			}
			DatagramPacket dgp = new DatagramPacket(messageData, messageData.length, socket.getInetAddress(), socket.getPort());
			try {
				socket.send(dgp);
			} catch (IOException e) {
				System.err.println("There were problems sending a packet. Closing...");
				System.exit(22);
			}
		}
		/**
		 * Paints the canvas. Goes through each PaintedPoint in the HashSet paintedPoints, and get's their coordinates, colour and size and then paints an oval on that location, with that colour and of that size. Synchronized to make sure there will be no ConcurrentModificationException thrown, because a point was added by the {@link Canvas#addPoint(Point, Color, int)}, while iterating the HashSet.
		 */
		synchronized public void paintComponent(Graphics g) {
			super.paintComponent(g);
			for(PaintedPoint paintedPoint : paintedPoints) {
				g.setColor(paintedPoint.getColour());
				Point p = paintedPoint.getPoint();
				int size = paintedPoint.getSize();
				g.fillOval(p.x-size/2, p.y-size/2, size, size);
			}
		}
		
		public class mouseClickListener extends MouseAdapter {
			public void mousePressed(MouseEvent me) {
				addPoint(me.getPoint(), pencil.getColour(), pencil.getSize());
				sendPoint(me.getPoint());
			}
		}
		public class mouseDraggedListener extends MouseMotionAdapter {
			public void mouseDragged(MouseEvent me) {
				addPoint(me.getPoint(), pencil.getColour(), pencil.getSize());
				sendPoint(me.getPoint());
			}
		}
	}
	/**
	 * Class representing the pencil used for painting. Keeps track of colour and size of the pencil.
	 * @author Simpn
	 *
	 */
	public class Pencil {
		Color colour;
		int size;
		
		public Pencil() {
			colour = new Color(0,0,0);
			size = 2;		
		}
		public void setColour(Color colour) {
			this.colour = colour;
		}
		public void setSize(int size) {
			this.size = size;
		}
		public Color getColour() {
			return colour;
		}
		public int getSize() {
			return size;
		}
	}
	/**
	 * Class representing a point on the canvas which should be painted. Keep track of coordinates, colour and size.
	 * @author Simpn
	 *
	 */
	public class PaintedPoint {
		private Point point;
		private Color colour;
		private int size;
		
		public PaintedPoint(Point point, Color colour, int size) {
			this.point = point;
			this.colour = colour;
			this.size = size;
		}
		public Color getColour() {
			return colour;
		}
		public Point getPoint() {
			return point;
		}	
		public int getSize() {
			return size;
		}
	}
}