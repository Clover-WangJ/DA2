package message;

import java.io.Serializable;

public class Message implements Serializable, Comparable<Message> {

	private static final long serialVersionUID = 5695487132387898197L;
	private int idSender;
	private int clock;
	private TYPE type;
	
	public enum TYPE {
		REQUEST,
		GRANT,
		RELEASE,
		RELINQUISH,
		INQUIRE,
		POSTPONED
	}
	
	public Message(int idSender, int clock, TYPE type) {
		super();
		this.idSender = idSender;
		this.clock = clock;
		this.type = type;
	}

	public int compareTo(Message msg) {
		int d = this.clock - msg.getClock();

		if (d == 0) {
			// Equal time, use process id as tie-breaker
			d = this.idSender - msg.getIdSender();
		}

		return d;
	}
	
	public int getIdSender() {
		return idSender;
	}


	public int getClock() {
		return clock;
	}


	public TYPE getType() {
		return type;
	}



	
}
