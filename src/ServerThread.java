import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class ServerThread implements Runnable{
    private PrintWriter log;
    private Scanner sc;
    private Socket socket;
    private BufferedWriter bw;
    private BufferedReader br;
    private String serverHost;
    private Map<String,String> accounts;
    private Boolean port_mode;
    private Boolean pasv_mode;
    
    //General Constructor
    public ServerThread(Socket socket,PrintWriter log, Map<String,String> accounts, Boolean port_mode, Boolean pasv_mode){
        this.socket = socket;
        this.log = log;
        this.accounts = accounts;
        this.port_mode = port_mode;
        this.pasv_mode = pasv_mode;
    }

    //Gets the date    
    public String getDate(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    //main thread
    public void run(){
        try{
            //Creates writers and readers
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter out = new BufferedWriter(osw);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to: " + socket.getRemoteSocketAddress().toString());
            log.println(getDate() + " Connected to: " + socket.getRemoteSocketAddress().toString());

            //Generates a helper class to implement FTP commands
            ServerThreadHelper sth = new ServerThreadHelper(log, socket.getRemoteSocketAddress().toString(),out,accounts);
            sth.sendMessage("220 Welcome to a FTP server by ajt89\n");
            boolean connected = true;
            
            //constantly reading in commands and running the corresponding command.
            while (connected){
                //Reading in input and splitting by space and attempting to parse command
                String input = in.readLine();
                String[] inputArgs = input.split(" ");
                log.println(getDate() + " " + socket.getRemoteSocketAddress().toString() + " Recieved: " + input);
                System.out.println(getDate() + " " + socket.getRemoteSocketAddress().toString() + " Recieved: " + input);
                switch (inputArgs[0]){
                case "USER": sth.user(input);
                    break;
                    
                case "PASS": sth.pass(input);
                    break;
                    
                case "CWD": sth.cwd(input);
                    break;
                    
                case "CDUP": sth.cdup(input);
                    break;
                    
                case "QUIT": sth.quit(input);
                    connected = false;
                    break;
                    
                case "PASV": 
                    if (pasv_mode){
                        sth.pasv(input);
                    }
                    else{
                        sth.sendMessage("202 PASV disabled on this server.\n");
                    }
                    break;
                    
                case "EPSV": 
                    if (pasv_mode){
                        sth.epsv(input);
                    }
                    else{
                        sth.sendMessage("202 EPSV disabled on this server.\n");
                    }
                    break;
                    
                case "PORT":
                    if (port_mode){
                        sth.port(input);
                    }
                    else{
                        sth.sendMessage("202 PORT disabled on this server.\n");
                    }
                    break;
                    
                case "EPRT": 
                    if (port_mode){
                        sth.eprt(input);
                    }
                    else{
                        sth.sendMessage("202 EPRT disabled on this server.\n");
                    }
                    break;
                    
                case "RETR": sth.retr(input);
                    break;
                    
                case "PWD": sth.pwd(input);
                    break;
                    
                case "LIST": sth.list(input);
                    break;
                
                case "HELP": sth.help(input);
                    break;
                
                default: out.write("500 Syntax error, command unrecognized.\n");
                    out.flush();
                    log.println(getDate() + socket.getRemoteSocketAddress().toString() + " Sent: 500 Syntax error, command unrecognized.\n");
                    System.out.println(getDate() + socket.getRemoteSocketAddress().toString() + " Sent: 500 Syntax error, command unrecognized.\n");
                    break;
                }
            }

            //While loop is broken out of, doing some clean up
            out.close();
            socket.close();
            System.out.println("Disconnected from: " + socket.getRemoteSocketAddress().toString());
            log.println(getDate() + " Disconnected from: " + socket.getRemoteSocketAddress().toString());
            log.close();
        } catch(Exception e){
            e.printStackTrace();
            log.println(getDate() + " " + e);
            log.close();
        }
    }
}
