import java.net.InetAddress;

public class Player{
		InetAddress address;
		int port;
		String name;
		
		public Player(InetAddress address, int port, String name){
			this.address = address;
			this.port = port;
			this.name = name;
		}
	}