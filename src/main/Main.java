package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import component.Component;
import component.Config;

public class Main {
	
	static final int expectedNetworkSize = 7;
	
	
	public static void main(String[] args) throws RemoteException, IOException {
//		Registry registry = LocateRegistry.createRegistry(1099);
		
		int registryPort = Integer.parseInt(args[0]);
		int nodePort = Integer.parseInt(args[1]);
		
		ComponentImp node = new ComponentImp(registryPort, nodePort, expectedNetworkSize);
		node.notifyOthers();
		System.out.println("started");
//		BufferedReader br = new BufferedReader(new FileReader("nodes.txt"));
//		String line = "";	
//		String[] split_line;
//		while ((line = br.readLine()) != null) {
//			split_line = line.split(" ");
//			Integer set[] = new Integer[split_line.length];
//			for (int i = 0; i < split_line.length; i++) {
//			  set[i] = Integer.valueOf(line.split(" ")[i].trim());			  
//			}
//			System.out.println(set[0]);
//			registry.rebind("rmi://localhost/Mae"+nodesNum, new ComponentImp(set, nodesNum));
//			nodesNum++;
//		}
		
//		br.close();
		
				
	}
	
	
	
	
	
	
	
	
	
}
