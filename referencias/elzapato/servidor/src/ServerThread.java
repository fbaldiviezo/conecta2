import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class ServerThread extends Thread {
    private static ArrayList<ServerThread> threadList = new ArrayList<>();
    private Socket socket;
    private BufferedReader entrada;
    private BufferedWriter salida;
    private String usuario;

    public ServerThread(Socket socket) {
        try {
            this.socket = socket;
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            this.usuario = entrada.readLine();            
            threadList.add(this);
            salida.write("¡Hola, " + usuario + "! Bienvenido al chat.");
            salida.newLine();
            salida.flush();
            
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String str = entrada.readLine();
                
                // eliminar de la lista de mensajes al baboso xd
                if("exit".equals(str)) {
                	envioMensaje("se ha desconectado", this);
                	threadList.remove(this);
                	break;
                }
                
                if("aaa".equals(str) || "bbb".equals(str)) {
                	salida.write("has sido botado por no hablar bien");
                    salida.newLine();
                    salida.flush();
                	threadList.remove(this);
                	break;
                }
                
                envioMensaje(str, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void envioMensaje(String messageToSend, ServerThread emisor) throws IOException {
        for (ServerThread serverThread : threadList) {
        	// deberia no enviar al emisor supuestamente
        	if(serverThread != emisor) {
        		serverThread.salida.write("(" + emisor.usuario + "): " + messageToSend);
                serverThread.salida.newLine();
                serverThread.salida.flush();
        	}
            
        }

    }
    
}
