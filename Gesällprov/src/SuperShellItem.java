public class SuperShellItem extends WeaponItem {
	
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public SuperShellItem(double x, double y){
		super(x, y);
		this.loadImage("Images/SuperShellItem1.png");
	}
	public SuperShellItem(double x, double y, int hash){
	super(x, y, hash);
	this.loadImage("Images/SuperShellItem1.png");
}
	public String toMessage(){
		return "SuperShellItem " + super.toMessage() + " " + hash;
	}
	
	public int ammoType(){
		return 1;
	}
}
