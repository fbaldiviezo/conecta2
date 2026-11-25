import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class VentanaChatRed extends JFrame {
    // Diseño recibido: cabeceras verdes, lista sencilla y mensajes en HTML.
    private static final Color WA_HEADER_BG = new Color(0, 128, 105);
    private static final Color WA_CHAT_BG = new Color(236, 229, 221);
    private static final Color WA_GREEN_BTN = new Color(37, 211, 102);
    private static final Color WA_INPUT_BG = new Color(240, 242, 245);
    private static final Color WA_SELECTED_ITEM = new Color(233, 237, 239);
    private static final String PLACEHOLDER = "Escritura del mensaje";

    private final String usuario;
    private final ClientThread conexion;
    private final ArrayList<Mensaje> mensajes = new ArrayList<>();
    private final ArrayList<String> conectados = new ArrayList<>();
    private final ArrayList<Long> lecturasPendientes = new ArrayList<>();
    private JLabel lblClienteActivo;
    private DefaultListModel<String> modeloClientes;
    private JList<String> listaClientes;
    private JTextPane areaMensajes;
    private JTextField txtMensaje;
    private JButton btnEnviar;
    // private JButton btnSalir;
    // Botón Todos oculto: el envío general solo está permitido al iniciar la sesión.
    // private JButton btnTodos;
    private String contactoActivo;
    private JLabel lblEstado;
    private boolean cargando = true;
    private boolean conectado = true;
    private boolean cerrando;
    private boolean enviando;

    public VentanaChatRed(String usuario, ClientThread conexion) {
        this.usuario = usuario;
        this.conexion = conexion;
        configurarVentana();
        inicializarComponentes();
        asignarEventos();
        mostrarHistorial();
        actualizarEntrada();
    }

    private void configurarVentana() {
        setTitle("Programación en Red - " + usuario);
        setSize(750, 520);
        setMinimumSize(new Dimension(600, 420));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void inicializarComponentes() {
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setPreferredSize(new Dimension(260, 0));
        panelIzquierdo.setBorder(new MatteBorder(0, 0, 0, 1, new Color(210, 215, 218)));

        JPanel headerIzquierdo = crearCabecera();
        JLabel lblClienteEmisor = crearTitulo(usuario);
        lblClienteEmisor.setToolTipText("Cliente emisor de mensaje");
        headerIzquierdo.add(lblClienteEmisor, BorderLayout.CENTER);
        // btnTodos = new JButton("Todos");
        // btnTodos.setBackground(WA_HEADER_BG);
        // btnTodos.setForeground(Color.WHITE);
        // btnTodos.setFocusPainted(false);
        // headerIzquierdo.add(btnTodos, BorderLayout.EAST);

        modeloClientes = new DefaultListModel<>();
        listaClientes = new JList<>(modeloClientes);
        listaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaClientes.setBackground(Color.WHITE);
        listaClientes.setFixedCellHeight(50);
        listaClientes.getAccessibleContext().setAccessibleName("Lista de clientes");
        listaClientes.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
                String nombre = value.toString();
                int pendientes = contarNoLeidos(nombre);
                String contador = pendientes == 0 ? "" : " <font color='#E53935'><b>(" + pendientes + ")</b></font>";
                label.setText("<html>" + Estilo.html(nombre) + contador + "</html>");
                label.setToolTipText(conectados.contains(nombre) ? "En línea" : "Desconectado");
                label.setBackground(isSelected ? WA_SELECTED_ITEM : Color.WHITE);
                label.setForeground(new Color(17, 27, 33));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                label.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(0, 0, 1, 0, WA_INPUT_BG), new EmptyBorder(0, 16, 0, 8)));
                return label;
            }
        });
        JScrollPane scrollLista = new JScrollPane(listaClientes);
        scrollLista.setBorder(null);
        panelIzquierdo.add(headerIzquierdo, BorderLayout.NORTH);
        panelIzquierdo.add(scrollLista, BorderLayout.CENTER);

        JPanel panelDerecho = new JPanel(new BorderLayout());
        JPanel headerDerecho = crearCabecera();
        lblClienteActivo = crearTitulo("Todos");
        headerDerecho.add(lblClienteActivo, BorderLayout.CENTER);
        // btnSalir = new JButton("Salir");
        // btnSalir.setFocusPainted(false);
        // btnSalir.setForeground(Color.WHITE);
        // btnSalir.setBackground(WA_HEADER_BG);
        // headerDerecho.add(btnSalir, BorderLayout.EAST);

        areaMensajes = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        areaMensajes.setContentType("text/html");
        areaMensajes.setEditable(false);
        areaMensajes.setBackground(WA_CHAT_BG);
        areaMensajes.setBorder(new EmptyBorder(12, 12, 12, 12));
        areaMensajes.getAccessibleContext().setAccessibleName("Historial de mensajes");
        JScrollPane scrollMensajes = new JScrollPane(areaMensajes);
        scrollMensajes.setBorder(null);
        scrollMensajes.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel panelInferior = new JPanel(new BorderLayout(10, 6));
        panelInferior.setBackground(WA_INPUT_BG);
        panelInferior.setBorder(new EmptyBorder(10, 12, 6, 12));
        txtMensaje = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    g.setColor(Color.GRAY);
                    FontMetrics fuente = g.getFontMetrics();
                    g.drawString(PLACEHOLDER, getInsets().left,
                            (getHeight() - fuente.getHeight()) / 2 + fuente.getAscent());
                }
            }
        };
        txtMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMensaje.setBorder(new EmptyBorder(10, 12, 10, 12));
        txtMensaje.getAccessibleContext().setAccessibleName("Escritura del mensaje");
        btnEnviar = new JButton(new Icon() {
            public int getIconWidth() {
                return 22;
            }

            public int getIconHeight() {
                return 22;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(c.isEnabled() ? WA_HEADER_BG : Color.GRAY);
                g.fillPolygon(new int[] { x + 3, x + 20, x + 3 }, new int[] { y + 2, y + 11, y + 20 }, 3);
            }
        });
        btnEnviar.setBackground(WA_GREEN_BTN);
        btnEnviar.setFocusPainted(false);
        btnEnviar.setToolTipText("Enviar mensaje");
        btnEnviar.getAccessibleContext().setAccessibleName("Enviar mensaje");
        lblEstado = new JLabel("Cargando historial…");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEstado.setForeground(new Color(90, 105, 110));
        panelInferior.add(txtMensaje, BorderLayout.CENTER);
        panelInferior.add(btnEnviar, BorderLayout.EAST);
        panelInferior.add(lblEstado, BorderLayout.SOUTH);
        panelDerecho.add(headerDerecho, BorderLayout.NORTH);
        panelDerecho.add(scrollMensajes, BorderLayout.CENTER);
        panelDerecho.add(panelInferior, BorderLayout.SOUTH);

        add(panelIzquierdo, BorderLayout.WEST);
        add(panelDerecho, BorderLayout.CENTER);
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(WA_HEADER_BG);
        panel.setPreferredSize(new Dimension(0, 55));
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));
        return panel;
    }

    private JLabel crearTitulo(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return label;
    }

    private void asignarEventos() {
        btnEnviar.addActionListener(e -> enviarMensaje());
        txtMensaje.addActionListener(e -> enviarMensaje());
        // btnSalir.addActionListener(e -> salir());
        // btnTodos.addActionListener(e -> listaClientes.clearSelection());
        listaClientes.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String contacto = listaClientes.getSelectedValue();
                if (contacto == null) {
                    if (contactoActivo != null) listaClientes.setSelectedValue(contactoActivo, false);
                    return;
                }
                contactoActivo = contacto;
                conexion.enviar("ABRIR\t" + contacto);
                actualizarTitulo();
                mostrarHistorial();
                actualizarEntrada();
                marcarLecturas();
            }
        });
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                marcarLecturas();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });
    }

    public void recibir(String linea) {
        if (cerrando)
            return;
        try {
            String orden = linea.split("\t", 2)[0];
            switch (orden) {
                case "CLIENTES":
                    actualizarClientes(linea.split("\t"));
                    break;
                case "MENSAJE":
                    Mensaje mensaje = new Mensaje(linea.split("\t", 7));
                    if (buscarMensaje(mensaje.getId()) != null)
                        break;
                    mensajes.add(mensaje);
                    if (mensaje.getDestinatario().equals(usuario) && !modeloClientes.contains(mensaje.getEmisor())) {
                        modeloClientes.addElement(mensaje.getEmisor());
                    }
                    if (!cargando) {
                        listaClientes.repaint();
                        if (mensaje.contactoDe(usuario).equals(listaClientes.getSelectedValue()))
                            mostrarHistorial();
                        marcarLecturas();
                    }
                    break;
                case "ENVIADO":
                    enviando = false;
                    txtMensaje.setText("");
                    lblEstado.setText("Mensaje guardado");
                    actualizarEntrada();
                    break;
                case "LISTO":
                    cargando = false;
                    listaClientes.repaint();
                    lblEstado.setText("Conectado · Sin seleccionar un cliente envías a todos");
                    actualizarEntrada();
                    mostrarHistorial();
                    marcarLecturas();
                    break;
                case "LEIDO":
                    long id = Long.parseLong(linea.split("\t")[1]);
                    lecturasPendientes.remove(Long.valueOf(id));
                    Mensaje leido = buscarMensaje(id);
                    if (leido != null) {
                        leido.setLeido(true);
                        listaClientes.repaint();
                        if (leido.contactoDe(usuario).equals(listaClientes.getSelectedValue()))
                            mostrarHistorial();
                    }
                    break;
                case "ERROR":
                    enviando = false;
                    lecturasPendientes.clear();
                    actualizarEntrada();
                    lblEstado.setText("No se completó la operación");
                    JOptionPane.showMessageDialog(this, linea.split("\t", 2)[1], "Conecta2",
                            JOptionPane.WARNING_MESSAGE);
                    break;
                default:
                    throw new IllegalArgumentException("Respuesta desconocida");
            }
        } catch (RuntimeException e) {
            conexion.cerrar();
        }
    }

    private void actualizarClientes(String[] campos) {
        conectados.clear();
        for (int i = 1; i + 1 < campos.length; i += 2) {
            String nombre = campos[i];
            if (Boolean.parseBoolean(campos[i + 1]))
                conectados.add(nombre);
        }
        listaClientes.repaint();
        actualizarTitulo();
    }

    private Mensaje buscarMensaje(long id) {
        for (Mensaje mensaje : mensajes) {
            if (mensaje.getId() == id)
                return mensaje;
        }
        return null;
    }

    private int contarNoLeidos(String contacto) {
        int cantidad = 0;
        for (Mensaje mensaje : mensajes) {
            if (mensaje.getEmisor().equals(contacto) && mensaje.getDestinatario().equals(usuario)
                    && !mensaje.isLeido())
                cantidad++;
        }
        return cantidad;
    }

    private void enviarMensaje() {
        String destino = contactoActivo;
        if (!conectado || cargando || enviando)
            return;
        String texto = txtMensaje.getText();
        if (texto.strip().equalsIgnoreCase("bye")) {
            salir();
            return;
        }
        if (texto.isBlank())
            return;
        if (texto.length() > 4000 || texto.indexOf('\0') >= 0) {
            JOptionPane.showMessageDialog(this, "El mensaje admite hasta 4000 caracteres y no admite NUL.");
            return;
        }
        enviando = true;
        lblEstado.setText("Enviando…");
        actualizarEntrada();
        if (!conexion.enviar("ENVIAR\t" + (destino == null ? "" : destino) + "\t" + Mensaje.enUnaLinea(texto))) {
            desconectado("No se pudo enviar el mensaje.");
        }
    }

    private void marcarLecturas() {
        if (!conectado || cargando || !isFocused())
            return;
        String contacto = listaClientes.getSelectedValue();
        for (Mensaje mensaje : mensajes) {
            if (mensaje.getEmisor().equals(contacto) && mensaje.getDestinatario().equals(usuario)
                    && !mensaje.isLeido() && !lecturasPendientes.contains(mensaje.getId())) {
                if (conexion.enviar("LEER\t" + mensaje.getId()))
                    lecturasPendientes.add(mensaje.getId());
            }
        }
    }

    private void actualizarTitulo() {
        String contacto = listaClientes.getSelectedValue();
        lblClienteActivo.setText(contacto == null ? "Todos" : contacto);
        lblClienteActivo.setToolTipText(contacto == null ? "Enviar a todos los clientes conectados"
                : conectados.contains(contacto) && conectado ? "En línea" : "Desconectado");
    }

    private void actualizarEntrada() {
        boolean disponible = conectado && !cargando && !enviando;
        txtMensaje.setEditable(disponible);
        btnEnviar.setEnabled(disponible);
        listaClientes.setEnabled(!enviando);
        // btnTodos.setEnabled(!enviando);
    }

    private void mostrarHistorial() {
        String contacto = listaClientes.getSelectedValue();
        StringBuilder historialHtml = new StringBuilder();
        historialHtml.append("<html><body style='font-family:Segoe UI; font-size:11pt; color:#111b21;'>");
        if (contacto != null) {
            historialHtml.append("<table width='100%' cellspacing='8' cellpadding='0'>");
            for (Mensaje mensaje : mensajes) {
                if (!mensaje.contactoDe(usuario).equals(contacto))
                    continue;
                boolean propio = mensaje.getEmisor().equals(usuario);
                String hora = DateTimeFormatter.ofPattern("dd/MM HH:mm").format(
                        Instant.ofEpochMilli(mensaje.getFecha()).atZone(ZoneId.systemDefault()));
                // Escapar HTML evita que el texto recibido se ejecute como etiquetas.
                String texto = Estilo.html(mensaje.getTexto().replaceAll("(\\S{28})(?=\\S)", "$1\n"))
                        .replace("\r", "").replace("\n", "<br>").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
                historialHtml.append("<tr><td align='").append(propio ? "right" : "left").append("'>")
                        .append("<table width='88%' cellpadding='9' cellspacing='0'><tr><td bgcolor='")
                        .append(propio ? "#d9fdd3" : "#ffffff").append("'>")
                        .append(texto).append("<br><font size='2' color='#667781'>")
                        .append(hora).append(propio ? (mensaje.isLeido() ? " · Leído" : " · Guardado") : "")
                        .append("</font></td></tr></table></td></tr>");
            }
            historialHtml.append("</table>");
        }
        historialHtml.append("</body></html>");
        areaMensajes.setText(historialHtml.toString());
        // Las lecturas y llegadas se muestran al final de la conversación.
        SwingUtilities.invokeLater(() -> areaMensajes.setCaretPosition(areaMensajes.getDocument().getLength()));
    }

    public void desconectado(String motivo) {
        if (cerrando || !conectado)
            return;
        conectado = false;
        actualizarEntrada();
        actualizarTitulo();
        lblEstado.setText("Sin conexión");
        JOptionPane.showMessageDialog(this, motivo + (enviando
                ? "\nEl texto se conserva. Revisa el historial antes de reenviarlo."
                : ""),
                "Conecta2", JOptionPane.WARNING_MESSAGE);
    }

    private void salir() {
        cerrando = true;
        conexion.salir();
        dispose();
    }
}
