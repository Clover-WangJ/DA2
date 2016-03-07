package main;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.*;
import java.util.Random;

import component.Component;
import message.*;
import message.Message.TYPE;

public class ComponentImp extends UnicastRemoteObject implements Component{

	private static final long serialVersionUID = 3208829885919992960L;
	private Integer[] set;
	private Component[] request_set;
	private Component[] registry_set;
	private int init_step = 0;
	private int id;
	private Clock clk = new Clock(0);
	private Queue<Message> reqQueue = new PriorityBlockingQueue<>(); 
	private AtomicInteger grantsNum = new AtomicInteger(0);
	private AtomicBoolean granted = new AtomicBoolean(false);
	private AtomicBoolean inquiring = new AtomicBoolean(false);
	private AtomicBoolean postponed = new AtomicBoolean(false);
	private AtomicReference<Message> currentGrant = new AtomicReference<>();
	
	protected ComponentImp(Integer[] rs, int index) throws RemoteException {
		super();
		this.set = rs;
		request_set = new Component[rs.length];
		this.id = index;
		
//		System.out.println("node" + this.id + "has set" + Arrays.toString(set));
	}

	public Integer[] getSet() throws RemoteException{
		return set;
	}

	public void setRequest_set(Component request_set) throws RemoteException {
		this.request_set[init_step++] = request_set;
//		System.out.println("Node" + this.getId() + "has set" + this.request_set[init_step-1].getId());
	}

	public void setRegistry_set(Component[] registry_set) {
		this.registry_set = registry_set;
	}

	public int getId() throws RemoteException{
		return id;
	}

	synchronized public void multicast(TYPE type) throws RemoteException{		
//		for(Component c: request_set){
//			System.out.println(c.getId());		
//		}
		if(type == TYPE.REQUEST){
			grantsNum.set(0);
		}	
		Message msg = new Message(this.getId(), this.clk.add(), type);
		for (Component c : request_set){		
			new Thread ( () -> {					
				try {
					Thread.sleep((int)(Math.random()*15));
//					System.out.println("Process " + this.getId() + " has sent requests out to Process " + c.getId());
					c.receive(msg);						
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();		
			

		}
	}
	
	public void receive(Message msg) throws RemoteException {
//		System.out.println("node" + msg.getIdSender() + " at time" + msg.getClock() + "; Sent request to Process " + this.getId()+ "\n");	
		clk.update(msg.getClock());
		switch(msg.getType()) {
		case REQUEST:
			System.out.println("node" + this.getId() + " at time" + this.clk.getTimeStamp() + "; Recieve request from Process " + msg.getIdSender()+ "\n");
			this.receiveRequest(msg);
			break;
		case GRANT:			
			System.out.println("Process " + msg.getIdSender() + " has sent grant out to Process " + this.getId());
			if(grantsNum.incrementAndGet() == request_set.length) {
				postponed.set(false);
				enterCS();
				System.out.println("Process " + this.getId() + " is done");
//				for(Component c: this.request_set){
//					c.receive(new Message(this.getId(), this.clk.add(), TYPE.RELEASE));
//				}
				this.multicast(TYPE.RELEASE);
			}
			break;
		case INQUIRE:
			System.out.println("Process " + msg.getIdSender() + " has sent inquire out to Process " + this.getId());
			while(!postponed.get() && grantsNum.get() < this.request_set.length) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					break;
				}
			}
			if(postponed.get()) {
				grantsNum.decrementAndGet();
				registry_set[msg.getIdSender()].receive(new Message(this.getId(), this.clk.add(), TYPE.RELINQUISH));
			}			
			break;
		case RELINQUISH:
			System.out.println("Process " + msg.getIdSender() + " has sent relinquish out to Process " + this.getId());
			inquiring.set(false);
			granted.set(false);
			reqQueue.add(currentGrant.get());
			currentGrant.set(reqQueue.poll());
			granted.set(true);
			registry_set[currentGrant.get().getIdSender()].receive(new Message(this.getId(), this.clk.add(), TYPE.GRANT));
			break;
		case RELEASE:
			System.out.println("Process " + msg.getIdSender() + " has sent release out to Process " + this.getId());
			granted.set(false);
			inquiring.set(false);
			if(!reqQueue.isEmpty()) {
				currentGrant.set(reqQueue.poll());
				granted.set(true);
				registry_set[currentGrant.get().getIdSender()].receive(new Message(this.getId(),this.clk.add(),TYPE.GRANT));
			}
			break;
		case POSTPONED:
			System.out.println("Process " + msg.getIdSender() + " has sent postpone out to Process " + this.getId());
			postponed.set(true);		
			break;
		default:
//			logerr(String.format("Unhandled message %s", m.toString()));
		}
	}
	
	synchronized public void receiveRequest(Message msg) throws RemoteException {
			
//		System.out.println("Process " + this.getId() + " has received request from Process " + msg.getIdSender());
		if(!granted.get()){
			granted.set(true);
			currentGrant.set(msg);	
			registry_set[msg.getIdSender()].receive(new Message(this.getId(),this.clk.add(),TYPE.GRANT));
		}
		else{
			
			reqQueue.add(msg);
//			reqQueue.peek();
			if(currentGrant.get().compareTo(msg) < 0||reqQueue.peek().compareTo(msg) < 0){
				registry_set[msg.getIdSender()].receive(new Message(this.getId(),this.clk.add(),TYPE.POSTPONED));
			}
			else{
				if(!inquiring.get()){
					inquiring.set(true); 
					registry_set[currentGrant.get().getIdSender()].receive(new Message(this.getId(),this.clk.add(),TYPE.INQUIRE));
				}
			}
		}
	}
	
	private void enterCS()  throws RemoteException {
		System.out.println("Process " + this.getId() + " is  in the critical section ");
		try {
			Thread.sleep(new Random().nextInt(200)+50);
		} catch (InterruptedException e) {
		}
		
	}
}
