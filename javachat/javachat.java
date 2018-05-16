import java.util.Scanner;
import java.net.*;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Thread;
import java.util.ArrayList;

// java Server <port>
class Server{	
	public static void main(String[] args){
		if(args.length<=0){
			System.out.println("Error! No arguments. 'Server <port>'");
			return;
		}
		
		String a1 = args[0];
		if(a1.isEmpty()){
			System.out.println("Error! Port is missing. 'Server <port>'");
			return;
		}

		int host = Integer.parseInt(a1);
		ServerThread server = new ServerThread(host);
		new Thread(server).start();
	}
}

// java Client <ip> <port>
class Client{	
	public static void main(String[] args){
		if(args.length<=0){
			System.out.println("Error! No arguments. 'Client <ip> <port>'");
			return;
		}
		
		String a1 = "";	
		if(args.length>=1)
			a1 = args[0];
		if(a1.isEmpty()){
			System.out.println("Error! IP is missing. 'Client <ip> <port>'");
			return;
		}	
		
		String a2 = "";
		if(args.length>=2)
			a2 = args[1];
		if(a2.isEmpty()){
			System.out.println("Error! Port is missing. 'Client <ip> <port>'");
			return;
		}
		
		String host = a1;
		int port = Integer.parseInt(a2);
		ClientThread client = new ClientThread(host, port);
		new Thread(client).start();
	}
}

// Palvelinsäie.
class ServerThread implements Runnable{
	private int port;
	private boolean threadsCreated = false;
	
	public ServerThread(int port){
		this.port = port;
		try{
			String IP = InetAddress.getLocalHost().getHostAddress();
			System.out.println(IP + ":" + port);
		}
		catch(IOException e){
			System.out.println("ServerThread IP error " + e);
		}
	}

	public void start(){
	}
	
	public void acceptNextClient(ServerSocket server){
		try(
			Socket client = server.accept();
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		)
		{
			String IP = client.getInetAddress().toString().substring(1);
			ClientList.addClient(IP, in, out, IP + " joined.");
			if(!this.threadsCreated){
				// server uses ClientReader to read.
				Writer write = new Writer(out, true);
				new Thread(write).start();
				this.threadsCreated = true;
			}
			acceptNextClient(server);
		}
		catch(IOException e){
			System.out.println("ServerThread socket create error " + e);
		}
	}
	
	public void run(){
		ServerSocket server = null;
		try{
			server = new ServerSocket(this.port);
		}
		catch(IOException e){
			System.out.println("ServerThread run error " + e);
		}
		acceptNextClient(server);
	}
}

// Asiakassäie.
class ClientThread implements Runnable{
	private String host;
	private int port;
	private int connectTimes = 0;
	
	public ClientThread(String host, int port){
		this.host = host;
		this.port = port;
	}
	
	public void start(){
	}
	
	public void run(){
		try{
			String IP = InetAddress.getLocalHost().getHostAddress();
			System.out.println(IP);
		}
		catch(IOException e){
			System.out.println("ClientThread IP error " + e);
		}
		System.out.println("Connecting to " + this.host + ":" + this.port + " ...");
		
		try(
			Socket client = new Socket(this.host, this.port);
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		)
		{
			System.out.println("Connected to " + this.host + ":" + this.port);
			this.connectTimes = 0;
			Reader read = new Reader(in, out);
			new Thread(read).start();
			Writer write = new Writer(out, false);
			new Thread(write).start();
			while(true);
		}
		catch(IOException e){
			System.out.println("ClientThread run error " + e);
			if(this.connectTimes<12){
				this.connectTimes++;
				run(); // try again.
			}
		}
	}
}

// Asiakkaan tiedot.
class ClientInfo{
	public String IP = "";
	public BufferedReader in = null;
	public PrintWriter out = null;
	
	public ClientInfo(String IP, BufferedReader in, PrintWriter out){
		this.IP = IP;
		this.in = in;
		this.out = out;
	}
}

// Asiakaslista.
class ClientList{
	private static ArrayList<ClientInfo> clients = new ArrayList<>();
	public static int clientCount = 0;

	public static void addClient(String IP, BufferedReader in, PrintWriter out, String message){
		System.out.println(message);
		sendToAllClients(message);
		
		ClientInfo client = new ClientInfo(IP, in, out);		
		clients.add(client);
		
		addClientReader(client);
	}
	
	public static void removeClient(ClientInfo client){
		clients.remove(client);
		String leftMessage = client.IP + " left.";
		System.out.println(leftMessage);
		sendToAllClients(leftMessage); // Lähetä <ip> left.
		clientCount--;
	}
	
	public static void addClientReader(ClientInfo client){
		ClientReader read = new ClientReader(client);
		new Thread(read).start();
		clientCount++;
	}
	
	public static void sendToAllClients(String s){
		for(int i=0; i<clients.size(); i++)
			clients.get(i).out.println(s);
	}
}

// Palvelin lukee viestejä asiakkaalta tämän luokan avulla.
class ClientReader extends Thread{
	private ClientInfo client;
	
	public ClientReader(ClientInfo client){
		this.client = client;
	}
	
	public void run() {
		while(true){
			try{
				String fromClient = this.client.in.readLine();
				if(fromClient!=null){
					System.out.println(fromClient);
					ClientList.sendToAllClients(fromClient);
				}
			}
			catch(IOException e){
				System.out.println("ClientReader run error " + e);
				ClientList.removeClient(this.client);
				break; // Pysäytä ikuinen silmukka.
			}
		}
	}
	
	public void start() {}
}

// Palvelimen kirjoittaja.
class Writer implements Runnable{
	private static PrintWriter out;
	private Scanner write = new Scanner(System.in);
	private boolean isServer = false;
	private String IP = "";

	Writer(PrintWriter out, boolean isServer){
		this.out = out;
		this.isServer = isServer;
	}

	public void run() {				
		try{
			this.IP = InetAddress.getLocalHost().getHostAddress();
		}
		catch(IOException e){
			System.out.println("Writer run error " + e);
		}
		
		while(true){
			String nextLine = write.nextLine();
			if(nextLine!=null && nextLine.length()>0){
				if(isServer){
					String s = this.IP + " (server): " + nextLine;
					ClientList.sendToAllClients(s);
					System.out.println(s);
				}
				else
					this.out.println(this.IP + ": " + nextLine);
			}
		}
	}
	
	public void start(){}
}

// Asiakas lukee viestejä palvelimelta tämän luokan avulla.
class Reader extends Thread{
	private static BufferedReader in;
	private static PrintWriter out;

	Reader(BufferedReader in, PrintWriter out){
		this.in = in;
		this.out = out;
	}
	
	public void run() {				
		while(true){
			try{
				String fromClient = this.in.readLine();
				if(fromClient!=null){
					System.out.println(fromClient);
				}
			}
			catch(IOException e){
				System.out.println("Reader run error " + e);
				System.out.println("Server closed.");
				System.exit(0); // no server = terminate program.
			}
		}
	}
	
	public void start(){}
}
