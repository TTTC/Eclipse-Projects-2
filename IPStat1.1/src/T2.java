/**
 * Class that starts a new thread where-in it continually prints <code>Tråd T2: Tråd threadId</code> to <code>system.out</code> every second. The thread can be stopped, paused and unpaused. Implements {@link Runnable} interface.
 * @author Simon Wallgren Ahlström
 * @version 1.0
 */
public class T2 implements Runnable{
	private int threadId;
	private boolean alive, active;
	Thread thread = new Thread(this);
	
	/**
	 * Class constructor which gives the thread an id-number. The id-number is used when the thread prints its message. It is up for the the creating thread to keep track of the id-number's of the objects.
	 * @param threadId the assignment id-number
	 */
	public T2(int threadId){
		this.threadId = threadId;
	}
	/**
	 * Prints <code>Tråd T2: Tråd threadId</code> to <code>system.out</code> every second, if the thread has not been stopped or paused.
	 */
	public void run() {
		alive = active = true;
		while(alive) {
			while(active) {
				System.out.println("Tråd T2: Tråd " + threadId);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) { }
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) { }
		}
	}
	/**
	 * Calls {@link Thread#start()} to start this thread.
	 */
	public void start() {
		thread.start();
	}
	public void end() {
		alive = active = false;
	}
	public void pause() {
		active = false;
	}
	public void unpause() {
		active = true;
	}
}