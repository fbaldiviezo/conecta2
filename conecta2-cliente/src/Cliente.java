import java.awt.Font;
import javax.swing.*;

public class Cliente {
    private ClientThread conexion;
    private VentanaChatRed chat;
    private String errorAcceso;

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].strip().matches("[\\p{L}\\p{N}_-]{2,24}")) {
            System.err.println("Uso: java -jar Cliente.jar <alias>");
            System.err.println("El alias debe tener de 2 a 24 letras, números, guiones o guiones bajos.");
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            for (Object clave : UIManager.getDefaults().keySet().toArray()) {
                if (UIManager.get(clave) instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(clave, new javax.swing.plaf.FontUIResource("Segoe UI", Font.PLAIN, 14));
                }
            }
            Cliente cliente = new Cliente();
            cliente.conexion = new ClientThread("localhost", 3333, args[0].strip(),
                    cliente::recibir, cliente::desconectado);
            cliente.conexion.start();
        });
    }

    private void recibir(String linea) {
        if (chat != null) {
            chat.recibir(linea);
            return;
        }
        String[] campos = linea.split("\t", 2);
        if (campos.length == 2 && campos[0].equals("BIENVENIDO")) {
            chat = new VentanaChatRed(campos[1], conexion);
            chat.setVisible(true);
        } else if (campos.length == 2 && campos[0].equals("ERROR")) {
            errorAcceso = campos[1];
            conexion.cerrar();
        }
    }

    private void desconectado(String motivo) {
        if (chat != null) {
            chat.desconectado(motivo);
        } else {
            System.err.println(errorAcceso == null ? motivo : errorAcceso);
            System.exit(1);
        }
    }
}
