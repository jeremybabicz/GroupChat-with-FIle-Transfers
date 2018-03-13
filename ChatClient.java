import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatClient 
{
	public static int serverPortNumber;
	public static int listeningPortNumber;
	ExecutorService fileThreads = Executors.newFixedThreadPool(1);
	
	Socket clientSocket;
	PrintWriter myOutput;
	BufferedReader in;
	BufferedReader stdIn;
	
	ServerSocket ss;
	Socket fileClientSocket;
	
	DataInputStream input;
	DataOutputStream output;
	
	int filePortNumber;
	
	
	public static void main (String [] args)
	{
		try
		{
			// Gets the port number from the command line
			serverPortNumber = Integer.parseInt(args[3]);
			listeningPortNumber = Integer.parseInt(args[1]);
		} 
		catch (Exception e) 
		{
			//User didn't enter the expected input
			System.out.println("Usage:");
			System.out.println("\tjava ChatClient <port number>");
            return;
		}
		ChatClient client = new ChatClient(serverPortNumber, listeningPortNumber);
		
		//have threads execute functions
		client.execute();
	}
	//ChatClient constructor
	public ChatClient (int sp, int lp)
	{
		serverPortNumber = sp;
		listeningPortNumber = lp;
		try
		{
			clientSocket = new Socket("localhost", serverPortNumber);
			myOutput = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			stdIn = new BufferedReader(new InputStreamReader(System.in));
		}
		catch(Exception e)
		{
			
		}
	}
	
	//execute threads
	public void execute ()
	{
		//create threads
		ExecutorService threads = Executors.newFixedThreadPool(3);
		
		//threads handle reading and writing to server 
		threads.submit(this::read);
		threads.submit(this::write);
		
		threads.submit(this::writeFiles);
		
		//terminate threads
		threads.shutdown();
	}
	
	//reads in data from Server
	public void read ()
	{
		try
		{
			String inputLine;
			while((inputLine = in.readLine()) != null)
			{
				//if file is being requested 
				if(inputLine.startsWith("-f"))
				{
					String fileName = inputLine.replaceAll("-f","");
					
					String fp = in.readLine();
					filePortNumber = Integer.parseInt(fp);
					
					fileThreads.submit(() -> readFiles(fileName, filePortNumber));
				}
				//just message
				else
					System.out.println(inputLine);
			}
			
			closeMessenger();
		}
	
		catch(Exception e){};
	}

	//writes out to the Server
	public void write ()
	{
		try
		{
			String outputLine;
			String name = null;
			while((outputLine = stdIn.readLine()) != null)
			{
				//if user name is not entered
				//sends user name and listeningPortNumber
				if(name == null)
				{
					name = outputLine; 
					myOutput.println(name);
					myOutput.println(listeningPortNumber);
				}
				//if user wants to transfer file
				else if(outputLine.equals("f"))
				{
					//get file name from user
					System.out.println("Who owns the file?");
					String fileOwner = stdIn.readLine();
					System.out.println("Which file do you want? ");
					String file = stdIn.readLine();
					
					//send fileName to other socket
					myOutput.println("-f" + fileOwner);
					myOutput.println("-f" + file);
					
				}
				//if user wants to send a message
				else if(outputLine.equals("m"))
				{
					System.out.println("enter a message");
					String message = stdIn.readLine();
					myOutput.println(message);
				}
				else if(outputLine.equals("x"))
				{
					break;
				}
				else
					System.out.println("Please enter a correct char");
				
				System.out.println("Enter an option ('m', 'f', 'x'):");
				
			}
			//close Messenger Socket
			closeMessenger();
		}
	
		catch(Exception e){};
	}
	
	public void readFiles (String fileName, int fp)
	{
			try
			{
				fileClientSocket = new Socket("localhost", fp);
				input = new DataInputStream(fileClientSocket.getInputStream());
				output = new DataOutputStream(fileClientSocket.getOutputStream());
				
				output.writeUTF(fileName);
				
				FileOutputStream fileOut = new FileOutputStream(fileName);
				
				int numRead;
				
				byte [] buffer = new byte[1500];
				
				while((numRead = input.read(buffer)) != -1)
					fileOut.write(buffer, 0, numRead);
				
				fileOut.close();
				fileClientSocket.close();
			}
			catch (Exception e){};
	}
	
	public void writeFiles ()
	{
		try
		{
			ss = new ServerSocket(listeningPortNumber);
			while (true)
				{
					fileClientSocket = ss.accept();
					
					input = new DataInputStream(fileClientSocket.getInputStream());
					output = new DataOutputStream(fileClientSocket.getOutputStream());
					
					String fileName = input.readUTF();
					
					File file = new File(fileName);
					
					//if file doesn't exist 
					if(!file.exists() || !file.canRead() || file.length() == 0)
					{
						fileClientSocket.close();
						continue;
					}
					
					FileInputStream fileInput = new FileInputStream(file);
					
					int numRead;
					
					byte [] buffer = new byte[1500];
					
					while((numRead = fileInput.read(buffer)) != -1)
					{
						output.write(buffer, 0, numRead);
					}
					
					fileInput.close();
					fileClientSocket.close();
				}
		}
		catch(Exception e) {};
	}
	
	//close Client Socket
	public void closeMessenger ()
	{
		try
		{
			clientSocket.shutdownOutput();
			clientSocket.close();
			System.exit(0);
		}
		catch(Exception e)
		{

		}
	}
}