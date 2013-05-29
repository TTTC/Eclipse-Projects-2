import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public abstract class SimpGameEntity extends Rectangle{
	protected static final long serialVersionUID = 1L;//Default auto-generated
	protected BufferedImage image = null;
	protected double x;
	protected double y;
	protected int angle;
	protected int hash;
	
	public SimpGameEntity(double x, double y, int angle){
		this.x = x;
		this.y = y;
		this.angle = angle;
		
		this.hash = (int)(x + y);
			int modValue = 60;
			if(this instanceof Weapon){
				if(this instanceof Shell)
					modValue *= 1;
				else if(this instanceof SuperShell)
					modValue *= 2;
				else if(this instanceof Mortar)
					modValue *= 3;
				else if(this instanceof Missile)
					modValue *= 4;
				else if(this instanceof Mine)
					modValue *= 5;
			}
			else if(this instanceof WeaponItem){
				if(this instanceof SuperShellItem)
					modValue *= 6;
				else if(this instanceof MortarItem)
					modValue *= 7;
				else if(this instanceof MissileItem)
					modValue *= 8;
				else if(this instanceof MineItem)
					modValue *= 9;
			}
			this.hash = ((this.hash % modValue) + modValue);
	}
	public SimpGameEntity(double x, double y, int angle, int hash){
		this(x, y, angle);
		this.hash = hash;
	}
	
	protected void loadImage(String filePath){
		try {
			this.image = ImageIO.read(new File(filePath));
		} catch (IOException e) {
			System.err.println("Unable to load image \"" + filePath + "\" - exiting");
			System.exit(404);
		}
	}
	
	public String toMessage(){
		return x + " " + y + " " + angle;
	}
	
	public void paint(Graphics2D g2){
		AffineTransform matrix = g2.getTransform();
		g2.rotate(Math.toRadians(angle), x+image.getHeight()/2, y+image.getWidth()/2);
		g2.drawImage(image, (int)x,(int) y, null);
		g2.setTransform(matrix);
	
		/* The code below prints colored dots at the corners of an object
		 * Keep it commented-out unless debugging */
		/*double[][] cornerArr = this.getCorners();
		Color clr = g2.getColor();
		g2.setColor(Color.RED);
		g2.fillRect((int)cornerArr[0][0], (int)cornerArr[0][1], 2, 2);
		g2.setColor(Color.GREEN);
		g2.fillRect((int)cornerArr[1][0], (int)cornerArr[1][1], 2, 2);
		g2.setColor(Color.BLUE);
		g2.fillRect((int)cornerArr[2][0], (int)cornerArr[2][1], 2, 2);
		g2.setColor(Color.YELLOW);
		g2.fillRect((int)cornerArr[3][0], (int)cornerArr[3][1], 2, 2);
		g2.setColor(Color.DARK_GRAY);
		g2.fillRect((int)this.x, (int)this.y, 4, 4);
		g2.setColor(clr);*/
	}
	public abstract void moveAndRotate();//If you want to handle some kind of movement

	public double[][] getCorners(){
		double[][] cornerArr = new double[4][2];
		double radians = Math.toRadians(-this.angle);
		/* Returns (x,y)-coordinates of each corner
		 * for this entity. Assumes a rectangle. */
		
		//Top-left
		cornerArr[0][0] = (this.x + this.getHeight()/2) + ((-this.getWidth()/2) * Math.sin(radians) + (-this.getHeight()/2) * Math.cos(radians));
		cornerArr[0][1] = (this.y + this.getWidth()/2) + ((-this.getWidth()/2) * Math.cos(radians) - (-this.getHeight()/2) * Math.sin(radians));
		//Top-right
		cornerArr[1][0] = (this.x + this.getHeight()/2) + ((-this.getWidth()/2) * Math.sin(radians) + (this.getHeight()/2) * Math.cos(radians));
		cornerArr[1][1] = (this.y + this.getWidth()/2) + ((-this.getWidth()/2) * Math.cos(radians) - (this.getHeight()/2) * Math.sin(radians));
		//Bottom-left
		cornerArr[2][0] = (this.x + this.getHeight()/2) + ((this.getWidth()/2) * Math.sin(radians) + (-this.getHeight()/2) * Math.cos(radians));
		cornerArr[2][1] = (this.y + this.getWidth()/2) + ((this.getWidth()/2) * Math.cos(radians) - (-this.getHeight()/2) * Math.sin(radians));
		//Bottom-right
		cornerArr[3][0] = (this.x + this.getHeight()/2) + ((this.getWidth()/2) * Math.sin(radians) + (this.getHeight()/2) * Math.cos(radians));
		cornerArr[3][1] = (this.y + this.getWidth()/2) + ((this.getWidth()/2) * Math.cos(radians) - (this.getHeight()/2) * Math.sin(radians));
		
		return cornerArr;
	}
	
	public int hashCode(){
		return hash;
	}
	public double getXCoord(){
		return this.x;
	}
	public double getYCoord(){
		return this.y;
	}
	public double getX(){
		return this.x;
	}
	public double getY(){
		return this.y;
	}
	public double getHeight(){
		return this.image.getHeight();
	}
	public double getWidth(){
		return this.image.getWidth();
	}
	public int getAngle(){
		return this.angle;
	}
	public double getMinX(){
		return this.image.getMinX();	
	}
	public double getMinY(){
		return this.image.getMinY();
	}
	public void setX(double x){
		this.x = x;
	}
	public void setY(double y){
		this.y = y;
	}
	public void setAngle(int angle){
		this.angle = angle;
	}	
}
