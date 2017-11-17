import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientConnection implements Runnable{
    private PrintWriter log;
    private String hostname;
    private String filename;
    private String message;
    private int portNumber;
    private boolean pasv;
    private boolean retr;
    private boolean connectionStatus;
    private boolean transferStatus;
    private boolean commandStatus;
    private boolean filenameStatus;
    private boolean fileStatus;
    private boolean completed;
    Socket connection;
    ServerSocket server;
    
    //General constructor
    public ClientConnection(PrintWriter log,String hostname,int portNumber,boolean pasv){
        this.log = log;
        this.hostname = hostname;
        this.portNumber = portNumber;
        this.pasv = pasv;
        retr = false;
        filename = "";
        connectionStatus = false;
        transferStatus = false;
        commandStatus = false;
        filenameStatus = false;
        server = null;
        connection = null;
        fileStatus = false;
        completed = false;
        new Thread((Runnable) this).start();
    }
    
    //Gets date
    public String getDate(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    //Returns data transfer status
    public boolean getTransferStatus(){
        return transferStatus;
    }
    
    //Returns message generated
    public String returnMessage(){
        return message;
    }
    
    //Returns status of the connections with the Client
    public boolean connectionStatus(){
        return connectionStatus;
    }
    
    //Sets command to be retr or list
    public void setCommand(boolean retr){
        this.retr = retr;
        commandStatus = true;
    }
    
    //Returns status of command
    public boolean commandStatus(){
        return commandStatus;
    }
    
    //Sets filename
    public void setFilename(String requestFile){
        if (requestFile != null && !requestFile.isEmpty()){
            filename = requestFile.substring(1);
        }
        else{
            filename = null;
        }
        filenameStatus = true;
    }
    
    //Returns status of filename
    public boolean filenameStatus(){
        return filenameStatus;
    }
    
    //Return status of file transfer.
    public boolean fileStatus(){
        return fileStatus;
    }
    
    //Main thread
    public void run(){
        try{
            //If passive, open another ServerSocket and accept connection
            if (pasv){
                log.println(getDate() + " Opening socket on port " + portNumber);
                System.out.println("Opening socket on port " + portNumber);
                server = new ServerSocket(portNumber);
                connection = server.accept();
                log.println(getDate() + " Accepting connection from " + connection.getRemoteSocketAddress().toString());
                connectionStatus = true;
            }
            //If port, connect to the client
            else{
                log.println(getDate() + " Connecting to " + hostname + " at " + portNumber);
                System.out.println(getDate() + " Connecting to " + hostname + " at " + portNumber);
                while (connection == null){
                    connection = new Socket(hostname, portNumber);
                }
                connectionStatus = true;
            }
            //Waiting for connection to be established, command to be set, and filename to be sent
            while(!connectionStatus){}
            while(!commandStatus){}
            while(!filenameStatus){}

            //Main while loop
            while(!completed){
                //If retr is set
                if (retr){
                    //Checks if file is valid
                    File f = new File(filename);
                    if(!f.exists()){
                        message = "550 File unavailable.\n";
                        transferStatus = true;
                        completed = true;
                        break;                        
                    }
                    //Sets input stream to file and output stream to the socket.
                    InputStream in =  new FileInputStream(f);
                    OutputStream out = connection.getOutputStream();
                    message = "150 Opening Binary mode data connection for \"" + filename + "\" (" + f.length() + " bytes) \n";
                    fileStatus = true;
                    //Sending data in 8kb chunks.
                    byte[] bytes = new byte[8192];
                    int count;
                    System.out.println("Starting file transfer process");
                    //Send data until entire file is sent
                    while ((count = in.read(bytes)) > 0){
                        out.write(bytes, 0, count);
                    }
                    //Do clean up
                    out.close();
                    in.close();
                    connection.close();
                    if (pasv){
                        server.close();
                    }
                    message = "226 Requested file action successful.\n";
                    transferStatus = true;
                    completed = true;
                    break;    
                }
                //If list is set
                else{
                    //Opens up a buffered writer to send plain text to the client.
                    OutputStream os = connection.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);
                    //If no filename is sent, list the directory
                    if (filename==null){
                        File f = new File(".");
                        File[] files = f.listFiles();
                        String output = "";
                        message = "150 Here comes the directory listing.\n";
                        fileStatus = true;
                        //For all files and directories in the directory, send them all
                        for (File file : files) {
                            output = file.getCanonicalPath() + "\n";
                            bw.write(output);
                            bw.flush();
                        }
                        message = "226 Directory send OK.\n";
                        transferStatus = true;
                        completed = true;
                    }
                    //If filename is set, send information about that specific file
                    else{
                        //Validate file
                        File f = new File(filename);
                        if(!f.exists()){
                            message = "550 File unavailable.\n";
                            transferStatus = true;
                            completed = true;
                            break;                    
                        }
                        bw.write("150 Here comes the file listing.\n");
                        fileStatus = true;
                        System.out.println("Name: " + f.getName());
                        System.out.println("Absolute path: " + f.getAbsolutePath());
                        System.out.println("Size: " + f.length());
                        System.out.println("Last modified: " + f.lastModified());
                        message = "226 Directory send OK.\n";
                        transferStatus = true;
                        completed = true;    
                    }
                    //Do cleanup
                    bw.close();
                    connection.close();
                    if (pasv){
                        server.close();
                    }
                }
            }
        } catch(FileNotFoundException e){
            log.println(getDate() + " " + e);
            message = "550 File unavailable.\n";
            transferStatus = true;
            
        } catch(Exception e){
            log.println(getDate() + " " + e);
            message = "553 Requested action not taken.\n";
            transferStatus = true;
        }
    }
}
