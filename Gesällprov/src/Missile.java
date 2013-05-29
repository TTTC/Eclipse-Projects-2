public class Missile extends Weapon{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int DAMAGE = 20;
	protected static final int SPEED = 7;
	
	public Missile(double x, double y, int angle){
		super(x, y, angle);
		this.loadImage("Images/Missile1.png");
	}
	public Missile(double x, double y, int angle, int hash){
		super(x, y, angle, hash);
		this.loadImage("Images/Missile1.png");
	}
	
	public void moveAndRotate(){
		this.x += SPEED * Math.cos(Math.toRadians(angle));
		this.y += SPEED * Math.sin(Math.toRadians(angle));
	}
	
	public String toMessage(){
		return "Missile " + super.toMessage() + " " + hash;
	}
	
	public int getDamage(){
		return DAMAGE;
	}
}
