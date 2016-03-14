package main;

import java.rmi.RemoteException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.*;
import java.util.Random;
import java.rmi.AlreadyBoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import component.Component;
import message.*;
import message.Message.TYPE;

public class ComponentImp extends UnicastRemoteObject implements Component{

	private static final long serialVersionUID = 3208829885919992960L;
	private int nodePort;
	private int expectedNetworkSize;
	private Registry registry;
	private Integer[] set;
	private int id;
	private Clock clk = new Clock(0);
	private Queue<Message> reqQueue = new PriorityBlockingQueue<>(); 
	private AtomicInteger grantsNum = new AtomicInteger(0);
	private AtomicBoolean granted = new AtomicBoolean(false);
	private AtomicBoolean inquiring = new AtomicBoolean(false);
	private AtomicBoolean postponed = new AtomicBoolean(false);
	private AtomicReference<Message> currentGrant = new AtomicReference<>();
	AtomicInteger nodesJoined = new AtomicInteger(0);
	
	protected ComponentImp(int registryPort, int nodePort, int expectedNetworkSize) throws RemoteException, IOException {
		super();
		this.nodePort = nodePort;
		this.id = nodePort;
		this.expectedNetworkSize = expectedNetworkSize;
		this.registry = LocateRegistry.getRegistry(registryPort);
		try {
			this.registry.bind("rmi://localhost/Mae"+Integer.toString(this.nodePort), this);
		} catch (AlreadyBoundException e) {
			System.out.println("Daboom: " + e);
		}		
		
		BufferedReader br = new BufferedReader(new FileReader("nodes.txt"));
		String line = "";	
		String[] split_line;
        int index = 0;
		while ((line = br.readLine()) != null) {
			split_line = line.split(" ");
			System.out.println("Row" + index + "has content" + Arrays.toString(split_line));
		    if( index == this.id){
  			    this.set = new Integer[split_line.length];
				for (int i = 0; i < split_line.length; i++) {
			        set[i] = Integer.valueOf(line.split(" ")[i].trim());
			        System.out.println("set" + i + "is" + set[i]);
			    }	
		    }
		    index++;
		}		
		br.close();
		System.out.println("node" + this.id + "has set" + Arrays.toString(set));
	}

	// Notify other nodes that you have joined the network.
	public void notifyOthers() throws RemoteException{		
		try
		{
			// Inform the other nodes that you have joined the network by calling their newNodeJoined remote method.
			String[] connectedNodes = this.registry.list();
			for (String nodeName : connectedNodes) {
				nodesJoined.incrementAndGet();
				Component remoteNode = getRemoteNode(nodeName);
				remoteNode.newNodeJoined();
				System.out.println("Notified node: " + nodeName);				
			}
		} catch (Exception e) {
			System.out.println("Kaboom: " + e);
		}
		
		while(nodesJoined.get() < expectedNetworkSize + 1){
			try{
				Thread.sleep(5);
			}catch(Exception e){
				
			}
		}
		if (nodesJoined.get() == expectedNetworkSize + 1){
				System.out.println("JoinedNodes: " + nodesJoined.get());
				try{
					Thread.sleep((int)(Math.random()*50));
				}catch(Exception e){			
				}
				multicast(TYPE.REQUEST);
		}
		
	}
	
	private Component getRemoteNode(String nodeStringId) throws AccessException, RemoteException, NotBoundException {
		Component remoteNode = (Component) this.registry.lookup(nodeStringId);
		return remoteNode;
	}
	
	public void newNodeJoined() throws RemoteException {
		// Increase the counter of the nodes already in the network.
		// Start the algorithm if enough nodes have joined the network.
		nodesJoined.incrementAndGet();
//		if (nodesJoined.incrementAndGet() == expectedNetworkSize + 1){
//			System.out.println("JoinedNodes: " + nodesJoined.get());
//			try{
//				Thread.sleep((int)(Math.random()*50));
//			}catch(Exception e){			
//			}
//			multicast(TYPE.REQUEST);
//		}
			
	}
	
	public Integer[] getSet() throws RemoteException{
		return set;
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
		for (Integer c : set){		
			new Thread ( () -> {					
				try {
					Thread.sleep((int)(Math.random()*15));
//					System.out.println("Process " + this.getId() + " has sent requests out to Process " + c.getId());
					getRemoteNode("rmi://localhost/Mae"+Integer.toString(c)).receive(msg);						
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();		
			

		}
	}
	
	public void receive(Message msg) throws RemoteException, NotBoundException {
//		System.out.println("node" + msg.getIdSender() + " at time" + msg.getClock() + "; Sent request to Process " + this.getId()+ "\n");	
		clk.update(msg.getClock());
		switch(msg.getType()) {
		case REQUEST:
			System.out.println("node" + this.getId() + " at time" + this.clk.getTimeStamp() + "; Recieve request from Process " + msg.getIdSender()+ "\n");
			this.receiveRequest(msg);
			break;
		case GRANT:			
			System.out.println("Process " + msg.getIdSender() + " has sent grant out to Process " + this.getId());
			if(grantsNum.incrementAndGet() == set.length) {
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
			while(!postponed.get() && grantsNum.get() < set.length) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					break;
				}
			}
			if(postponed.get()) {
				grantsNum.decrementAndGet();
				getRemoteNode("rmi://localhost/Mae"+Integer.toString(msg.getIdSender())).receive(new Message(this.getId(), this.clk.add(), TYPE.RELINQUISH));
			}			
			break;
		case RELINQUISH:
			System.out.println("Process " + msg.getIdSender() + " has sent relinquish out to Process " + this.getId());
			inquiring.set(false);
			granted.set(false);
			reqQueue.add(currentGrant.get());
			currentGrant.set(reqQueue.poll());
			granted.set(true);
			getRemoteNode("rmi://localhost/Mae"+Integer.toString(currentGrant.get().getIdSender())).receive(new Message(this.getId(), this.clk.add(), TYPE.GRANT));
			break;
		case RELEASE:
			System.out.println("Process " + msg.getIdSender() + " has sent release out to Process " + this.getId());
			granted.set(false);
			inquiring.set(false);
			if(!reqQueue.isEmpty()) {
				currentGrant.set(reqQueue.poll());
				granted.set(true);
				getRemoteNode("rmi://localhost/Mae"+Integer.toString(currentGrant.get().getIdSender())).receive(new Message(this.getId(),this.clk.add(),TYPE.GRANT));
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
	
	synchronized public void receiveRequest(Message msg) throws RemoteException, NotBoundException {
			
//		System.out.println("Process " + this.getId() + " has received request from Process " + msg.getIdSender());
		if(!granted.get()){
			granted.set(true);
			currentGrant.set(msg);	
			getRemoteNode("rmi://localhost/Mae"+Integer.toString(msg.getIdSender())).receive(new Message(this.getId(),this.clk.add(),TYPE.GRANT));
		}
		else{
			
			reqQueue.add(msg);
//			reqQueue.peek();
			if(currentGrant.get().compareTo(msg) < 0||reqQueue.peek().compareTo(msg) < 0){
				getRemoteNode("rmi://localhost/Mae"+Integer.toString(msg.getIdSender())).receive(new Message(this.getId(),this.clk.add(),TYPE.POSTPONED));
			}
			else{
				if(!inquiring.get()){
					inquiring.set(true); 
					getRemoteNode("rmi://localhost/Mae"+Integer.toString(currentGrant.get().getIdSender())).receive(new Message(this.getId(),this.clk.add(),TYPE.INQUIRE));
				}
				else{
					System.out.println("error");
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
