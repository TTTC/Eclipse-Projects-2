public class MineItem extends WeaponItem {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MineItem(double x, double y){
		super(x, y);
		this.loadImage("Images/MineItem1.png");
	}
	public MineItem(double x, double y, int hash){
		super(x, y, hash);
		this.loadImage("Images/MineItem1.png");
	}
	public String toMessage(){
		return "MineItem " + super.toMessage() + " " + hash;
	}
	
	public int ammoType(){
		return 4;
	}
}
