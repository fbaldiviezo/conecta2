import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

// El servidor coordina el acceso a esta clase mediante sus métodos synchronized.
public class ArchivoDatos {
    private final Path archivo;
    private final ArrayList<String> usuarios = new ArrayList<>();
    private final ArrayList<Mensaje> mensajes = new ArrayList<>();
    private long siguienteId = 1;

    public ArchivoDatos(Path directorio) throws IOException {
        Files.createDirectories(directorio);
        this.archivo = directorio.toAbsolutePath().resolve("datos.txt");
        cargar();
    }

    private void cargar() throws IOException {
        if (!Files.exists(archivo)) Files.createFile(archivo);
        int numero = 0;
        long ultimo = 0;
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            numero++;
            try {
                String[] registro = linea.split("\t", 2);
                if (registro.length != 2) throw new IllegalArgumentException("Registro incompleto");
                if (registro[0].equals("USUARIO")) {
                    String nombre = normalizarUsuario(registro[1]);
                    if (!nombre.equals(registro[1]) || existe(nombre)) {
                        throw new IllegalArgumentException("Alias repetido o sin normalizar");
                    }
                    usuarios.add(nombre);
                } else if (registro[0].equals("MENSAJE")) {
                    String[] p = registro[1].split("\t", 6);
                    if (p.length != 6) throw new IllegalArgumentException("Se esperaban 6 campos de mensaje");
                    if (!p[4].equals("true") && !p[4].equals("false")) {
                        throw new IllegalArgumentException("El estado de lectura debe ser true o false");
                    }
                    Mensaje mensaje = new Mensaje(Long.parseLong(p[0]), Long.parseLong(p[1]),
                            p[2], p[3], Mensaje.restaurarSaltos(p[5]));
                    mensaje.setLeido(Boolean.parseBoolean(p[4]));
                    if (mensaje.getId() <= ultimo || mensaje.getId() == Long.MAX_VALUE
                            || mensaje.getFecha() <= 0 || !existe(mensaje.getEmisor())
                            || !existe(mensaje.getDestinatario())
                            || mensaje.getEmisor().equals(mensaje.getDestinatario())) {
                        throw new IllegalArgumentException("Identificador, fecha o usuarios inválidos");
                    }
                    validarTexto(mensaje.getTexto());
                    ultimo = mensaje.getId();
                    mensajes.add(mensaje);
                } else {
                    throw new IllegalArgumentException("Tipo de registro desconocido");
                }
            } catch (RuntimeException e) {
                throw new IOException("datos.txt: línea " + numero + " inválida: " + e.getMessage(), e);
            }
        }
        siguienteId = ultimo + 1;
    }

    private void agregar(String texto) throws IOException {
        Files.writeString(archivo, texto + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void guardarTodo() throws IOException {
        ArrayList<String> lineas = new ArrayList<>();
        for (String usuario : usuarios) lineas.add("USUARIO\t" + usuario);
        for (Mensaje mensaje : mensajes) lineas.add("MENSAJE\t" + mensaje.aArchivo());
        Files.write(archivo, lineas, StandardCharsets.UTF_8);
    }

    public boolean existe(String nombre) {
        return usuarios.contains(nombre);
    }

    public String registrar(String alias) throws IOException {
        String nombre = normalizarUsuario(alias);
        if (!existe(nombre)) {
            agregar("USUARIO\t" + nombre);
            usuarios.add(nombre);
        }
        return nombre;
    }

    public ArrayList<String> getUsuarios() {
        return new ArrayList<>(usuarios);
    }

    public ArrayList<Mensaje> historial(String usuario) {
        ArrayList<Mensaje> resultado = new ArrayList<>();
        for (Mensaje mensaje : mensajes) {
            if (mensaje.getEmisor().equals(usuario) || mensaje.getDestinatario().equals(usuario)) {
                resultado.add(mensaje);
            }
        }
        return resultado;
    }

    public Mensaje guardar(String emisor, String destinatario, String texto) throws IOException {
        validarTexto(texto);
        if (!existe(destinatario) || emisor.equals(destinatario)) {
            throw new IllegalArgumentException("Selecciona otro usuario registrado.");
        }
        Mensaje mensaje = new Mensaje(siguienteId, System.currentTimeMillis(), emisor, destinatario, texto);
        agregar("MENSAJE\t" + mensaje.aArchivo());
        siguienteId++;
        mensajes.add(mensaje);
        return mensaje;
    }

    private Mensaje buscarMensaje(long id) {
        for (Mensaje mensaje : mensajes) {
            if (mensaje.getId() == id) return mensaje;
        }
        return null;
    }

    public Mensaje marcarLeido(String destinatario, long id) throws IOException {
        Mensaje mensaje = buscarMensaje(id);
        if (mensaje == null || !mensaje.getDestinatario().equals(destinatario)) {
            throw new IllegalArgumentException("Solo puedes leer los mensajes que recibiste.");
        }
        if (!mensaje.isLeido()) {
            mensaje.setLeido(true);
            try {
                guardarTodo();
            } catch (IOException e) {
                mensaje.setLeido(false);
                throw e;
            }
        }
        return mensaje;
    }

    public static void validarTexto(String texto) {
        if (texto.isBlank() || texto.length() > 4000 || texto.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Escribe entre 1 y 4000 caracteres; no se admite texto vacío ni NUL.");
        }
    }

    public static String normalizarUsuario(String nombre) {
        String valor = Normalizer.normalize(nombre.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        if (!valor.matches("[\\p{L}\\p{N}_-]{2,24}")) {
            throw new IllegalArgumentException(
                    "El alias debe tener de 2 a 24 letras, números, guiones o guiones bajos.");
        }
        return valor;
    }
}
