
public abstract class Weapon extends SimpGameEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int DAMAGE = 1;
	protected static final int SPEED = 1;
	
	public Weapon(double x, double y, int angle){
		super(x, y, angle);
	}
	public Weapon(double x, double y, int angle, int hash){
		super(x, y, angle, hash);
	}
	
	public void moveAndRotate(){
		this.x += SPEED * Math.cos(Math.toRadians(angle));
		this.y += SPEED * Math.sin(Math.toRadians(angle));
	}
	
	public abstract int getDamage();
}
