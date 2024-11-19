import java.net.Socket;

public class HttpRequest implements Runnable {
    public Socket clientSocket;
    public HttpRequest(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
    }
}
