import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
/**
 * Class for downloading and viewing HTML-documents. Has a quite minimalist GUI - just a text field to enter the adress, and a text field to view the document - no button. The HTML-code is not really processed, but just presented as text.
 */
public class SimpWebBrowser extends JFrame{
	private static final long serialVersionUID = 1L;
	private JTextArea textDisplayArea;
	private JTextField addressBar;
	/**
	 * Constructor. Sets up the two fields in the application, as well as the listener for the enter button.
	 */
	public SimpWebBrowser() {
		
		/* Establish GUI */
		super("SimpWebBrowser");
		
		
		this.addressBar = new JTextField(Integer.MAX_VALUE);
		this.addressBar.setMaximumSize(addressBar.getPreferredSize());
		this.addressBar.addActionListener(new AddressBarListener());
		
		this.textDisplayArea = new JTextArea();
		this.textDisplayArea.setEditable(false);
		
		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(),BoxLayout.Y_AXIS));
		this.setSize(800,600);
		this.add(addressBar);
		this.add(new JScrollPane(textDisplayArea));
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	public static void main(String[] args) {
		new SimpWebBrowser();
	}
	
	/**
	 * Class listening for the enter-key on the user's keyboard. If user has focus on the JTextField, and presses enter {@link this#actionPerformed(ActionEvent)}.
	 * @author simon
	 *
	 */
	class AddressBarListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent arg0) {
			new Thread(this).start();
		}
		/**
		 * Reads the document and prints it to the text area in SimpWebBrowser that this listener is connected to. Runs in its own thread to avoid the "locking out" interaction from the user while the document is read.
		 */
		synchronized public void run() {
			URL url = null;
			BufferedReader reader = null;
			try {
				url = new URL(addressBar.getText());
				URLConnection urlConnection = url.openConnection();
				reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				
				textDisplayArea.setText("");
				String read;
				try {
					while((read = reader.readLine()) != null) 
						textDisplayArea.append(read + System.lineSeparator());
				} catch (IOException e) {
					textDisplayArea.setText("");
					textDisplayArea.setText("Error reading data from server.");
				}
			} catch (MalformedURLException e) {
				textDisplayArea.setText("");
				textDisplayArea.setText("Error connecting to server.");
			} catch (IOException e) {
				textDisplayArea.setText("");
				textDisplayArea.setText("Error connecting to server.");
			}		
		}
	}
}
