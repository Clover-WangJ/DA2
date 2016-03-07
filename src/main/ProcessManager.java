package main;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;


import component.Component;
import component.Config;
import message.Message.TYPE;

public class ProcessManager {

	public Component[] PROCESS_IDS;
	
	public void setRegistry(){
	  try{
		Registry registry = LocateRegistry.getRegistry("localhost", Config.PORT);
		PROCESS_IDS = new Component[registry.list().length];
		for(int i=0; i<registry.list().length; i++){
			PROCESS_IDS[i] = (Component) registry.lookup(registry.list()[i]);
		}
		for(int i=0; i<PROCESS_IDS.length; i++){
			PROCESS_IDS[i].setRegistry_set(PROCESS_IDS);	
			for(Integer j : PROCESS_IDS[i].getSet()){
				PROCESS_IDS[i].setRequest_set(PROCESS_IDS[j]);			
			}		
		}		
	  } catch (RemoteException e1) {
          e1.printStackTrace();
      } catch (NotBoundException e2) {
          e2.printStackTrace();
      } 
	  
	  MaeRun();
	}
	
	public void MaeRun(){
		for(int i = 0; i< PROCESS_IDS.length; i++){
			Component process = PROCESS_IDS[i];
			new Thread ( () -> {					
				try {
					Thread.sleep((int)(Math.random()*50));
					process.multicast(TYPE.REQUEST);
				} catch (Exception e) {
					e.printStackTrace();
	 	 		}
			}).start();
		}
	}
	
}
