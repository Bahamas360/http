import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
    public static ServerSocket serverSocket;
    public static final int port = 6789;
    public static Thread clientThread;
    public static void main(String[] args) {
        try{
            serverSocket = new ServerSocket(port);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");
                if (clientThread != null) {
                    clientThread = new Thread(new HttpRequest(clientSocket));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}