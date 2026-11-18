import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;

public class Servidor {
    private final ArchivoDatos datos;
    private final ArrayList<ServerThread> threadList = new ArrayList<>();

    public Servidor(ArchivoDatos datos) {
        this.datos = datos;
    }

    public static void main(String[] args) {
        try {
            int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 3333;
            if (puerto < 1 || puerto > 65535)
                throw new IllegalArgumentException("Puerto fuera de rango.");
            Path ruta = Path.of(args.length > 1 ? args[1] : "datos");
            ArchivoDatos datos = new ArchivoDatos(ruta);
            try (ServerSocket serverSocket = new ServerSocket(puerto)) {
                Servidor servidor = new Servidor(datos);
                System.out.println("Conecta2: servidor iniciado en el puerto " + puerto);
                System.out.println("Datos: " + ruta.toAbsolutePath());
                while (true) {
                    Socket socket = serverSocket.accept();
                    new ServerThread(socket, servidor).start();
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.exit(1);
        }
    }

    // Estas operaciones mantienen juntos el guardado y el orden de las
    // notificaciones.
    public synchronized void ingresar(ServerThread cliente, String alias) throws IOException {
        String nombre = datos.registrar(alias);
        if (buscar(nombre) != null)
            throw new IllegalArgumentException("Ese alias ya está conectado.");
        cliente.setUsuario(nombre);
        threadList.add(cliente);
        cliente.enviar("BIENVENIDO\t" + nombre);
        for (Mensaje mensaje : datos.historial(nombre))
            cliente.enviar(mensaje.aProtocolo());
        publicarUsuarios();
        cliente.enviar("LISTO");
        System.out.println("Conectado: " + nombre);
    }

    public synchronized void enviarMensaje(ServerThread emisor, String destino, String texto) throws IOException {
        ArchivoDatos.validarTexto(texto);
        ArrayList<String> destinatarios = new ArrayList<>();
        if (destino.isEmpty()) {
            for (ServerThread cliente : threadList) {
                if (cliente != emisor) destinatarios.add(cliente.getUsuario());
            }
        } else {
            destinatarios.add(ArchivoDatos.normalizarUsuario(destino));
        }
        if (destinatarios.isEmpty()) throw new IllegalArgumentException("No hay otros clientes conectados.");
        for (String destinatario : destinatarios) {
            Mensaje mensaje = datos.guardar(emisor.getUsuario(), destinatario, texto);
            // Cada copia se confirma después de guardarla, también en un envío a todos.
            notificar(emisor, mensaje.aProtocolo());
            ServerThread receptor = buscar(destinatario);
            if (receptor != null) notificar(receptor, mensaje.aProtocolo());
        }
        emisor.enviar("ENVIADO");
    }

    public synchronized void seleccionarChat(ServerThread cliente, String contacto) {
        if (!contacto.isEmpty()) {
            contacto = ArchivoDatos.normalizarUsuario(contacto);
            if (!datos.existe(contacto) || contacto.equals(cliente.getUsuario())) {
                throw new IllegalArgumentException("Selecciona otro usuario registrado.");
            }
        }
        cliente.setContacto(contacto);
    }

    public synchronized void leer(ServerThread lector, long id) throws IOException {
        Mensaje mensaje = datos.marcarLeido(lector.getUsuario(), id);
        // La misma respuesta actualiza al lector y al emisor.
        notificar(lector, "LEIDO\t" + id);
        ServerThread emisor = buscar(mensaje.getEmisor());
        if (emisor != null) notificar(emisor, "LEIDO\t" + id);
    }

    public synchronized void desconectar(ServerThread cliente) {
        if (threadList.remove(cliente)) {
            System.out.println("Desconectado: " + cliente.getUsuario());
            ServerThread receptor = buscar(cliente.getContacto());
            if (receptor != null) {
                try {
                    Mensaje aviso = datos.guardar(cliente.getUsuario(), receptor.getUsuario(),
                            cliente.getUsuario() + " salió del chat.");
                    notificar(receptor, aviso.aProtocolo());
                } catch (IOException e) {
                    System.err.println("No se pudo guardar el aviso de salida: " + e.getMessage());
                }
            }
            publicarUsuarios();
        }
    }

    private ServerThread buscar(String nombre) {
        for (ServerThread cliente : threadList) {
            if (nombre.equals(cliente.getUsuario()))
                return cliente;
        }
        return null;
    }

    private void publicarUsuarios() {
        StringBuilder linea = new StringBuilder("CLIENTES");
        for (String nombre : datos.getUsuarios()) {
            linea.append('\t').append(nombre);
            linea.append('\t').append(buscar(nombre) != null);
        }
        for (ServerThread cliente : threadList)
            notificar(cliente, linea.toString());
    }

    private void notificar(ServerThread cliente, String linea) {
        try {
            cliente.enviar(linea);
        } catch (IOException e) {
            cliente.cerrar();
        }
    }
}
