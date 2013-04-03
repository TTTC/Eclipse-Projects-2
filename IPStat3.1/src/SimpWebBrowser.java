import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
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

public class SimpWebBrowser extends JFrame{
	JTextArea textDisplayArea;
	JTextField addressBar;
	
	public SimpWebBrowser() {
		
		/* Establish GUI */
		super("SimpWebBrowser");
		
		
		addressBar = new JTextField(Integer.MAX_VALUE);
		addressBar.setMaximumSize(addressBar.getPreferredSize());
		addressBar.addActionListener(new AddressBarListener());
		
		textDisplayArea = new JTextArea();
		textDisplayArea.setEditable(false);
		
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
	
	class AddressBarListener implements ActionListener, Runnable{
		public void actionPerformed(ActionEvent arg0) {
			new Thread(this).start();
		}

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
