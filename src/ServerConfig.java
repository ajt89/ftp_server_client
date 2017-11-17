import java.io.*;
import java.util.*;

public class ServerConfig {
    private File conf;
    private String logdirectory;
    private File usernamefile;
    private int numlogfiles;
    private Map<String, String> accounts;
    private boolean port_mode;
    private boolean pasv_mode;
    private boolean usernamefilefound;
    
    //General constructor
    public ServerConfig(File conf){
        this.conf = conf;
        accounts = new HashMap<String, String>();
        numlogfiles = 5;
        port_mode = false;
        pasv_mode = true;
        usernamefilefound = false;
    }
    
    /* Reads through the entire log file.
       It ignores any lines that being with "#"
       For logdirectory, it will default to /home/aj/logfiles
       For numlogfiles, it will default to 5
       For usernamefile, if it is not found, the server will close
       For port_mode, default is false
       For pasv_mode, default is true
    */
    public void readConfig(){
        try {
            FileInputStream fis = new FileInputStream(conf);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            String[] lines;
            while ((line = br.readLine()) != null) {
                if (line.substring(0,1).equals("#")){
                    continue;
                }
                lines = line.split("=");
                switch (lines[0]){
                case "logdirectory":
                    if (lines[1] != null && !lines[1].isEmpty()){
                        logdirectory = lines[1];
                        File d = new File(logdirectory);
                        if (!d.isDirectory()){
                            System.err.println("Log directory is not a directory, using default");
                            logdirectory = ("/home/aj/logfiles");
                        }
                    }
                    else{
                        logdirectory = ("/home/aj/logfiles");
                    }
                    break;
                case "numlogfiles":
                    if (lines[1] != null && !lines[1].isEmpty()){
                        try {
                            numlogfiles = Integer.parseInt(lines[1]);
                        } catch (NumberFormatException  e){
                            System.err.println("Error reading number of log files, setting to 5");
                            numlogfiles = 5;
                        } 
                    }
                    else{
                        numlogfiles = 5;
                    }
                    break;
                case "usernamefile":
                    usernamefile = new File(lines[1]);
                    Boolean fileStatus = readAccountsfile(usernamefile);
                    if (!fileStatus){
                    	System.exit(1);
                    }
                    usernamefilefound = true;
                    break;
                case "port_mode":
                    switch (lines[1]){
                    case "NO": port_mode = false;
                        break;
                    case "YES": port_mode = true;
                        break;
                    default: port_mode = false;
                    }
                    break;
                case "pasv_mode":
                    switch (lines[1]){
                    case "NO": pasv_mode = false;
                        break;
                    case "YES": pasv_mode = true;
                        break;
                    default: pasv_mode = true;
                    }
                    break;
                }
            }
            br.close();

            //Checks that at least one mode is enabled, if not close the server
            if (!port_mode && !pasv_mode){
                System.err.println("Port and Pasv are both disabled, exiting");
                System.exit(1);
            }
            //If username file is not found, close the server
            if (!usernamefilefound){
                System.err.println("Accounts file not found, exiting");
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Configuration file not found, exiting");
            System.exit(1);
        } catch (IOException e){
            System.err.println("Error reading configuration file, exiting");
            System.exit(1);
        }
    }
    
    //Reads the actual accounts file
    private boolean readAccountsfile(File file){
        try{
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            String[] lines;
            while ((line = br.readLine()) != null) {
                lines = line.split(" ");
                if (lines.length > 2){
                    System.out.println("Accounts file not formatted correctly, skipping line");
                    continue;
                }
                accounts.put(lines[0], lines[1]);
            }
            br.close();
            if (accounts.isEmpty()){
                System.err.println("Accounts file empty, exiting");
                return false;
            }
            return true;
        } catch (FileNotFoundException e){
            System.err.println("Accounts file not found, exiting");
            return false;
        } catch (IOException e) {
            System.err.println("Accounts file not readable, exiting");
            return false;
        }
    }
    
    public String getLogDirectory(){
        return logdirectory;
    }
    
    public int getNumLogFiles(){
        return numlogfiles;
    }
    
    public Map<String,String> getAccounts(){
        return accounts;
    }
    
    public boolean getPort(){
        return port_mode;
    }
    
    public boolean getPasv(){
        return pasv_mode;
    }
}
