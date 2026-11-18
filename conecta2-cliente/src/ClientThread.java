import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public class ClientThread extends Thread {
    private final String host;
    private final int puerto;
    private final String usuario;
    private final Consumer<String> alRecibir;
    private final Consumer<String> alDesconectar;
    private final Socket socket = new Socket();
    private final ExecutorService envios = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "envios-cliente");
        hilo.setDaemon(true);
        return hilo;
    });
    private volatile boolean activo = true;
    private volatile String motivo = "Se perdió la conexión con el servidor.";
    private BufferedWriter salida;

    public ClientThread(String host, int puerto, String usuario,
            Consumer<String> alRecibir, Consumer<String> alDesconectar) {
        this.host = host;
        this.puerto = puerto;
        this.usuario = usuario;
        this.alRecibir = alRecibir;
        this.alDesconectar = alDesconectar;
        setName("recepcion-cliente");
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            socket.connect(new InetSocketAddress(host, puerto), 5000);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(15000);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            escribir("INGRESAR\t" + usuario);
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                if (linea.startsWith("BIENVENIDO\t")) socket.setSoTimeout(0);
                String respuesta = linea;
                SwingUtilities.invokeLater(() -> alRecibir.accept(respuesta));
            }
        } catch (IOException e) {
            if (activo) motivo = "No se pudo mantener la conexión: " + e.getMessage();
        } finally {
            cerrar();
            SwingUtilities.invokeLater(() -> alDesconectar.accept(motivo));
        }
    }

    public boolean enviar(String linea) {
        if (!activo) return false;
        try {
            envios.execute(() -> {
                try {
                    escribir(linea);
                    if (linea.equals("SALIR")) cerrar();
                } catch (IOException e) {
                    motivo = "Falló el envío. Revisa el historial antes de reenviar.";
                    cerrar();
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    private synchronized void escribir(String linea) throws IOException {
        salida.write(linea);
        salida.newLine();
        salida.flush();
    }

    public void cerrar() {
        activo = false;
        envios.shutdownNow();
        try { socket.close(); } catch (IOException ignored) { }
    }

    public void salir() {
        // La cola envía primero la selección de chat y después la salida.
        if (!enviar("SALIR")) cerrar();
    }
}
