import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

// El servidor coordina el acceso a esta clase mediante sus métodos synchronized.
public class ArchivoDatos {
    private final Path directorio;
    private final ArrayList<String> usuarios = new ArrayList<>();
    private final ArrayList<Mensaje> mensajes = new ArrayList<>();
    private long siguienteId = 1;

    public ArchivoDatos(Path directorio) throws IOException {
        this.directorio = directorio.toAbsolutePath();
        Files.createDirectories(this.directorio);
        cargar();
    }

    private void cargar() throws IOException {
        int numero = 0;
        for (String linea : lineas("usuarios.txt")) {
            numero++;
            try {
                String nombre = normalizarUsuario(linea);
                if (!nombre.equals(linea) || existe(linea)) {
                    throw new IllegalArgumentException("Alias repetido o sin normalizar");
                }
                usuarios.add(nombre);
            } catch (RuntimeException e) {
                throw invalido("usuarios.txt", numero, e);
            }
        }
        numero = 0;
        long ultimo = 0;
        for (String linea : lineas("mensajes.txt")) {
            numero++;
            if (numero == 1 && linea.equals("# Conecta2 - mensajes en texto")) continue;
            try {
                String[] p = linea.split("\t", 5);
                if (p.length != 5)
                    throw new IllegalArgumentException("Se esperaban 5 campos");
                Mensaje mensaje = new Mensaje(Long.parseLong(p[0]), Long.parseLong(p[1]),
                        p[2], p[3], Mensaje.restaurarSaltos(p[4]));
                if (mensaje.getId() <= ultimo || mensaje.getId() == Long.MAX_VALUE
                        || mensaje.getFecha() <= 0 || !existe(mensaje.getEmisor())
                        || !existe(mensaje.getDestinatario())
                        || mensaje.getEmisor().equals(mensaje.getDestinatario())) {
                    throw new IllegalArgumentException("Identificador, fecha o usuarios inválidos");
                }
                validarTexto(mensaje.getTexto());
                ultimo = mensaje.getId();
                mensajes.add(mensaje);
            } catch (RuntimeException e) {
                throw invalido("mensajes.txt", numero, e);
            }
        }
        siguienteId = ultimo + 1;
        numero = 0;
        for (String linea : lineas("lecturas.txt")) {
            numero++;
            try {
                Mensaje mensaje = buscarMensaje(Long.parseLong(linea));
                if (mensaje == null)
                    throw new IllegalArgumentException("Mensaje inexistente");
                mensaje.setLeido(true);
            } catch (RuntimeException e) {
                throw invalido("lecturas.txt", numero, e);
            }
        }
    }

    private List<String> lineas(String archivo) throws IOException {
        Path ruta = directorio.resolve(archivo);
        if (!Files.exists(ruta)) {
            Files.writeString(ruta, archivo.equals("mensajes.txt") ? "# Conecta2 - mensajes en texto\n" : "",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        return Files.readAllLines(ruta, StandardCharsets.UTF_8);
    }

    private IOException invalido(String archivo, int linea, Exception causa) {
        return new IOException(archivo + ": línea " + linea + " inválida: " + causa.getMessage(), causa);
    }

    private void agregar(String archivo, String texto) throws IOException {
        Files.writeString(directorio.resolve(archivo), texto, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public boolean existe(String nombre) {
        return usuarios.contains(nombre);
    }

    public String registrar(String alias) throws IOException {
        String nombre = normalizarUsuario(alias);
        if (!existe(nombre)) {
            agregar("usuarios.txt", nombre + "\n");
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
        agregar("mensajes.txt", mensaje.aArchivo() + "\n");
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
            agregar("lecturas.txt", id + "\n");
            mensaje.setLeido(true);
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
