import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class WebServer  {
    public static ServerSocket serverSocket;
    public static SSLServerSocket sslServerSocket;
    public static final int httpPort = 80;
    public static final int httpsPort = 443;
    public static Thread clientThread;
    //holds the kestore path and the password
    public static String keystorepath = null;
    public static String keystorepass = null;
    private static volatile boolean isRunning = true;
    public static void main(String[] args) {
        //if ctrl + c is pressed shuts down the server with respective messages
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down the server");
            shutdown();
        }));
        //checks for arguments if there are two give keystore path and pass their values
        if(args.length == 2){
            keystorepath = args[0];
            keystorepass = args[1];
        }
        //if its 0 or 1 the user either didnt provide the keystore path or the password
        else if(args.length > 0){
            System.err.println("Error:Didn't provide keystore path or password.");
            System.err.println("Usage: java WebStore.java <keystorepath> <keystorepassword>");
            //close
            System.exit(1);
        }
        //creates a thread to handle HTTP connections
        Thread httpThread = new Thread(()-> start_HTTP_Server());
        httpThread.start();
        //if both the keystore path and pass are given then creates a thread to watch for HTTPS connections
        if(keystorepath != null && keystorepass != null) {
            Thread httpsThread = new Thread(() -> start_HTTPS_Server());
            httpsThread.start();
        }
    }
    private static void shutdown() {
        isRunning = false; // Stop the server loop
        try {
            //check if there is server socket open
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Close the server socket
                System.out.println("Server socket closed.");
            }
            //check if there is a sslserversocket open then close it
            if (sslServerSocket != null && !sslServerSocket.isClosed()) {
                sslServerSocket.close(); // Close the server socket
                System.out.println("sslServerSocket socket closed.");
            }
            //catch any problems that happen when closing  the sockets
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }
    //method to handle HTTP connection
    public static void start_HTTP_Server(){
        try{
            //create a server socket that listens to port 80
            serverSocket = new ServerSocket(httpPort);
            System.out.println("HTTP Server running on port " + httpPort);
            while(isRunning) {
                //accepts client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected " + clientSocket.getPort());
                //create a thread for each client connection
                clientThread = new Thread(new HttpRequestHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Http Server exception: " + e.getMessage());
            e.printStackTrace();


        }
    }
    //method to handle HTTPS
    public static void start_HTTPS_Server(){
        System.setProperty("javax.net.ssl.keyStore", keystorepath);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorepass);
        try{
            SSLServerSocketFactory sslServerSocketFactory =
                    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(httpsPort);
            System.out.println("HTTPS Server running on port " + httpsPort);
            while(isRunning) {
                Socket clientSocket = sslServerSocket.accept();
                System.out.println("Client connected " + clientSocket.getPort());
                clientThread = new Thread(new HttpRequestHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Https Server exception: " + e.getMessage());
            e.printStackTrace();


        }
    }
}
class HttpRequestHandler implements Runnable {
    private static final String sourceFile = ".";//directory where the index html is and the other file types are
    //this sourceFile should be changed to where the html, video, jpg fill are located
    private Socket clientSocket;
    public HttpRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try{
            BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream clientOutput = new DataOutputStream(clientSocket.getOutputStream());

            String requestLine = clientInput.readLine();
            if(requestLine == null||!requestLine.startsWith("GET")) {
                sendErrorResponse(clientOutput,405, "Method not Allowed");
                return;
            }
            //splits the requestline on space
            String[] requestPart =  requestLine.split(" ");
            System.out.println(requestPart[0]+ requestPart[1]+ requestPart[2]);
            //sites file path to if it contains /
            String filepath = requestPart[1].equals("/") ? "/index.html" : requestPart[1];
            //sanitize
            filepath = sanitize(sourceFile + filepath);
            //create file object on file path
            File file = new File(filepath);
            //check if file exists
            checkIfFileExists(file, clientOutput ,404, "Not Found");

        } catch (Exception e) {
            System.err.println("Request handling exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try{
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Closing Socket exception: " + e.getMessage());
            }
        }

    }
    //checks if the file exists if does it sends response if there is no file sends 404 not found message
    private void checkIfFileExists(File file, DataOutputStream clientOutput, int code, String message) throws IOException {
        if(!file.exists()){
            sendErrorResponse(clientOutput, code, message);
        }
        else{
            sendFile(clientOutput, file);
        }
    }
    //sanitizes the path for multipule ../ or // and returns the sanitized path
    private String sanitize(String s) {
        while(s.contains("../")){
            s = s.replace("../", "/");
        }
        while(s.contains("//")){
            s = s.replace("//", "/");
        }
        return s;
    }
    //method to create a error page for respected error and message
    private void sendErrorResponse(DataOutputStream clientOutput, int code, String message) throws IOException {
        //build the error message
        String ErrorMessage = code + " " + message;
        //create respose with header and page
        String Response = "HTTP/1.1 " + ErrorMessage + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                "<html>\r\n" +
                "<head>\r\n" +
                "    <meta name=\"color-scheme\" content=\"light dark\">\r\n" +
                "</head>\r\n" +
                "<style>\r\n" +
                "body {\r\n" +
                "display: flex;\r\n" +
                "justify-content: center;\r\n" +
                "align-items: center;\r\n" +
                "}\r\n" +
                "h1 {\r\n" +
                "text-align: center;\r\n" +
                "font-size: 100px;\r\n" +
                "}\r\n" +
                "</style>\r\n" +
                "<body>\r\n" +
                "<h1>" +
                ErrorMessage+
                "</h1>\r\n" +
                "</body>\r\n" +
                "</html>";
        //send the respose to the client
        clientOutput.writeBytes(Response);
    }
    private void sendFile(DataOutputStream clientOutput, File file) throws IOException {
        //get the contenttype
        String fileType = contentType(file.getName());
        //get the length of the file
        long contentsLength = file.length();
        //create the response with header
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type:"+ fileType + "\r\n" +
                "Content-Length: "+ contentsLength + "\r\n" +
                "\r\n";
        //send out the respose
        clientOutput.writeBytes(response);
        try {
            //takes inputstream of a file
            FileInputStream fileInputStream = new FileInputStream(file);
            //create a byte buffer of the length ofthe file
            //hold chunks of the file
            byte[] buffer = new byte[(int) file.length()];

            int curByte;
            //reads in to buffer when end of file return -1
            while ((curByte = fileInputStream.read(buffer)) != -1) {
                //send the valid portion of file to the client
                clientOutput.write(buffer, 0, curByte);
            }
        }catch (FileNotFoundException e) {
            sendErrorResponse(clientOutput, 404, "Not Found");
        }
    }
    //method to get the contenttype of the file
    private String contentType(String fileName){
        String type = "";
        //hashmap to store the possible file types
        HashMap<String, String> contentTypes = new HashMap<>();
        contentTypes.put("html", "text/html");
        contentTypes.put("css", "text/css");
        contentTypes.put("js", "application/javascript");
        contentTypes.put("txt", "text/plain");
        contentTypes.put("json", "application/json");
        contentTypes.put("jpeg", "image/jpeg");
        contentTypes.put("png", "image/png");
        contentTypes.put("gif", "image/gif");
        contentTypes.put("bmp", "image/bmp");
        contentTypes.put("ico", "image/x-icon");
        contentTypes.put("mp4", "video/mp4");
        //for loop that goes through hashmap and finds what the correct contenttype
        for(String currentfiletype : contentTypes.keySet()){
            if(fileName.endsWith("."+currentfiletype)){
                type = contentTypes.get(currentfiletype);
            }

        }
        //returns the correct types
        return type;
    }

}