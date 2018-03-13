import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class ChatServer 
{
	//global variables
	public static int portNumber;
	boolean keepRunning = true;
	
	ServerSocket serverSocket;
	Map<Socket, PrintWriter> socketMap;
	ExecutorService threads;
	
	Map<String, String> portMap = new HashMap<>();
	
	public static void main (String [] args)
	{
		try
		{
			// Gets the port number from the command line
			portNumber = Integer.parseInt(args[0]);
		} 
		catch (Exception e) 
		{
			//User didn't enter the expected input
			System.out.println("Usage:");
			System.out.println("\tjava ChatServer <port number>");
            return;
		}

		ChatServer server = new ChatServer(portNumber);
		
		//have server continuously wait and accept clients
		server.acceptClient();
			
		//terminates threads
		server.endThreads();
			
	}

	//server constructor
	public ChatServer(int p)
	{
		try
		{
			//create HashMap containing clientSockets and their names
			this.socketMap = new HashMap<>();
			
			//create threads to handle clients
			this.threads = Executors.newFixedThreadPool(10);
			
			this.serverSocket = new ServerSocket(p);
		}
		catch(Exception e)
		{
			
		}
		
	}
	
	//server continuously waits and connects to clients
	public void acceptClient ()
	{
		try
		{
			System.out.println("Listening on port " + portNumber + "...");
			while(keepRunning)
			{
				Socket clientSocket = serverSocket.accept();
				
				PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
				
				//add clientSocket and PrintWriter info to HashMap
			    socketMap.put(clientSocket, output);
				//execute thread to handle client
				threads.submit(() -> handleClient(clientSocket));
			}
		}
		catch(Exception e)
		{
			
		}
	}
		 
	//reads data from clients and writes out data
	public void handleClient (Socket cSocket)
	{
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
			
			String inputLine, port, name = null;
			
			
			
			//prompt client to enter their username
			socketMap.get(cSocket).println("Please enter your name: ");
			
			while((inputLine = in.readLine()) != null)
			{
				//get name of client 
				if(name == null)
				{
					name = inputLine;
					
					//notifies server that a new client has connected
					System.out.println("Connected to " + name + "...");
					
					port = (in.readLine());
					
					portMap.put(name,port);
				}
				//if file is requested 
				else if(inputLine.startsWith("-f"))
				{
					//read in fileOwner and fileName
					String fileOwner = inputLine.replaceAll("-f","");
					String fileName = in.readLine();
					
					//if the user exists
					if(portMap.get(fileOwner) != null)
					{
						//client sends themselves the fileName port number for reading
						socketMap.get(cSocket).println(fileName);
						socketMap.get(cSocket).println(portMap.get(fileOwner));
					}
					else
						socketMap.get(cSocket).println("No such user in chat room");
				}
				else
				{
					//sends sender's message to all clients in chat
					for(Map.Entry<Socket, PrintWriter> entry : socketMap.entrySet())
					{
						if(!(entry.getKey() == cSocket))
							entry.getValue().println(name + ": " + inputLine);	
					}
				}
			
		    }
			//removes socket from maps once standard input closes
			portMap.remove(name);
			socketMap.remove(cSocket);
		   }
	
		catch(Exception e){};
	}
	
	//terminates threads 
	public void endThreads ()
	{
		threads.shutdownNow();
	}
}