import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HTTPServer  {
    public static ServerSocket serverSocket;
    public static final int port = 6789;
    public static Thread clientThread;
    public static void main(String[] args) {
        try{
            serverSocket = new ServerSocket(port);
            System.out.println("HTTP Server running on port " + port);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected " + clientSocket.getPort());
                clientThread = new Thread(new HttpRequestHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();


        }
    }
}
class HttpRequestHandler implements Runnable {
    private static final String sourceFile = "src";//directory where the index html is and the other file types are
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
            System.out.println("Received Request: " + requestLine);
            if(requestLine == null||!requestLine.startsWith("GET")) {
                sendErrorResponse(clientOutput,405, "Method not Allowed");
                return;
            }
            String[] requestPart =  requestLine.split(" ");
            String filepath = requestPart[1].equals("/") ? "/index.html" : requestPart[1];
            filepath = sanitize(sourceFile + filepath);
            File file = new File(filepath);
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
    private void checkIfFileExists(File file, DataOutputStream clientOutput, int code, String message) throws IOException {
        if(!file.exists()){
            sendErrorResponse(clientOutput, code, message);
        }
        else{
            sendFile(clientOutput, file);
        }
    }
    private String sanitize(String s) {
        if(s.contains("../")){
            s = s.replace("../", "/");
        }
        return s;
    }

    private void sendErrorResponse(DataOutputStream clientOutput, int code, String message) throws IOException {
        String ErrorMessage = code + " " + message;
        String Response = "HTTP/1.1 " + ErrorMessage + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                ErrorMessage;
        clientOutput.writeBytes(Response);
    }
    private void sendFile(DataOutputStream clientOutput, File file) throws IOException {
        String FileName = file.getName();
        long contentsLength = file.length();
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: "+ contentsLength + "\r\n" +
                "\r\n";
        clientOutput.writeBytes(response);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fileInputStream.read(buffer);
            String content = new String(buffer, StandardCharsets.UTF_8);
            clientOutput.writeBytes(content);
        }catch (FileNotFoundException e) {
            sendErrorResponse(clientOutput, 404, "Not Found");
        }
    }

}