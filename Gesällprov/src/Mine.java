public class Mine extends Weapon{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int DAMAGE = 35;
	protected static final int SPEED = 0;
	
	public Mine(double x, double y){
		super(x, y, 0);
		this.loadImage("Images/Mine1.png");
	}
	
	public Mine(double x, double y, int hash){
		super(x, y, 0, hash);
		this.loadImage("Images/Mine1.png");
	}
	public void moveAndRotate(){
		//Do nothing - the mine is not supposed to move
	}
	
	public String toMessage(){
		return "Mine " + x + " " + y + " " + hash;
	}
	
	public int getDamage(){
		return DAMAGE;
	}
}
