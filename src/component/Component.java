package component;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import message.*;
import message.Message.TYPE;

public interface Component extends Remote{

	public Integer[] getSet() throws RemoteException;
	
	public int getId() throws RemoteException;
	
	public void multicast(TYPE type) throws RemoteException; 
	
	public void receive(Message msg) throws RemoteException, NotBoundException;
	
	public void newNodeJoined() throws Exception;
	
}
