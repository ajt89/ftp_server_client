import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerThreadHelper {
	private PrintWriter log;
	private Boolean loginStatus;
	private Boolean passiveStatus;
	private Boolean portStatus;
	private String username;
	private String password;
	private String hostname;
	private String clientHost;
	private ClientConnection cc;
	private BufferedWriter out;
	private int port;
	private Map<String,String> accounts;
	
    //General constructor, sets all states to false
	public ServerThreadHelper(PrintWriter log, String clientHostAddress, BufferedWriter out, Map<String,String> accounts){
		this.log = log;
		String[] clients = clientHostAddress.split(":");
		clientHost = clients[0].substring(1);
		this.out = out;
		loginStatus = false;
		passiveStatus = false;
		portStatus = false;
		hostname = "";
		port = 0;
		this.accounts = accounts;
	}
	
    //Gets date
	public String getDate(){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}
	
    //Verifies the user by comparing user and pass to accounts map
	public boolean verifyUser(String user, String pass){
		if (pass.equals(accounts.get(user))){
			return true;
		}
		return false;
	}
	
    //Sends message to the client
	public void sendMessage(String input){
		try{
			out.write(input);
			out.flush();
			log.println(getDate() + " " + clientHost.toString() + " Sent: " + input);
			System.out.println(getDate() + " " + clientHost.toString() + " Sent: " + input);
		} catch (Exception e){
			e.printStackTrace();
			log.println(getDate() + " " + e);
			log.close();
		}
	}
	
    /*Implements USER.
    If matches USER <username> proceed, else give error
    */
	public void user(String input){
		String pattern = "USER (.*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			username = m.group(1);
			sendMessage("331 Please specify the password.\n");
			return;
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /*Implements PASS.
    If matches PASS <password> verify account information and let user in or not.
    If not, return syntax error
    */
	public void pass(String input){
		String pattern = "PASS (.*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			password = m.group(1);
			if (verifyUser(username,password)){
				loginStatus = true;
				sendMessage("230 Login successful.\n");
				return;
			}
			sendMessage("530 Login incorrect.\n");
			return;
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /*Implements CWD
    If matches CWD <directory>, change directory to <directory> if possible.
       If unable to change directory, return failure else, return success. 
    If does not match, return syntax error.
    */
	public void cwd(String input){
		if (!loginStatus){
			sendMessage("530 Not logged in.\n"); 
			return;
		}
		String pattern = "CWD (.*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			String directory = m.group(1);
			String newDir = System.getProperty("user.dir") + "\\" +  directory;
			File f = new File(newDir);
			if (f.exists() && f.isDirectory()){
				System.setProperty("user.dir", newDir);
				sendMessage("250 Directory successfully changed.\n");
				return;
			}
			else{
				sendMessage("550 Failed to change directory.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements CDUP
    If matches CDUP, go up a directory.
    If does not match, return syntax error.
    */
	public void cdup(String input){
		if (!loginStatus){
			sendMessage("530 Not logged in.\n"); 
			return;
		}
		String pattern = "CDUP";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			String currentDir = System.getProperty("user.dir");
			File f = new File(currentDir);
			if (f.exists() && f.isDirectory()){
				String newDir = f.getParent();
				System.setProperty("user.dir", newDir);
				sendMessage("250 Directory successfully changed.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements QUIT
    If matches QUIT, send goodbye and end connection.
    If does not match, return syntax error.
    */
	public void quit(String input){
		String pattern = "QUIT";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			sendMessage("221 Goodbye.\n");
			return;
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	/* Implements PASV
    If matches PASV, generate a random port between 1024 and 49151 and get local hostname.
        Set server to be in a "passive" state and open up a socket at the port using 
        ClientConnection to wait for the client to connect.
        Send hostname and port as a1,a2,a3,a4,p1,p2 to the client where hostname is 
        a1.a2.a3.a4 and port is p1*256+p2.
    If does not match, return syntax error.
    */
	public void pasv(String input){
		if (!loginStatus){
			sendMessage("530 Not logged in.\n");
			return;
		}
		String pattern = "PASV";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try {
				hostname = InetAddress.getLocalHost().getHostAddress();
				String[] ips = hostname.split("\\.");
				String a1 = ips[0];
				String a2 = ips[1];
				String a3 = ips[2];
				String a4 = ips[3];
				port = ThreadLocalRandom.current().nextInt(1024, 49151 + 1);
				int p2 = port%256;
				int p1 = (port - p2)/256;
				passiveStatus = true;
				cc = new ClientConnection(log,hostname,port,true);
				sendMessage("227 Entering Passive Mode (" + a1 + "," + a2 + "," + a3 + "," + a4 + "," + p1 + "," + p2 + ").\n");
				return;
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
			}				
			
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements EPSV
    If matches EPSV, generate a random port between 1024 and 49151.
        Set server to be in a "passive" state and open up a socket at the port
        using ClientConnection to wait for the client to connect.
        Send port as (|||<port>|) to the server.
    If does not match, return syntax error.
    */
	public void epsv(String input){
		if (!loginStatus){
			sendMessage("530 Not logged in.\n"); 
			return;
		}
		String pattern = "EPSV";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try{
				port = ThreadLocalRandom.current().nextInt(1024, 49151 + 1);
				passiveStatus = true;
				cc = new ClientConnection(log,hostname,port,true);
				sendMessage("229 Entering Extended Passive Mode (|||" + port + "|)\n");
				return;
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements PORT
    If matches PORT(a1,a2,a3,a4,p1,p2) parse host as
        a1.a2.a3.a4 and port as p1*256+p2. Check host matches
        client or send error. Else, send success and set server to "port" mode.
    If not, send syntax error.
    */
	public void port(String input){
		if (!loginStatus){
			sendMessage("530 Not logged in.\n"); 
			return;
		}
		String pattern = "PORT (\\d+,\\d+,\\d+,\\d+),(\\d+),(\\d+)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try{
				String localHostname = m.group(1).replaceAll(",",".");
				if (!localHostname.equals(clientHost)){
					sendMessage("501 Syntax error in parameters or arguments.\n");
					return;
				}
				hostname = localHostname;
				port = Integer.parseInt(m.group(2)) * 256 + Integer.parseInt(m.group(3));
				portStatus = true;
				sendMessage("200 PORT command successful. Consider using PASV.\n");
				return;
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
				sendMessage("501 Syntax error in parameters or arguments.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements EPRT
    If matches EPRT |<1 or 2>|<hostname>|<port>| and hostname matches
        client send success and set server to "port" mode. If not, send error.
    If does not match, send syntax error.
    */
	public void eprt(String input){
		if (!loginStatus){
			sendMessage("530 Not Logged in.\n");
			return;
		}
		String pattern = "EPRT \\|(1|2)\\|(.*)\\|(\\d+)\\|";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try{
				if (!m.group(2).equals(clientHost)){
					sendMessage("501 Syntax error in parameters or arguments.\n");
					return;
				}
				hostname = m.group(2);
				port = Integer.parseInt(m.group(3));
				portStatus = true;
				sendMessage("200 EPRT command successful. Consider using EPSV.\n");
				return;
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
				sendMessage("501 Syntax error in parameters or arguments.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements RETR
    If matches RETR <file>, do
        If passive:
            Set ClientConnection to RETR mode and send filename to ClientConnection.
            Once ClientConnection receives a connection from the client and file has 
                been confirmed send message generated by ClientConnection. 
            Once transfer is complete, send message, generated by ClientConnection. 
            Close Client Connection and get server off passive.
        If port:
            Generate a new ClientConnection and set for PORT.
            Set ClientConnection to RETR mode and send filename to ClientConnection.
            Once ClientConnection is connected to the client and transfer is complete,
                send complete message.
    If not match, send syntax error.
    */
	public void retr(String input){
		if (!loginStatus){
			sendMessage("530 Not Logged in.\n");
			return;
		}
		else if (!passiveStatus && !portStatus){
			sendMessage("503 Please run PORT, PASV, EPRT, or EPSV\n");
			return;
		}
		if (portStatus){
			cc = new ClientConnection(log,hostname,port,false);
		}
		String pattern = "RETR( .*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try{
				cc.setCommand(true);
				cc.setFilename(m.group(1));
				if (passiveStatus){
					passiveStatus = false;
					while(!cc.connectionStatus()){};
					while(!cc.fileStatus()){};
					sendMessage(cc.returnMessage());
					while(!cc.getTransferStatus()){};
					sendMessage(cc.returnMessage());
					cc = null;
					return;
				}
				else{
					portStatus = false;
					while(!cc.connectionStatus()){};
					while(!cc.getTransferStatus()){};
					sendMessage( "226 Transfer complete.\n");
					cc = null;
					return;
				}
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
				sendMessage("550 File unavailable.\n");
				passiveStatus = false;
				portStatus = false;
				cc = null;
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements PWD
    If matches PWD, send client current working directory.
    If not, send syntax error.
    */
	public void pwd(String input){
		if (!loginStatus){
			sendMessage("530 Not Logged in.\n");
			return;
		}
		String pattern = "PWD";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try{
				sendMessage("257 \"" + System.getProperty("user.dir") + "\" is the current directory\n");
				return;
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
				sendMessage("550 Requested action not taken.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements LIST
    If matches LIST or LIST <file>, do
        If passive:
            Set ClientConnection to LIST mode and send file.
            Wait for connection to be established and file to be confirmed before
                sending message generated by ClientConnection.
            Wait for transfer to be complete then send message generated by
                ClientConnection.
            Get server off passive.
        If port:
            Generate a new ClientConnection and set for PORT.
            Send messages and get server off port.
    If not, send syntax error.
    */
	public void list(String input){
		if (!loginStatus){
			sendMessage("530 Not Logged in.\n");
			return;
		}
		else if (!passiveStatus && !portStatus){
			sendMessage("503 Please run PORT, PASV, EPRT, or EPSV\n");
			return;
		}
		if (portStatus){
			cc = new ClientConnection(log,hostname,port,false);
		}
		String pattern = "LIST(| .*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			try{
				cc.setCommand(false);
				cc.setFilename(m.group(1));
				if (passiveStatus){
					passiveStatus = false;
					while(!cc.connectionStatus()){};
					while(!cc.fileStatus()){};
					sendMessage(cc.returnMessage());
					while(!cc.getTransferStatus()){};
					sendMessage(cc.returnMessage());
					return;
				}
				else{
					portStatus = false;
					sendMessage("150 Here comes the directory listing.\n");
					sendMessage("226 Directory send OK.\n");
					return;
				}
			} catch (Exception e){
				log.println(getDate() + " " + e);
				e.printStackTrace();
				sendMessage("550 File unavailable.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
	
    /* Implements HELP
    If matches HELP: respond with all implemented commands
    If matches HELP <command>: respond with details about command.
    If not, send syntax error.
    */
	public void help(String input){
		String pattern = "HELP(| .*)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		if (m.find()){
			if (m.group(1) != null && !m.group(1).isEmpty()){
				String helpCommand = m.group(1).substring(1);
				switch (helpCommand){
				case "CDUP\n": sendMessage("214 CDUP moves up a directory.\n");
					return;
				case "CWD": sendMessage("214 CWD changes working directory.\n");
					return;
				case "EPRT": sendMessage("214 EPRT extension of PORT,adds support for ipv6.\n");
					return;
				case "EPSV": sendMessage("214 EPSV extension pf PASV,add support for ipv6.\n");
					return;
				case "HELP": sendMessage("214 HELP returns all commands supported.\n");
					return;
				case "LIST": sendMessage("214 LIST returns all files and directories in current working directory, must have passed PORT or PASV beforehand.\n");
					return;
				case "PASS": sendMessage("214 PASS sends password.\n");
					return;
				case "PASV": sendMessage("214 PASV asks server to open a port for data transfer.\n");
					return;
				case "PORT": sendMessage("214 PORT tells server to connect to the client for data transfer.\n");
					return;
				case "PWD": sendMessage("214 PWD returns current working directory.\n");
					return;
				case "QUIT": sendMessage("214 QUIT closes connection with server.\n");
					return;
				case "RETR": sendMessage("214 RETR retrieves file, must have passed PORT or PASV beforehand.\n");
					return;
				case "USER": sendMessage("214 USER sends username.\n");
					return;
				default: sendMessage("502 Command not implemented.\n");
				return;
				}
			}
			else{
				sendMessage("214-The following commands are recognized.\n");
				sendMessage(" CDUP CWD EPRT EPSV HELP LIST PASS PASV PORT PWD QUIT RETR USER\n");
				sendMessage("214 Help OK.\n");
				return;
			}
		}
		sendMessage("501 Syntax error in parameters or arguments.\n");
		return;
	}
}
