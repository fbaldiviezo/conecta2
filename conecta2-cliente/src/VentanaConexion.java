import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class VentanaConexion extends JFrame {
    private final JTextField alias = new JTextField();
    private final JTextField host;
    private final JTextField puerto;
    private final JButton conectar = Estilo.boton("Entrar");
    private final JLabel estado = Estilo.etiqueta(" ", 12, Estilo.SECUNDARIO, false);
    private ClientThread conexion;
    private VentanaChatRed chat;
    private String errorAcceso;

    public VentanaConexion(String direccion, String numeroPuerto) {
        super("Conecta2");
        host = new JTextField(direccion);
        puerto = new JTextField(numeroPuerto);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 460);
        setMinimumSize(new Dimension(360, 460));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Estilo.VERDE);
        cabecera.setPreferredSize(new Dimension(0, 55));
        cabecera.setBorder(new EmptyBorder(10, 15, 10, 15));
        cabecera.add(Estilo.etiqueta("Conecta2", 15, Color.WHITE, true));
        raiz.add(cabecera, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(236, 229, 221));
        panel.setBorder(new EmptyBorder(20, 28, 4, 28));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        campo(panel, "Alias", alias);
        campo(panel, "Servidor", host);
        campo(panel, "Puerto", puerto);
        panel.add(Box.createVerticalGlue());
        raiz.add(panel, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 8));
        pie.setBackground(new Color(240, 242, 245));
        pie.setBorder(new EmptyBorder(10, 12, 10, 12));
        conectar.setPreferredSize(new Dimension(0, 42));
        pie.add(conectar, BorderLayout.CENTER);
        pie.add(estado, BorderLayout.SOUTH);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(conectar);
        conectar.addActionListener(e -> conectar());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (conexion != null)
                    conexion.cerrar();
            }
        });
    }

    private void agregar(JPanel panel, JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (componente instanceof JLabel) {
            componente.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    Math.max(componente.getPreferredSize().height, componente.getMaximumSize().height)));
        }
        panel.add(componente);
    }

    private void campo(JPanel panel, String titulo, JTextField campo) {
        agregar(panel, Estilo.etiqueta(titulo, 14, Estilo.TEXTO, true));
        panel.add(Box.createVerticalStrut(7));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        campo.setBackground(Color.WHITE);
        campo.setForeground(Estilo.TEXTO);
        campo.setBorder(new EmptyBorder(10, 12, 10, 12));
        agregar(panel, campo);
        panel.add(Box.createVerticalStrut(16));
    }

    private void conectar() {
        try {
            String nombre = alias.getText().strip();
            if (!nombre.matches("[\\p{L}\\p{N}_-]{2,24}")) {
                throw new IllegalArgumentException("El alias debe tener de 2 a 24 caracteres válidos.");
            }
            int numero = Integer.parseInt(puerto.getText().strip());
            if (numero < 1 || numero > 65535 || host.getText().isBlank()) {
                throw new IllegalArgumentException("Revisa la dirección y el puerto (1 a 65535).");
            }
            errorAcceso = null;
            conectar.setEnabled(false);
            estado.setText("Conectando con el servidor…");
            conexion = new ClientThread(host.getText().strip(), numero, nombre, this::recibir, this::desconectado);
            conexion.start();
        } catch (IllegalArgumentException e) {
            estado.setText("<html>"
                    + Estilo.html(e instanceof NumberFormatException ? "El puerto debe ser numérico." : e.getMessage())
                    + "</html>");
        }
    }

    private void recibir(String linea) {
        if (chat != null) {
            chat.recibir(linea);
            return;
        }
        String[] p = linea.split("\t", -1);
        if (p[0].equals("BIENVENIDO") && p.length == 2) {
            chat = new VentanaChatRed(p[1], conexion, host.getText().strip(),
                    puerto.getText().strip());
            chat.setVisible(true);
            dispose();
        } else if (p[0].equals("ERROR") && p.length == 2) {
            errorAcceso = p[1];
            estado.setText("<html>" + Estilo.html(errorAcceso) + "</html>");
            conexion.cerrar();
        }
    }

    private void desconectado(String motivo) {
        if (chat != null) {
            chat.desconectado(motivo);
        } else {
            conectar.setEnabled(true);
            estado.setText("<html>" + Estilo.html(errorAcceso != null ? errorAcceso : motivo) + "</html>");
        }
    }
}
