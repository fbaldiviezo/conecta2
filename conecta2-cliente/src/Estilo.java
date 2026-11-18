import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Estilo {
    public static final Color VERDE = new Color(0, 128, 105);
    public static final Color ACENTO = new Color(37, 211, 102);
    public static final Color TEXTO = new Color(17, 27, 33);
    public static final Color SECUNDARIO = new Color(90, 105, 110);

    public static JLabel etiqueta(String texto, int tamano, Color color, boolean negrita) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("Segoe UI", negrita ? Font.BOLD : Font.PLAIN, tamano));
        etiqueta.setForeground(color);
        return etiqueta;
    }

    public static JButton boton(String texto) {
        JButton boton = new JButton(texto);
        boton.setBackground(ACENTO);
        boton.setForeground(VERDE);
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(11, 18, 11, 18));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    public static String html(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
