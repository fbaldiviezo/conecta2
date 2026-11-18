public class Mensaje {
    private final long id;
    private final long fecha;
    private final String emisor;
    private final String destinatario;
    private final String texto;
    private boolean leido;

    public Mensaje(String[] p) {
        if (p.length != 7)
            throw new IllegalArgumentException("Mensaje incompleto.");
        id = Long.parseLong(p[1]);
        fecha = Long.parseLong(p[2]);
        emisor = p[3];
        destinatario = p[4];
        leido = Boolean.parseBoolean(p[5]);
        texto = restaurarSaltos(p[6]);
    }

    public long getId() {
        return id;
    }

    public long getFecha() {
        return fecha;
    }

    public String getEmisor() {
        return emisor;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getTexto() {
        return texto;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public String contactoDe(String usuario) {
        return emisor.equals(usuario) ? destinatario : emisor;
    }

    // Se conservan los saltos de línea de los historiales anteriores.
    public static String enUnaLinea(String texto) {
        return texto.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
    }

    public static String restaurarSaltos(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            if (caracter == '\\' && i + 1 < texto.length()) {
                char siguiente = texto.charAt(i + 1);
                if (siguiente == 'n' || siguiente == 'r' || siguiente == '\\') {
                    caracter = siguiente == 'n' ? '\n' : siguiente == 'r' ? '\r' : '\\';
                    i++;
                }
            }
            resultado.append(caracter);
        }
        return resultado.toString();
    }
}
