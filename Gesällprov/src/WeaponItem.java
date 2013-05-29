
public abstract class WeaponItem extends SimpGameEntity {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WeaponItem(double x, double y){
		super(x, y, 0);
	}
	public WeaponItem(double x, double y, int hash){
		super(x, y, 0, hash);
	}
	public void moveAndRotate(){
		//Do nothing, no movement
	}
	
	abstract public int ammoType();
}
