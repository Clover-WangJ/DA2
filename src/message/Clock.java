package message;

import java.util.concurrent.atomic.AtomicInteger;

public class Clock {

	private AtomicInteger TimeStamp;

	public Clock(int timeStamp) {
		super();
		TimeStamp = new AtomicInteger(timeStamp);
	}

	public int getTimeStamp() {
		return TimeStamp.get();
	}

	synchronized public void update(int clk) {
			if (TimeStamp.get() < clk){
				TimeStamp.set(clk);
			}	
			TimeStamp.incrementAndGet();
			
	}

	synchronized public int add() {
		return TimeStamp.incrementAndGet();
	}
	
}
