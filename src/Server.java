import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server{
    
    //Gets the date
    public static String getDate(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    public static void main(String args[]){
        if(args.length != 2){
            System.err.println("Usage: java FTPServer <server.log> <port number>");
            System.exit(1);
        }
        
        String configFile = "ftpserverd.conf";
        File conf = new File(configFile);
        if (!conf.exists() || conf.isDirectory()){
            System.err.println("Configuration file not found, exiting");
            System.exit(1);
        }
        ServerConfig sc = new ServerConfig(conf);
        sc.readConfig();
        String logdirectory = sc.getLogDirectory();
        int numlogfiles = sc.getNumLogFiles();
        Map<String,String> accounts = sc.getAccounts();
        boolean port_mode = sc.getPort();
        boolean pasv_mode = sc.getPasv();
        
        //Log creation
        String logName = logdirectory + "/" + args[0];
        File f = new File(logName);
        File logFolder = new File("/home/aj/logfiles");
        File[] listOfFiles = logFolder.listFiles();
        Map<Integer,String> logMap = new HashMap<Integer,String>();

        //Reading all files in the specified log directory
        for (int i = 0; i < listOfFiles.length; i++){
        	String tempFilename = listOfFiles[i].getName();
        	if (tempFilename.equals("")){
        		continue;
        	}
        	String[] tempFilenames = tempFilename.split("\\.");
        	if (tempFilenames[0].equals(args[0])){
        		if (tempFilenames.length == 1){
        			logMap.put(0,listOfFiles[i].getAbsolutePath().toString());
        		}
        		else{
        			logMap.put(Integer.parseInt(tempFilenames[1])+1,listOfFiles[i].getAbsolutePath().toString());
        		}
        	}
        }

        //Generation and renaming of logs
        //Base case, if no logs exist
        if (!f.exists() || f.isDirectory()){
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.err.println("Error generating log file, exiting");
    			System.exit(1);
            }
        }
        //Case where other log files exist. 
        else{
        	for (int i = logMap.size(); i > 0; i--){
        		//renaming logs
        		if (i-1 < numlogfiles){
    				File temp = new File(logMap.get(i-1));
    				String newfilename = logdirectory + "/" + args[0] + ".00" + (i-1);
    				temp.renameTo(new File(newfilename));
    			}
    			//removing older logs
    			else{
    				File remove = new File(logMap.get(i-1));
    				remove.delete();
    			}
    		}
    		//create the new log
    		try {
    			f.createNewFile();
    		} catch (IOException e){
    			System.err.println("Error generating log file, exiting");
    			System.exit(1);
    		}
        }

        PrintWriter log = null;
        try {
            log = new PrintWriter(new FileWriter(f, true));
        } catch (IOException e) {
            System.err.println("Error generating log file, exiting");
    	    System.exit(1);
        }

        //Creates socket
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(Integer.parseInt(args[1]));
            System.out.println("Listening on " + args[1]);
            System.out.println("Hostname: " + InetAddress.getLocalHost().getHostName());
            log.println(getDate() + " Listening on " + args[1]);
            log.println(getDate() + " Hostname: " + InetAddress.getLocalHost().getHostName());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            log.println(getDate() + " " + e);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            log.println(getDate() + " " + e);
            System.exit(1);
        }

        //While loop to accept multiple clients at a time
        while(true){
            //accept connections and spawn a new thread for them
            Socket sock = null;
            try {
                sock = socket.accept();
                log.println(getDate() + " Accepting connection from " + sock);
                new Thread(new ServerThread(sock, log, accounts,port_mode,pasv_mode)).start();
                System.out.println("Thread running");
            } catch (IOException e) {
                e.printStackTrace();
                log.println(getDate() + " " + e);
            }
        }
    }
}
