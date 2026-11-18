public class Mensaje {
    private final long id;
    private final long fecha;
    private final String emisor;
    private final String destinatario;
    private final String texto;
    private boolean leido;

    public Mensaje(long id, long fecha, String emisor, String destinatario, String texto) {
        this.id = id;
        this.fecha = fecha;
        this.emisor = emisor;
        this.destinatario = destinatario;
        this.texto = texto;
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

    public String aArchivo() {
        return id + "\t" + fecha + "\t" + emisor + "\t" + destinatario + "\t" + enUnaLinea(texto);
    }

    public String aProtocolo() {
        return "MENSAJE\t" + id + "\t" + fecha + "\t" + emisor + "\t" + destinatario
                + "\t" + leido + "\t" + enUnaLinea(texto);
    }

    // Solo se representan los saltos de línea para no cortar una orden del socket.
    // Los nombres, acentos y el contenido del mensaje siguen siendo texto legible.
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
