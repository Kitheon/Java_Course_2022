package networkchat;

/*
Program 7, CET 350
* 
* Group 5
* @author Robert Krency, kre1188@calu.edu
* @author Kevin Reisch, rei3819@calu.edu
*/


import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.io.*;

public class Chat implements Runnable, ActionListener, WindowListener
{
	private BufferedReader br;						//io
	private PrintWriter pw;
	
	protected final static boolean auto_flush = true;		//for readability
		
	//buttons
	Button ChangePortButton = new Button("Change Port");	//changes the port
	Button SendButton = new Button("Send");					//sends message
	Button ServerButton = new Button("Start Server");		//start your own host server
	Button ClientButton = new Button("Connect");			//connect to a server
	Button DisconnectButton = new Button("Disconnect");		//disconnect from server
	Button ChangeHostButton = new Button("Change Host");	//change who is hosting the server
	
	//labels, display text
	Label PortLabel = new Label("Port: ");
	Label HostLabel = new Label("Host: ");
	
	//text fields
	TextField ChatText = new TextField(70);					//write your message
	TextField PortText = new TextField(10);					//identify port
	TextField HostText = new TextField(10);					//identify host
	
	//text areas
	TextArea DialogScreen = new TextArea("", 10, 80);		//chat, 10 rows, 80 columns
	TextArea MessageScreen = new TextArea("", 3, 80);		//system messages, 3 rows, 80 columns
	
	//Frame
	Frame DispFrame;
	
	//Grid bag layout
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints constraints = new GridBagConstraints();
	
	//Thread
	Thread TheThread;
	
	//Sockets
	Socket client;
	Socket server;
	
	//Server Socket
	ServerSocket listen_socket;
	
	//variables
	String host = "";						//host name
	int port = 44004;						//port we are using
	int service = 0;						//machine state, 0 = waiting to be identified, 1 = is server, 2 = is client,
	private int timeout = 1000;				//wait time for a connection
	boolean more = true;					//control for program loop
	

	public static void main(String[] args)
	{
		int timeout = 1000;
		//accept parameter from keyboard
		//this is the time out value
		//if not an integer, use a default value
		Chat chat = new Chat(timeout);
	}
	
	public Chat(int WT)
	{
		initComponents();
	}
	
	//sets up screen
	public void initComponents()
	{
		//frame setup
		DispFrame = new Frame("Chat");
		DispFrame.setLayout(gbl);
		DispFrame.setVisible(true);
		
		//gridbag
		gbl.columnWeights = new double[] {1, 1, 7, 1, 2, 2};
		gbl.columnWidths = new int[] {1, 1, 7, 1, 2, 2};
		gbl.rowWeights = new double[] {7, 1, 1, 1, 1, 3};
		gbl.rowHeights = new int[] {7, 1, 1, 1, 1, 3};
		
		constraints.anchor = GridBagConstraints.NORTHEAST;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.fill = GridBagConstraints.BOTH;
		
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(DialogScreen, constraints);
		DispFrame.add(DialogScreen);
		
		constraints.gridy = 1;
		constraints.gridwidth = 5;
		gbl.setConstraints(ChatText, constraints);
		DispFrame.add(ChatText);
		
		constraints.gridx = 5;
		constraints.gridwidth = 1;
		gbl.setConstraints(SendButton, constraints);
		DispFrame.add(SendButton);
		
		constraints.gridx = 1;
		constraints.gridy = 2;
		gbl.setConstraints(HostLabel, constraints);
		DispFrame.add(HostLabel);
		
		constraints.gridx = 2;
		gbl.setConstraints(HostText, constraints);
		DispFrame.add(HostText);
		
		constraints.gridx = 4;
		gbl.setConstraints(ChangeHostButton, constraints);
		DispFrame.add(ChangeHostButton);
		
		constraints.gridx = 5;
		gbl.setConstraints(ServerButton, constraints);
		DispFrame.add(ServerButton);
		
		constraints.gridx = 1;
		constraints.gridy = 3;
		gbl.setConstraints(PortLabel, constraints);
		DispFrame.add(PortLabel);
		
		constraints.gridx = 2;
		gbl.setConstraints(PortText, constraints);
		DispFrame.add(PortText);
		PortText.setText("44004");
		
		constraints.gridx = 4;
		gbl.setConstraints(ChangePortButton, constraints);
		DispFrame.add(ChangePortButton);
		
		constraints.gridx = 5;
		gbl.setConstraints(ClientButton, constraints);
		DispFrame.add(ClientButton);
		
		constraints.gridy = 4;
		gbl.setConstraints(DisconnectButton, constraints);
		DispFrame.add(DisconnectButton);
		
		constraints.gridy = 5;
		constraints.gridx = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(MessageScreen, constraints);
		DispFrame.add(MessageScreen);
		
		//add listeners
		ChangePortButton.addActionListener(this);
		SendButton.addActionListener(this);
		ChangeHostButton.addActionListener(this);
		ServerButton.addActionListener(this);
		ClientButton.addActionListener(this);
		DisconnectButton.addActionListener(this);
		
		ChatText.addActionListener(this);
		HostText.addActionListener(this);
		PortText.addActionListener(this);
		
		DispFrame.addWindowListener(this);
		DispFrame.setSize(new Dimension(600, 400));
		
		//disable
		ChatText.setEnabled(false);
		SendButton.setEnabled(false);
		DialogScreen.setEditable(false);
		DialogScreen.setBackground(Color.WHITE);
		MessageScreen.setEditable(false);
		MessageScreen.setBackground(Color.WHITE);
		ClientButton.setEnabled(false);
	}
	
	//creates and starts a thread
	public void start()
	{
		if(TheThread == null)
		{
			TheThread = new Thread(this);
			TheThread.start();
		}
	}
	
	//Runnable
	public void run()
	{
		TheThread.setPriority(Thread.MAX_PRIORITY);
		msg("Chat created\n");
		
		while(more)
		{
			String line = "";
			try									//try to read line from buffered reader
			{
				line = br.readLine();

				if(line == null)				//if the socket is closed, end loop
				{
					more = false;
				}
				else if(line.equals("DISCONNECT"))
				{
					close();
				}
				else								//if not closed, add to chat
				{
					DialogScreen.append("in: " + line + "\n");
				}
			}
			catch(IOException e)
			{
				//e.printStackTrace();
			}
		}
		
		msg("No longer reading from connection\n");
		close();									//null sockets, reader, writer
	}
	
	//clean up and exit program
	public void stop()
	{
		close();												//null sockets, reader, writer
		if(TheThread != null)
		{
			TheThread.setPriority(Thread.MIN_PRIORITY); 		//set to min priority if exists
		}
		
		//remove action listeners
		ChangePortButton.removeActionListener(this);
		SendButton.removeActionListener(this);
		ChangeHostButton.removeActionListener(this);
		ServerButton.removeActionListener(this);
		ClientButton.removeActionListener(this);
		DisconnectButton.removeActionListener(this);
				
		ChatText.removeActionListener(this);
		HostText.removeActionListener(this);
		PortText.removeActionListener(this);
		
		DispFrame.removeWindowListener(this);
		
		DispFrame.dispose(); 									//dispose the frame
		System.exit(0); 										//exit program
	}
	
	//nulls sockets, reader, and writer if they exist
	public void close()
	{
		DispFrame.setTitle("");
		try
		{
			if(server != null)				//does server socket exist?
			{
                msg("Disconnected\n");						//notify disconnect
				if(pw != null)				//does the printwriter exist
				{
					pw.print("DISCONNECT");			//send null to other device
				}
				server.close(); 			//close the socket
				server = null;				//null the socket
			}
			if(client != null)				//same thing but for client
			{
                msg("Disconnected\n");						//notify disconnect
				if(pw != null)
				{
					pw.print("DISCONNECT");
				}
				client.close();
				client = null;
			}
		}
		catch(IOException e)
		{
			//e.printStackTrace();
		}
		
		//reset buttons
		SendButton.setEnabled(false);
		ClientButton.setEnabled(true);
		ServerButton.setEnabled(true);
		ChatText.setEnabled(false);
		ChangeHostButton.setEnabled(true);
		HostText.setEnabled(true);
		ClientButton.setEnabled(false);
		
		//reset host text field
		HostText.setText("");
		host = "";
		
        DispFrame.setTitle("Chat");

		service = 0;						//reset service state
		TheThread = null;					//null thread
	}
	
	//accepts string and determines if machine state is in server or client mode
	public void msg(String str)
	{
		String word = "";
		if(service == 1)
		{
			word = "Server: ";
		}
		else if(service == 2)
		{
			word = "Client: ";
		}
		MessageScreen.append(word);
		MessageScreen.append(str);			//append string to status message text area
		ChatText.requestFocus(); 			//clicks on the text box so user doesn't have to
	}
	
	//Action listener
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		String data;
		
		if((source == ChatText) || (source == SendButton))		//send message, 2 ways to trigger
		{
			data = ChatText.getText().trim();
            if (!data.isEmpty()){                          // Only send message if it's not null or empty
                DialogScreen.append("out: " + data + "\n");			//append to chat
                pw.println(data);									//send to other
                ChatText.setText(""); 							//clear text field
            }
		}
		
		if(source == ServerButton)
		{
			try
			{
				DispFrame.setTitle("Server");
				ServerButton.setEnabled(false);					//disable buttons
				ClientButton.setEnabled(false);
				
				if(listen_socket != null)						//clear and close listen socket
				{
					listen_socket.close();
					listen_socket = null;
				}
				
				msg("Creating server socket...\n");
				
				listen_socket = new ServerSocket(port);			//set server socket port
				msg("Server socket created\n");
				listen_socket.setSoTimeout(10*timeout);			//time to wait for connection
				
				if(client != null)								//clear and close client socket
				{
					client.close();
					client = null;
				}
				
				try
				{
					msg("Looking for client...\n");
					client = listen_socket.accept();   				//listen for a socket request
					msg("Server: connection from " + client.getInetAddress() + "\n");	//status message
					
					try
					{
						br = new BufferedReader(new InputStreamReader(client.getInputStream()));		//make buffered reader
						pw = new PrintWriter(client.getOutputStream(), auto_flush);						//make print writer, autoflush
						
						service = 1;							//server mode
						ChatText.setEnabled(true);				//let user type messages
						SendButton.setEnabled(true);
						ChangeHostButton.setEnabled(false);
						HostText.setEnabled(false);
						more = true;							//run loop control
						start();								//start thread
					}
					catch(IOException er)
					{
						msg("No clients found\n");
						close();								//close sockets, reader, writer
					}
				}
				catch(SocketTimeoutException s)
				{
					msg("Connection timed out\n");
					close();
				}
			}
			catch(IOException er)
			{
				msg("Error: IOException\n");
				close();
			}
		}
		
		if(source == ClientButton)
		{
			msg("Client mode enabled\n");
			try
			{
				ServerButton.setEnabled(false);				//disable buttons
				ClientButton.setEnabled(false);
				if(server != null)							//close and clear server socket (not serversocket/listen_socket)
				{
					server.close();
					server = null;
				}
				server = new Socket();						//make new socket
				msg("Socket for server created\n");
				server.setSoTimeout(timeout);				//set timeout, shorter than server timeout
				try
				{
					msg("Searching for server...\n");
					server.connect(new InetSocketAddress(host, port));		//send connection request
					DispFrame.setTitle("Client"); 							//update frame title
					msg("A connection has been made to " + server.getInetAddress() + "\n");
					try
					{
						br = new BufferedReader(new InputStreamReader(server.getInputStream()));		//make buffered reader
						pw = new PrintWriter(server.getOutputStream(), auto_flush);						//and print writer
						
						service = 2;			//client service mode number
						
						ChatText.setEnabled(true);				//allow for typing
						SendButton.setEnabled(true);
						more = true;							//run loop control
						start();								//start thread
					}
					catch(IOException er)
					{
						msg("Server not found\n");
						close();
					}
				}
				catch(SocketTimeoutException s)
				{
					msg("Connection timed out\n");
					close();
				}
			}
			catch(IOException er)
			{
				msg("Server not found\n");
				close();
			}
		}
		
		if(source == DisconnectButton)
		{
			if(TheThread != null)
			{
				TheThread.interrupt();						//interrupt thread
			}
			close();
		}
		
		if((source == HostText) || (source == ChangeHostButton))
		{
			host = HostText.getText().trim();				//get text box text
			//close();
			if(host.length() > 0)
			{
				ClientButton.setEnabled(true);
			}
			msg("Host set to " + host + "\n");
		}
		
		if((source == PortText) || (source == ChangePortButton))
		{
			close();
			String p = PortText.getText().trim();			//get port text box text
			try
			{
				port = Integer.parseInt(p);					//try to convert into an int
				ClientButton.setEnabled(true);
			}
			catch(Exception in)	
			{
				msg("Port must be an integer\n");			//if can't, error
			}
			msg("Port set to " + p + "\n");
		}
		
		ChatText.requestFocus();
	}
	
	//Window listener
	public void windowClosing(WindowEvent e) 
	{
		//Same code as disconnect button, but with stop()
		msg("Disconnected\n");						//notify disconnect
		if(TheThread != null)
		{
			TheThread.interrupt();						//interrupt thread	
		}
		close();
		stop();
	}
	public void windowClosed(WindowEvent e)
	{
		ChatText.requestFocus();
	}
	public void windowOpened(WindowEvent e)
	{
		ChatText.requestFocus();
	}
	public void windowActivated(WindowEvent e)
	{
		ChatText.requestFocus();
	}
	public void windowDeactivated(WindowEvent e)
	{
		ChatText.requestFocus();
	}
	public void windowIconified(WindowEvent e)
	{
		ChatText.requestFocus();
	}
	public void windowDeiconified(WindowEvent e)
	{
		ChatText.requestFocus();
	}

}