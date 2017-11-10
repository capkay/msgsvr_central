import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;
import java.text.*;
import java.io.*;
import java.net.*;

// threadable client_worker object to handle each client connection
class ClientWorker implements Runnable 
{
	// client socket instance variable will be updated when thread is created
	private Socket client;
	// set of variables that will be shared from main server application
	// semaphores to provide mutual exclusion when accessing respective lists
	Semaphore rw_all;
	Semaphore rw_curr;
	Semaphore rw_data;
	// list of strings to hold names of users for known user list & connected user list
	List<String> all_names;
	List<String> curr_names;
	// list of data objects to hold message related information
	List<Data> data;
	// name of client : will be initialized when client sends its name
	String my_name = null;

	// constructor to connect respective variables
	ClientWorker(Socket client,Semaphore rw_data,Semaphore rw_all,Semaphore rw_curr,List<String> all_names,List<String> curr_names,List<Data> data) 
	{
		this.client  = client;
		this.rw_data = rw_data;
		this.rw_all = rw_all;
		this.rw_curr = rw_curr;
		this.all_names = all_names;
		this.curr_names = curr_names;
		this.data = data;
	}

	// command processing method : perform actions based on client requests	, passed through cmd & out handles
	public int rx_cmd(BufferedReader cmd,PrintWriter out){
		try
		{
			String cmd_in = cmd.readLine();
			// display all users 
			if(cmd_in.equals("1")){
				// print timestamp and log respective message to terminal
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" displays all known users");
				// send all names to client
				for (int i = 0; i < all_names.size(); i++) {
					out.println(all_names.get(i));
				}
				// send "end of message" to indicate end of current transaction
				out.println("EOM");
			} 
			// display all connected users
			else if(cmd_in.equals("2")){
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" displays all connected users");
				for (int i = 0; i < curr_names.size(); i++) {
					out.println(curr_names.get(i));
				}
				out.println("EOM");
			}
			// send message to particular user
			else if(cmd_in.equals("3")){ 
				// get name and message from client
				String name = cmd.readLine();
				String msg  = cmd.readLine();
				// print timestamp and log respective message to terminal
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" posts a message for "+name);
		
				// access the all user list and check if the addressee's name is already present in the known user list, otherwise make this user as known by adding to the list. also sort the list by alphabetical order if updating the list	
				try
				{
					// semaphore for mutual exclusion	
					rw_all.acquire();
					if(!all_names.contains(name)){
						all_names.add(name);
						java.util.Collections.sort(all_names,String.CASE_INSENSITIVE_ORDER);
						// print timestamp and log respective message to terminal
						System.out.println(time+", "+name+" added to known user list");
					}
					rw_all.release();
					rw_data.acquire();
					// add the message info to data object list
					data.add(new Data(my_name,name,time,msg));
					rw_data.release();
				}
	   			catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			// send message to currently connected users
			else if(cmd_in.equals("4")){ 
				String msg  = cmd.readLine();
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" posts a message for all connected users");
				try
				{
					rw_curr.acquire();
					rw_data.acquire();
					for (int i = 0; i < curr_names.size(); i++) {
						String name = curr_names.get(i);
						data.add(new Data(my_name,name,time,msg));
					}
					rw_data.release();
					rw_curr.release();
				}
	   			catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			// send message to all known users
			else if(cmd_in.equals("5")){ 
				String msg  = cmd.readLine();
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" posts a message for all known users");
				try
				{
					rw_all.acquire();
					rw_data.acquire();
					for (int i = 0; i < all_names.size(); i++) {
						String name = all_names.get(i);
						data.add(new Data(my_name,name,time,msg));
					}
					rw_data.release();
					rw_all.release();
				}
	   			catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			// to retreive message for this client
			else if(cmd_in.equals("6")){ 
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" gets messages");
				// search through data to find messages intended for this user and delete after sending them
				try
				{
					rw_data.acquire();
					for (int i = 0; i < data.size(); i++) {
						Data tmp = data.get(i);
						if(tmp.to.equals(my_name)){
							out.println(tmp.from);
							out.println(tmp.time);
							out.println(tmp.msg);
						}
					}
					rw_data.release();
				}
	   			catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				out.println("EOM");
				
				try
				{
					rw_data.acquire();
					for (int i = 0; i < data.size(); i++) {
						Data tmp = data.get(i);
						if(tmp.to.equals(my_name)){
							data.remove(tmp);
						}
					rw_data.release();
					}
				}
	   			catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			// return 0 to exit from while loop in run() method, when session is going to end
			else if(cmd_in.equals("7")){ 
				return 0;
			}
		}
		catch (IOException e) 
		{
			System.out.println("Read failed");
			System.exit(-1);
		}

		// default : return 1, to continue processing further commands 
		return 1;
	}
	
	// run method of thread to handle respective client 
	public void run()
	{
		BufferedReader in = null;
		PrintWriter out = null;
		// get the streams to communicate with client
		try 
		{
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out = new PrintWriter(client.getOutputStream(), true);
		} 
		catch (IOException e) 
		{
			System.out.println("in or out failed");
			System.exit(-1);
		}
		try 
		{
			// get the client name	
			my_name = in.readLine();
			// permission flag to allow requests from client, initialized to not allow requests
			boolean run_client = false;	
		
			// access currently connected list to check if same username is in use	
			rw_curr.acquire();
			// no duplicate name : add to current list and sort it. 
			if(!curr_names.contains(my_name)){
				curr_names.add(my_name);
				java.util.Collections.sort(curr_names,String.CASE_INSENSITIVE_ORDER);
				// give permission to client to run 
				out.println("continue");
				// set flag to process requests from client
				run_client = true;
			} else {
				// indicate client to abort
				out.println("abort");
			}
			rw_curr.release();
			
			// process requests from client only if flag is set
			if(run_client){	
				// modify known user list , print logging message to indicate whether user is known or unknown 	
				rw_all.acquire();
				String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				if(all_names.contains(my_name)){
					System.out.println(time+", Connection by known user "+my_name);
				} else {
					all_names.add(my_name);
					java.util.Collections.sort(all_names,String.CASE_INSENSITIVE_ORDER);
					System.out.println(time+", Connection by unknown user "+my_name);
				}
				rw_all.release();
				
	
				// process commands from client, till client indicates session termination through a request 
				while(this.rx_cmd(in,out) != 0){}
			
				// remove this user from the currently connected user list
				rw_curr.acquire();
				curr_names.remove(my_name);
				rw_curr.release();
				
				// show logging message with timestamp : user exits	
				time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
				System.out.println(time+", "+my_name+" exits");
				}
		}
	   	catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			System.out.println("Read failed");
			System.exit(-1);
		}
		// handle unexpected connection loss during a session
	   	catch (NullPointerException e)
		{
			String time = new SimpleDateFormat("MM/dd/yy hh:mm a").format(new java.util.Date());
			System.out.println(time+", Client connection lost: "+my_name);
			
			try
			{
				// remove from current user list when connection is lost unexpectedly	
				rw_curr.acquire();
				curr_names.remove(my_name);
				rw_curr.release();
			}
		   	catch (InterruptedException x)
			{
				x.printStackTrace();
			}

		}
		
		// close the client connection normally	
		try 
		{
			client.close();
		} 
		catch (IOException e) 
		{
			System.out.println("Close failed");
			System.exit(-1);
		}
	}
}


// server class : starts thread for each client trying to connect
class PServer 
{
	// server socket variable 
	ServerSocket server = null;
	// semaphores to handle mutual exclusion between threads sharing the name lists and data list
	Semaphore rw_data 	= new Semaphore( 1, true );
	Semaphore rw_curr 	= new Semaphore( 1, true );
	Semaphore rw_all 	= new Semaphore( 1, true );
	// list of names and data objects 
	List<String> all_names  = new ArrayList<String>();
	List<String> curr_names = new ArrayList<String>();
	List<Data> data  		= new ArrayList<Data>();

	// server listens on port and creates thread for each client connecting on this port	
	public void listenSocket(int port)
	{
		// create server socket on specified port number
		try
		{
			server = new ServerSocket(port); 
			System.out.println("Server running on port " + port +"," + " use ctrl-C to end");
		} 
		catch (IOException e) 
		{
			System.out.println("Error creating socket");
			System.exit(-1);
		}
		// create threads for each client trying to connect to this socket
		while(true)
		{
			ClientWorker w;
			try
			{
				w = new ClientWorker(server.accept(),rw_data,rw_all,rw_curr,all_names,curr_names,data);
				Thread t = new Thread(w);
				t.start();
			} 
			catch (IOException e) 
			{
				System.out.println("Accept failed");
				System.exit(-1);
			}
		}
	}
	
	// method to close server socket	
	protected void finalize()
	{
		try
		{
			server.close();
		} 
		catch (IOException e) 
		{
			System.out.println("Could not close socket");
			System.exit(-1);
		}
	}
	
	public static void main(String[] args)
	{
		// check for valid number of command line arguments
		if (args.length != 1)
		{
			System.out.println("Usage: java PServer <port-number>");
			System.exit(1);
		}
		// create server object and start running it to create threads to handle incoming connections	
		PServer server = new PServer();
		int port = Integer.valueOf(args[0]);
		server.listenSocket(port);
	}
}
