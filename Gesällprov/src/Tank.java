import java.awt.Graphics2D;
public class Tank extends SimpGameEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected final static int SPEED = 1;
	protected int hp;
	protected int movement;
	protected int rotatement;
	protected int cooldown;
	
	public Tank(double x, double y, int angle){
		super(x, y, angle);
		this.hp = 100;
		this.movement = 0;
		this.rotatement = 0;
		this.cooldown = 0;
		this.loadImage("Images/Tank1.png");
	}
	public Tank(double x, double y, int angle, int hp, int movement, int rotatement){
		super(x, y, angle);
		this.hp = hp;
		this.movement = movement;
		this.rotatement = rotatement;
		this.cooldown = 0;
		this.loadImage("Images/Tank1.png");
	}
	public Tank(double x, double y, int angle, int hp, int movement, int rotatement, int hash){
		super(x, y, angle, hash);
		this.hp = hp;
		this.movement = movement;
		this.rotatement = rotatement;
		this.cooldown = 0;
		this.loadImage("Images/Tank1.png");
	}
	
	public void paint(Graphics2D g2){
		if(this.hp > 0)//Only paint if alive
			super.paint(g2);
	}
	
	public void moveAndRotate(){
		if(cooldown > 0)
			cooldown--;
		
		if(movement == 1){
			/* Move forward */
			this.x += SPEED * Math.cos(Math.toRadians(angle));
			this.y += SPEED * Math.sin(Math.toRadians(angle));
		}
		else if(movement == 2){
			/* Move backward */
			this.x -= SPEED * Math.cos(Math.toRadians(angle));
			this.y -= SPEED * Math.sin(Math.toRadians(angle));
		}
		if(rotatement == 1){
			/* Rotate left */
			angle -= SPEED;
		}
		else if(rotatement == 2){
			/* Rotate right */
			angle += SPEED;
		}
	}
	
	public String toMessage(){
		/*
		 * x y angle hp movement rotatement
		 * Example of a tank at coords 50,50, 
		 * at a 145 degree angle, moving 
		 * backwards, and not rotating: 
		 * Tank 50 50 145 100 2 0 */
		return "Tank " + super.toMessage() + " " + hp + " " + movement + " " + rotatement + " " + hash;
	}
	
	public int getHP(){
		return this.hp;
	}
	public int getMovement(){
		return this.movement;
	}
	public int getRotatement(){
		return this.rotatement;
	}
	public int getCooldown(){
		return this.cooldown;
	}
	public void setHP(int hp){
		this.hp = hp;
	}
	public void setMovement(int movement){
		/*
		 * 0 = stop moving
		 * 1 = forward
		 * 2 = backward
		 */
		this.movement = movement;
	}
	public void setRotatement(int rotatement){
		this.rotatement = rotatement;
		/*
		 * 0 = stop rotating
		 * 1 = left
		 * 2 = right
		 */
	}
	public void setCooldown(int cooldown){
		this.cooldown = cooldown;
	}
}
