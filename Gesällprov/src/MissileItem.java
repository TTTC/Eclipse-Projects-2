public class MissileItem extends WeaponItem {
	
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MissileItem(double x, double y){	
		super(x, y);
		this.loadImage("Images/MissileItem1.png");
	}
	public MissileItem(double x, double y, int hash){
		super(x, y, hash);
		this.loadImage("Images/MissileItem1.png");
	}
	public String toMessage(){
		return "MissileItem " + super.toMessage() + " " + hash;
	}
	
	public int ammoType(){
		return 3;
	}
}
