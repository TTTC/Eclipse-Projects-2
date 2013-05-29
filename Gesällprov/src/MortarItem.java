public class MortarItem extends WeaponItem {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MortarItem(double x, double y){
		super(x, y);
		this.loadImage("Images/MortarItem1.png");
	}
	public MortarItem(double x, double y, int hash){
		super(x, y, hash);
		this.loadImage("Images/MortarItem1.png");
	}
	
	public String toMessage(){
		return "MortarItem " + super.toMessage() + " " + hash;
	}
	
	public int ammoType(){
		return 2;
	}
}
