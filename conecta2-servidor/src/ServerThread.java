import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ServerThread extends Thread {
    private final Socket socket;
    private final Servidor servidor;
    private BufferedReader entrada;
    private BufferedWriter salida;
    private String usuario;

    public ServerThread(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
        setName("cliente-" + socket.getRemoteSocketAddress());
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String saludo = leerLinea();
            if (saludo == null)
                return;
            String[] campos = saludo.split("\t", -1);
            if (campos.length != 2 || !campos[0].equals("INGRESAR")) {
                throw new IllegalArgumentException("Primero debes enviar INGRESAR y tu alias.");
            }
            servidor.ingresar(this, campos[1]);
            socket.setSoTimeout(0);
            String linea;
            while ((linea = leerLinea()) != null) {
                if (linea.equals("SALIR"))
                    break;
                try {
                    // El último campo conserva las tabulaciones del texto.
                    campos = linea.split("\t", 3);
                    if (campos.length == 3 && campos[0].equals("ENVIAR")) {
                        servidor.enviarMensaje(this, campos[1], Mensaje.restaurarSaltos(campos[2]));
                    } else if (campos.length == 2 && campos[0].equals("LEER")) {
                        servidor.leer(this, Long.parseLong(campos[1]));
                    } else {
                        throw new IllegalArgumentException("Orden desconocida o cantidad de campos inválida.");
                    }
                } catch (IllegalArgumentException e) {
                    enviarError(e.getMessage());
                } catch (IOException e) {
                    System.err.println("Error procesando la operación de " + usuario + ": " + e.getMessage());
                    enviarError("No se pudo completar la operación. Revisa el historial antes de reenviar.");
                }
            }
        } catch (IllegalArgumentException e) {
            enviarError(e.getMessage());
        } catch (IOException e) {
            if (usuario == null)
                enviarError("No se completó el acceso al servidor.");
        } finally {
            cerrar();
            servidor.desconectar(this);
        }
    }

    // readLine no tiene límite; aquí se limita antes de reservar un texto excesivo.
    private String leerLinea() throws IOException {
        StringBuilder linea = new StringBuilder();
        int caracter;
        while ((caracter = entrada.read()) != -1) {
            if (caracter == '\n')
                return linea.toString();
            if (caracter != '\r')
                linea.append((char) caracter);
            if (linea.length() > 24000)
                throw new IOException("La línea supera el límite del protocolo.");
        }
        return linea.isEmpty() ? null : linea.toString();
    }

    public synchronized void enviar(String linea) throws IOException {
        salida.write(linea);
        salida.newLine();
        salida.flush();
    }

    private void enviarError(String texto) {
        try {
            if (salida != null)
                enviar("ERROR\t" + texto);
        } catch (IOException ignored) {
        }
    }

    public void cerrar() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
