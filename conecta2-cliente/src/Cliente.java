import java.awt.Font;
import javax.swing.*;

public class Cliente {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            for (Object clave : UIManager.getDefaults().keySet().toArray()) {
                if (UIManager.get(clave) instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(clave, new javax.swing.plaf.FontUIResource("Segoe UI", Font.PLAIN, 14));
                }
            }
            new VentanaConexion(args.length > 0 ? args[0] : "localhost",
                    args.length > 1 ? args[1] : "3333").setVisible(true);
        });
    }
}
