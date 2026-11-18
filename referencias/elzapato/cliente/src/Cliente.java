import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost",3333);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(),true);
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String userinput="";

            ClientThread clientThread = new ClientThread(socket);
            clientThread.start();
            
            System.out.print("Ingrese su nombre: ");
            userinput = stdin.readLine();
            salida.println(userinput);
            
            userinput = stdin.readLine();

            try {
                while(true) {
                    System.out.print("Mensaje::");
                    userinput = stdin.readLine();
                    
                    salida.println(userinput);
                    
                    // romper el while del cliente
                    if("exit".equals(userinput)) {
                    	clientThread.activo = false;
                    	System.exit(0);
                    	break;
                    }
                    
                    if("aaa".equals(userinput) || "bbb".equals(userinput)) {
                    	clientThread.activo = false;
                    	System.exit(0);
                    	break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println("Exception occured in client main::"+e.getStackTrace());
        }
    }
}
