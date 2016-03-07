package component;

import java.rmi.Remote;
import java.rmi.RemoteException;

import message.*;
import message.Message.TYPE;

public interface Component extends Remote{

	public Integer[] getSet() throws RemoteException;

	public void setRequest_set(Component request_set) throws RemoteException;
	
	public int getId() throws RemoteException;
	
	public void multicast(TYPE type) throws RemoteException; 
	
	public void receive(Message msg) throws RemoteException;
	
	public void setRegistry_set(Component[] registry_set) throws RemoteException;
	
}
