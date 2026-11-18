import java.net.*;
import java.io.*;

public class ClientThread extends Thread {
    private Socket socket;
    private BufferedReader entrada;
    public Boolean activo = true;

    public ClientThread(Socket s) throws IOException {
        this.socket = s;
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() { 
        try {
            while(activo) {
                String respueta = entrada.readLine();
                System.out.println(respueta);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
