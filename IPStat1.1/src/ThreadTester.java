
public class ThreadTester extends Thread{
	public static void main(String[] args) throws InterruptedException{
		int threadId = 1;
		T1 t1 = new T1(threadId++);
		t1.start();						//Starta T1
		
		Thread.sleep(5000);
		
		T2 t2 = new T2(threadId++);
		t2.start();						//Starta T2
		
		Thread.sleep(5000);
		
		t2.pause();						//Pausa T2
		
		Thread.sleep(5000);
		
		t2.unpause();					//Aktivera T2
		
		Thread.sleep(5000);
	
		t1.end();						//Stoppa T1
		
		Thread.sleep(5000);
		
		t2.end();						//Stoppa T2
	}
}

