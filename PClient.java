import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.List;
import java.io.*;
import java.net.*;

// client class : includes user interface logic to run in terminal
public class PClient
{
	// initialize components needed for one client object
	Socket socket = null; 		// socket handle passed to this variable after connection
	PrintWriter out = null; 	// to send data to server
	BufferedReader in = null; 	// to read data coming from server 
	String name = null; 		// name of client
	List<String> all_names  = new LinkedList<String>();   // storing names of all known users, received from server
	List<String> curr_names = new LinkedList<String>();   // storing names of all connected users, received from server
	List<Data> data  		= new LinkedList<Data>();     // custom data object, holds message info from server
	
	public static Pattern eom = Pattern.compile("^EOM");  // generic 'end of message' pattern 

	// carries out the initial handshakes with server by sending client name and getting permission from server to continue
	public void communicate()
	{
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter your name: ");
		name = sc.nextLine();
		
		//Send name over socket
		out.println(name);
		
		//Receive permission from server
		try
		{
			String line = in.readLine();
			if(line.equals("abort")){
				System.out.println("Aborting: Server refused connection as same username is currently being used");
				System.exit(1);
			}
		} 
		catch (IOException e)
		{
			System.out.println("Read failed");
			System.exit(1);
		}
	}

	// method to connect to server socket, given the host name and port number	
	public void listenSocket(String host, int port)
	{
		//Create socket connection
		try
		{
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} 
		catch (UnknownHostException e) 
		{
			System.out.println("Unknown host");
			System.exit(1);
		} 
		catch (IOException e) 
		{
			System.out.println("No I/O");
			System.exit(1);
		}
	}
	
	// method to get either all known users or all connected users	
	public void get_users(int option)
	{
		if(option == 1){
			out.println("1");  // to get all known users
			all_names.clear();
		} else {
			out.println("2"); // to get connected users
			curr_names.clear();
		}
		String rd_in = null;
		try 
		{
			Matcher m_eom = eom.matcher("start");  // initializing the matcher. "start" does not mean anything
			// obtain all known names/connecter user names from server till EOM is received 
			while(!m_eom.find()){
				rd_in = in.readLine();
				m_eom = eom.matcher(rd_in);
				if(!m_eom.find()){
					// add name to respective list
					if(option == 1){
						all_names.add(rd_in);
					} else {
						curr_names.add(rd_in);
					}
				} else { break; }
			}
		}
		catch (IOException e) 
		{
			System.out.println("Read failed");
			System.exit(-1);
		}
	   	catch (NullPointerException e)
		{
			System.out.println("Exiting : Connection lost with server");
			System.exit(-1);
		}
		
		// print the user names to the terminal
		if(option == 1){
			System.out.println("Known users:");
			for (int i = 0; i < all_names.size(); i++) {
				System.out.println("\t"+(i+1)+"   "+all_names.get(i));
			}
		} else {
			System.out.println("Currently connected users:");
			for (int i = 0; i < curr_names.size(); i++) {
				System.out.println("\t"+(i+1)+"   "+curr_names.get(i));
			}
		}
		
	}	

	// method to send a message, varies based on option	
	public void send_msg(int option,String name,String msg)
	{
		switch(option){
			// send to particular user
			case 0:
				out.println("3");
				out.println(name);
				out.println(msg);
			break;
			// send to currently connected users
			case 1:
				out.println("4");
				out.println(msg);
			break;
			// send to all known users
			case 2:
				out.println("5");
				out.println(msg);
			break;
		}
	}

	// method to get messages for this client	
	public void get_my_msgs()
	{
		out.println("6");
		String rd_in = null;
		String from  = null;
		String time  = null;
		String msg   = null;
		// clearing out previous messages from local copy, can be made to store permanently also
		data.clear();
		try
		{
			Matcher m_eom = eom.matcher("start");
			// get all messages from server and store a local copy using custom data object
			while(!m_eom.find()){
				rd_in = in.readLine();
				m_eom = eom.matcher(rd_in);
				if(!m_eom.find()){
					from = rd_in;
					time = in.readLine();
					msg  = in.readLine();
					data.add(new Data(from,name,time,msg));
				} else { break; }
			}
		}
		catch (IOException e) 
		{
			System.out.println("Read failed");
			System.exit(-1);
		}

		// show all messages for this client
		if(data.size() == 0){
			System.out.println("No new messages");
			return;
		} else {
			System.out.println("Your messages:");
		}
		
		for (int i = 0; i < data.size(); i++) {
			System.out.println("\t"+(i+1)+"   From "+data.get(i).from+", "+data.get(i).time+", "+data.get(i).msg);
		}
	}
	
	// sends close command	
	public void close()
	{
		out.println("7");
	}

	// this method makes sure no error occurs when entering an option to choose an action to be done by the client
	// modified from code taken from stackoverflow
	public int get_option(Scanner input)
	{
		int opt = 0;
		boolean problem = true;
		while (problem) {
			if (input.hasNextInt())
				opt = input.nextInt();
			else {
				System.out.println("\nEnter a proper number!!!");
				input.next();
				continue;
			}
			problem = false;
		}
		return opt;
	}

	public static void main(String[] args)
	{
		// exit if proper arguments are not provided
		if (args.length != 2)
		{
			System.out.println("Usage: PClient hostname <port>");
			System.exit(1);
		}

		// create new client object	
		PClient client = new PClient();
		// host name and port numbers are obtained from command line arguments
		String host = args[0];
		int port = Integer.valueOf(args[1]);
		// connect to socket
		client.listenSocket(host, port);
		// do initial communication : send name and check whether permission from server is obtained
		client.communicate();
		
		// initializing scanner to get input from terminal
		Scanner input = new Scanner(System.in);
		input.useDelimiter("\n");
		int opt = 0;

		String r_name = null;
		String r_msg  = null;
		// show UI messages and continuously carry out actions based on user input options
		do
		{
			switch(opt){
				// print all options
				case 0: 
					System.out.print("\n\nEnter your option:\n1.Display the names of all known users.\n2.Display the names of all currently connected users.\n3.Send a text message to a particular user.\n4.Send a text message to all currently connected users.\n5.Send a text message to all known users.\n6.Get my messages.\n7.Exit\nEnter your choice: ");
					// get user input using robust get_option method to avoid exceptions due to wrong key presses
					opt = client.get_option(input);
					System.out.println("\n");
				break;
				// display names of all known users
				case 1:
					client.get_users(1);
					opt = 0;
				break;
				// display names of currently connected users
				case 2:
					client.get_users(2);
					opt = 0;
				break;
				// send message to particular user
				case 3:
					System.out.println("Enter recipient's name: ");
					r_name = input.next();
					System.out.println("Enter a message: ");
					r_msg = input.next();
					client.send_msg(0,r_name,r_msg);
					System.out.println("\nMessage posted to "+r_name);
					opt = 0;
				break;
				// send message to currently connected users
				case 4:
					System.out.println("Enter a message: ");
					r_msg = input.next();
					client.send_msg(1,r_name,r_msg);
					System.out.println("\nMessage posted to all currently connected users");
					opt = 0;
				break;
				//send message to all known users
				case 5:
					System.out.println("Enter a message: ");
					r_msg = input.next();
					client.send_msg(2,r_name,r_msg);
					System.out.println("\nMessage posted to all known users");
					opt = 0;
				break;
				// get and print messages intended for this user
				case 6:
					client.get_my_msgs();
					opt = 0;
				break;
				default:
					System.out.println("Enter proper value");
					opt = 0;
				break;
			}
		}while (opt != 7); // exit when user enters corresponding option
		// send command to server to indicate session is going to terminate
		client.close();
	}
}
