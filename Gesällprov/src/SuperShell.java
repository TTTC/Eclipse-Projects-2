public class SuperShell extends Weapon{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int DAMAGE = 20;
	protected static final int SPEED = 9;
	
	public SuperShell(double x, double y, int angle){
		super(x, y, angle);
		this.loadImage("Images/SuperShell1.png");
	}
	public SuperShell(double x, double y, int angle, int hash){
		super(x, y, angle, hash);
		this.loadImage("Images/SuperShell1.png");
	}
	
	public void moveAndRotate(){
		this.x += SPEED * Math.cos(Math.toRadians(angle));
		this.y += SPEED * Math.sin(Math.toRadians(angle));
	}
	
	public String toMessage(){
		return "SuperShell " + super.toMessage() + " " + hash;
	}
	
	public int getDamage(){
		return DAMAGE;
	}
}
