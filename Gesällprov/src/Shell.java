public class Shell extends Weapon{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int DAMAGE = 15;
	protected static final int SPEED = 5;
	protected int framesToLive;
	
	public Shell(double x, double y, int angle){
		super(x, y, angle);
		this.framesToLive = 60;
		this.loadImage("Images/Shell1.png");
	}
	public Shell(double x, double y, int angle, int framesToLive){
		this(x, y, angle);
		this.framesToLive = framesToLive;
	}
	public Shell(double x, double y, int angle, int framesToLive, int hash){
		super(x, y, angle, hash);
		this.framesToLive = framesToLive;
		this.loadImage("Images/Shell1.png");
	}
	public void moveAndRotate(){
		//super.moveAndRotate();
		
		this.x += SPEED * Math.cos(Math.toRadians(angle));
		this.y += SPEED * Math.sin(Math.toRadians(angle));
		
		this.framesToLive--;//Reduced at every frame.
	}
	public String toMessage(){
		return "Shell " + super.toMessage() + " " + framesToLive + " " + hash;
	}
	
	public int getFramesToLive(){
		return this.framesToLive;
	}
	public void setFramesToLive(int framesToLive){
		this.framesToLive = framesToLive;
	}
	
	public int getDamage(){
		return DAMAGE;
	}
	
}
