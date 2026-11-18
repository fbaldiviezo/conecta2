import java.io.*;
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        try {
            ServerSocket serversocket = new ServerSocket(3333);
            System.out.println("Inciando el Servidor...");
            while(true) {
                Socket socket = serversocket.accept();
                ServerThread serverThread = new ServerThread(socket);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }
}
