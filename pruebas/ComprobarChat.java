import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ComprobarChat {
    private static VentanaChatRed chat;
    private static Path temporal;

    public static void main(String[] args) throws Exception {
        Path raiz = Path.of(args[0]).toAbsolutePath();
        temporal = raiz.resolve("tmp/casos-interfaz-" + UUID.randomUUID());
        Files.createDirectories(temporal);
        int puerto;
        try (ServerSocket reserva = new ServerSocket(0)) { puerto = reserva.getLocalPort(); }
        Process servidor = new ProcessBuilder("java", "-jar", raiz.resolve("conecta2-servidor/Conecta2-Servidor.jar").toString(),
                String.valueOf(puerto), temporal.resolve("datos").toString())
                .redirectErrorStream(true).redirectOutput(temporal.resolve("servidor.log").toFile()).start();
        int codigo = 0;
        try {
            Par bob = null;
            for (int i = 0; i < 60 && bob == null; i++) {
                try { bob = new Par(puerto, "bob"); } catch (IOException e) { Thread.sleep(75); }
            }
            if (bob == null) throw new AssertionError("No inicio el servidor");
            try (Par receptor = bob; Par carol = new Par(puerto, "carol")) {
                SwingUtilities.invokeAndWait(() -> {
                    for (Object clave : UIManager.getDefaults().keySet().toArray()) {
                        if (UIManager.get(clave) instanceof javax.swing.plaf.FontUIResource) {
                            UIManager.put(clave, new javax.swing.plaf.FontUIResource("Segoe UI", Font.PLAIN, 14));
                        }
                    }
                    Cliente.main(new String[] {"localhost", String.valueOf(puerto), "ana"});
                });
                esperar(() -> {
                    for (Window ventana : Window.getWindows()) {
                        if (ventana instanceof VentanaChatRed && ventana.isShowing()) {
                            chat = (VentanaChatRed) ventana;
                            return true;
                        }
                    }
                    return false;
                }, "Acceso con cliente real");
                esperar(() -> editor().isEditable(), "Escritura habilitada sin seleccionar");
                SwingUtilities.invokeAndWait(() -> {
                    JList<?> lista = componentes(chat, JList.class).get(0);
                    if (lista.getModel().getSize() != 0 || lista.getSelectedValue() != null) {
                        throw new AssertionError("El chat debe empezar sin contactos ni seleccion");
                    }
                    captura(chat, "todos.png");
                    editor().setText("Hola a todos");
                    editor().postActionEvent();
                });
                if (!receptor.hasta("MENSAJE")[6].equals("Hola a todos")
                        || !carol.hasta("MENSAJE")[6].equals("Hola a todos")) {
                    throw new AssertionError("El mensaje general no llego a los dos clientes");
                }
                esperar(() -> editor().getText().isEmpty() && editor().isEditable(), "Confirmacion del envio general");
                SwingUtilities.invokeAndWait(() -> {
                    if (componentes(chat, JList.class).get(0).getModel().getSize() != 0) {
                        throw new AssertionError("Enviar a todos no debe crear contactos de mensajes no recibidos");
                    }
                });
                String hola = "¡Hola, Ana! Ya podemos conversar.";
                String especial = "Texto literal: <b>HTML & etiquetas</b>\nY rutas como C:\\nueva\\ruta";
                receptor.enviar("ENVIAR\tana\t" + Mensaje.enUnaLinea(hola));
                receptor.hasta("MENSAJE");
                receptor.enviar("ENVIAR\tana\t" + Mensaje.enUnaLinea(especial));
                receptor.hasta("MENSAJE");
                carol.enviar("ENVIAR\tana\t¿Practicamos la defensa mañana?");
                carol.hasta("MENSAJE");
                esperar(() -> etiquetaCliente("bob").contains("(2)") && etiquetaCliente("carol").contains("(1)"),
                        "Contadores separados");
                SwingUtilities.invokeAndWait(() -> {
                    JList<?> lista = componentes(chat, JList.class).get(0);
                    lista.setSelectedIndex(indice(lista, "bob"));
                    chat.toFront();
                    chat.requestFocus();
                });
                esperar(() -> !etiquetaCliente("bob").contains("(2)") && !etiquetaCliente("bob").contains("(1)"), "Lectura al abrir");
                esperar(() -> etiquetaCliente("carol").contains("(1)"), "Otro contacto conserva pendientes");
                esperar(() -> documento().contains("<b>HTML & etiquetas</b>") && documento().contains("C:\\nueva\\ruta"),
                        "HTML literal y rutas visibles");
                int[] cambiosDeSeleccion = {0};
                SwingUtilities.invokeAndWait(() -> {
                    if (!editor().getText().isEmpty()) throw new AssertionError("El placeholder no debe ser texto del campo");
                    editor().setText("Borrador sin enviar");
                    JList<?> lista = componentes(chat, JList.class).get(0);
                    lista.addListSelectionListener(e -> {
                        if (!e.getValueIsAdjusting()) cambiosDeSeleccion[0]++;
                    });
                });
                try (Par nuevo = new Par(puerto, "david")) {
                    esperar(() -> componentes(chat, JList.class).get(0).getModel().getSize() == 2,
                            "La lista contiene solo a quienes enviaron mensajes");
                    SwingUtilities.invokeAndWait(() -> {
                        JList<?> lista = componentes(chat, JList.class).get(0);
                        if (!"bob".equals(lista.getSelectedValue()) || cambiosDeSeleccion[0] != 0
                                || !editor().getText().equals("Borrador sin enviar")) {
                            throw new AssertionError("Actualizar contactos altero la seleccion o el borrador");
                        }
                    });
                }
                SwingUtilities.invokeAndWait(() -> editor().requestFocusInWindow());
                esperar(() -> editor().isFocusOwner(), "Enfocar escritura");
                SwingUtilities.invokeAndWait(() -> {
                    editor().setText("¡Perfecto, Bob! Todo está en texto legible.");
                    botonEnviar().doClick();
                    if (editor().isEditable() || editor().getText().isEmpty()
                            || componentes(chat, JList.class).get(0).isEnabled()) {
                        throw new AssertionError("Se debe conservar y bloquear el envio hasta su confirmacion");
                    }
                    editor().postActionEvent();
                });
                String[] respuesta = receptor.hasta("MENSAJE");
                if (!respuesta[3].equals("ana") || !respuesta[6].contains("texto legible")) throw new AssertionError("Envio incorrecto");
                esperar(() -> editor().getText().isEmpty() && editor().isEditable(), "Confirmacion limpia y desbloquea el campo");
                long mensajesPropios = Files.readAllLines(temporal.resolve("datos/mensajes.txt"), StandardCharsets.UTF_8)
                        .stream().filter(linea -> linea.contains("\tana\tbob\t")).count();
                if (mensajesPropios != 2) throw new AssertionError("Enter repetido duplico el mensaje pendiente");
                receptor.enviar("LEER\t" + respuesta[1]);
                receptor.hasta("LEIDO");
                esperar(() -> documento().contains("Leído"), "Lectura del mensaje propio");
                SwingUtilities.invokeAndWait(() -> {
                    captura(chat, "chat.png");
                    chat.setSize(600, 420);
                    chat.validate();
                });
                Thread.sleep(250);
                SwingUtilities.invokeAndWait(() -> captura(chat, "chat-minimo.png"));

                // El placeholder escrito por el usuario también debe poder enviarse.
                SwingUtilities.invokeAndWait(() -> editor().requestFocusInWindow());
                esperar(() -> editor().isFocusOwner(), "Volver al editor");
                SwingUtilities.invokeAndWait(() -> {
                    editor().setText("Escritura del mensaje");
                    editor().postActionEvent();
                });
                if (!receptor.hasta("MENSAJE")[6].equals("Escritura del mensaje")) throw new AssertionError("Placeholder no enviable");
                esperar(() -> editor().getText().isEmpty() && editor().isEditable(), "Ultimo mensaje confirmado");
                SwingUtilities.invokeAndWait(() -> {
                    boton(chat, "Todos").doClick();
                    if (componentes(chat, JList.class).get(0).getSelectedValue() != null || !editor().isEditable()) {
                        throw new AssertionError("Todos debe quitar la seleccion y permitir escribir");
                    }
                    componentes(chat, JList.class).get(0).setSelectedIndex(0);
                    editor().setText("bye");
                    editor().postActionEvent();
                });
                if (!receptor.hasta("MENSAJE")[6].equals("ana salió del chat.")) {
                    throw new AssertionError("bye debe avisar al contacto activo y salir");
                }
                for (int i = 1; i <= 2; i++) {
                    receptor.enviar("ENVIAR\tana\tGuardado sin conexión " + i);
                    receptor.hasta("ENVIADO");
                }
                SwingUtilities.invokeAndWait(() -> {
                    for (Window ventana : Window.getWindows()) ventana.dispose();
                    chat = null;
                    Cliente.main(new String[] {"localhost", String.valueOf(puerto), "ana"});
                });
                esperar(() -> {
                    for (Window ventana : Window.getWindows()) {
                        if (ventana instanceof VentanaChatRed && ventana.isShowing()) {
                            chat = (VentanaChatRed) ventana;
                            return true;
                        }
                    }
                    return false;
                }, "Reconexion del cliente");
                esperar(() -> etiquetaCliente("bob").contains("(2)"), "Dos pendientes recuperados al volver");
                SwingUtilities.invokeAndWait(() -> {
                    JList<?> lista = componentes(chat, JList.class).get(0);
                    lista.setSelectedIndex(indice(lista, "bob"));
                    chat.toFront();
                    chat.requestFocus();
                });
                esperar(() -> documento().contains("Guardado sin conexión 1")
                        && documento().contains("Guardado sin conexión 2")
                        && documento().contains("¡Hola, Ana!"), "Historial antiguo y nuevo en la misma conversacion");
                esperar(() -> !etiquetaCliente("bob").contains("(2)") && !etiquetaCliente("bob").contains("(1)"),
                        "Lecturas confirmadas tras reconectar");
                SwingUtilities.invokeAndWait(() -> {
                    captura(chat, "reconexion.png");
                    boton(chat, "Salir").doClick();
                    for (Window ventana : Window.getWindows()) ventana.dispose();
                });
            }
            System.out.println("OK: acceso por alias, lista inicial vacia, envio a todos y privado, contadores y lecturas,");
            System.out.println("    borrador, confirmacion sin duplicados, Todos, bye y recuperacion de pendientes al reconectar");
            System.out.println("Capturas: " + temporal);
        } catch (Throwable e) {
            codigo = 1;
            e.printStackTrace();
        } finally {
            servidor.destroy();
            servidor.waitFor();
            SwingUtilities.invokeAndWait(() -> {
                for (Window ventana : Window.getWindows()) ventana.dispose();
            });
        }
        System.exit(codigo);
    }

    private static String etiquetaCliente(String nombre) {
        JList lista = componentes(chat, JList.class).get(0);
        int i = indice(lista, nombre);
        if (i < 0) return "";
        return ((JLabel) lista.getCellRenderer().getListCellRendererComponent(lista,
                lista.getModel().getElementAt(i), i, false, false)).getText();
    }

    private static String documento() {
        JTextPane area = componentes(chat, JTextPane.class).get(0);
        try { return area.getDocument().getText(0, area.getDocument().getLength()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void esperar(BooleanSupplier condicion, String descripcion) throws Exception {
        for (int i = 0; i < 80; i++) {
            boolean[] listo = {false};
            SwingUtilities.invokeAndWait(() -> listo[0] = condicion.getAsBoolean());
            if (listo[0]) return;
            Thread.sleep(75);
        }
        throw new AssertionError(descripcion);
    }

    private static int indice(JList<?> lista, String alias) {
        for (int i = 0; i < lista.getModel().getSize(); i++) {
            if (lista.getModel().getElementAt(i).equals(alias)) return i;
        }
        return -1;
    }
    private static JTextField editor() { return componentes(chat, JTextField.class).get(0); }
    private static JButton botonEnviar() {
        for (JButton boton : componentes(chat, JButton.class)) {
            if ("Enviar mensaje".equals(boton.getAccessibleContext().getAccessibleName())) return boton;
        }
        throw new AssertionError("Boton enviar ausente");
    }
    private static JButton boton(Container raiz, String texto) {
        for (JButton boton : componentes(raiz, JButton.class)) {
            if (texto.equals(boton.getText())) return boton;
        }
        throw new AssertionError("Boton ausente: " + texto);
    }
    private static <T> java.util.List<T> componentes(Container raiz, Class<T> tipo) {
        java.util.List<T> resultado = new ArrayList<>();
        for (Component componente : raiz.getComponents()) {
            if (tipo.isInstance(componente)) resultado.add(tipo.cast(componente));
            if (componente instanceof Container contenedor) resultado.addAll(componentes(contenedor, tipo));
        }
        return resultado;
    }
    private static void captura(JFrame ventana, String nombre) {
        ventana.validate();
        BufferedImage imagen = new BufferedImage(ventana.getWidth(), ventana.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D dibujo = imagen.createGraphics();
        ventana.paint(dibujo);
        dibujo.dispose();
        try { ImageIO.write(imagen, "png", temporal.resolve(nombre).toFile()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private static class Par implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader entrada;
        private final BufferedWriter salida;
        Par(int puerto, String alias) throws IOException {
            socket = new Socket("localhost", puerto);
            socket.setSoTimeout(5000);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            enviar("INGRESAR\t" + alias);
            hasta("LISTO");
        }
        void enviar(String linea) throws IOException {
            salida.write(linea + "\n");
            salida.flush();
        }
        String[] hasta(String tipo) throws IOException {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                String[] p = linea.split("\t", 7);
                if (p[0].equals("ERROR")) throw new IOException(p[1]);
                if (p[0].equals(tipo)) return p;
            }
            throw new EOFException();
        }
        public void close() throws IOException { socket.close(); }
    }
}
