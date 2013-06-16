import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SimpWebCameraStreamer extends JFrame{
	private static final long serialVersionUID = 1L;
	private JTextField hostField;
	private JTextField portField;
	
	public SimpWebCameraStreamer() {
		/* Set up GUI */
		this.hostField = new JTextField();
		this.portField = new JTextField();
		
		JLabel hostLabel = new JLabel();
		JLabel portLabel = new JLabel();
		JButton connectButton = new JButton("Send");
		connectButton.addActionListener(new ConnectButtonListener());
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		this.add(hostLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
		this.add(hostField, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		this.add(portLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
		this.add(portField, gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		this.add(connectButton, gbc);
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridheight = gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.BOTH;
		//this.add(new JScrollPane(log), gbc);
		
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);//TODO
		this.setSize(640,480);
		this.setVisible(true);
	}
	public class ConnectButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
