# Conecta2: análisis y propuesta de implementación

Documento de diseño previo a la codificación · 31 de octubre de 2026

> Documento histórico. El 8 de noviembre se simplificaron las órdenes y se eliminó Base64; el chat utiliza VentanaChatRed con el diseño suministrado por el usuario. La descripción vigente está en README.md y docs/PROTOCOLO.md. La pantalla de acceso se conserva.

## 1. Qué se va a construir

Conecta2 será una aplicación de escritorio Java para conversar de forma privada entre varias personas conectadas a un servidor. La pantalla tendrá una lista de contactos a la izquierda, el nombre del contacto activo arriba, el historial en el centro y un área de escritura abajo. El aspecto estará inspirado en WhatsApp: verdes, fondo claro, avatares con iniciales y burbujas alineadas según el emisor.

El objetivo académico es que el código se pueda explicar y modificar durante la defensa. Se utilizarán únicamente bibliotecas del JDK: Swing para la interfaz, sockets TCP para la comunicación, hilos para atender conexiones y archivos de texto UTF-8 para guardar información.

## 2. Requisitos encontrados en el PDF

Fuente: `proyecto - copia.pdf`, tres páginas, Sistema de Mensajería Conecta2, Programación en Red, UAJMS, Lic. Octavio Aguilar M.

1. Mostrar el nombre del cliente que envía los mensajes (área 1).
2. Mostrar el cliente seleccionado para conversar (área 2).
3. Mostrar los contactos y los mensajes pendientes entre paréntesis de color rojo (área 3).
4. Mostrar solamente el historial del contacto seleccionado (área 4).
5. Permitir escribir y enviar mensajes a ese contacto (área 5).
6. Comunicar varias personas mediante Java con interfaz gráfica.
7. Guardar usuarios y mensajes en archivos de texto, sin base de datos.

La página 3 presenta WhatsApp como referencia visual. Se preservará especialmente la regla del contador rojo entre paréntesis, aunque el ejemplo visual de WhatsApp utilice insignias verdes. El PDF también establece la entrega en Moodle un día antes y una evaluación presencial: presentación 30% y modificación del código 70%. No indica una fecha concreta; esa fecha deberá confirmarse en clase.

## 3. Estilo observado en elzapato

Se revisaron las cuatro clases fuente, los archivos de configuración de Eclipse y la estructura de los dos proyectos. Los archivos `.class` y `.jar` son las salidas compiladas de la referencia.

`Servidor.java` abre un ServerSocket en el puerto 3333, acepta conexiones dentro de un while y crea un ServerThread por conexión. `ServerThread.java` extiende Thread, lee líneas mediante BufferedReader y envía mediante BufferedWriter. `Cliente.java` abre un Socket y envía líneas. `ClientThread.java` extiende Thread y recibe en segundo plano. Las clases están en el paquete por defecto y el proyecto usa Java 21 y UTF-8.

La implementación conservará esos nombres principales, el uso directo de sockets, métodos claros, atributos privados y clases pequeñas sin frameworks. Se añadirán clases de modelo, persistencia e interfaz para evitar concentrar todo en un solo archivo. Se corregirán problemas del ejemplo: lectura de null al desconectar, acceso concurrente a la lista de clientes, cierre de recursos y bloqueo de la aceptación mientras se espera el nombre de un usuario. El envío general del ejemplo se sustituirá por envío al destinatario elegido.

## 4. Dos proyectos independientes

`conecta2-servidor` será un proyecto Eclipse Java ejecutable por consola. Tendrá Servidor, ServerThread, ArchivoDatos, Usuario y Mensaje. Será responsable de registrar alias, controlar sesiones, dirigir mensajes, guardar archivos y mantener los estados de lectura.

`conecta2-cliente` será otro proyecto Eclipse Java, ejecutable con interfaz Swing. Tendrá Cliente, ClientThread, VentanaConexion, VentanaChat, Contacto, Mensaje y componentes visuales. Será responsable de presentar contactos, conversaciones y estados, y enviar las acciones del usuario al servidor.

Cada proyecto tendrá su propio `src`, `bin`, `.project`, `.classpath` y configuración Java 21. No habrá referencias entre proyectos ni un tercer proyecto compartido. Se podrá compilar cada uno por separado y ejecutar varios clientes desde el mismo JAR. La carpeta `elzapato` se conservará como referencia.

## 5. Supuestos para los puntos no definidos

Se implementarán conversaciones individuales de texto; grupos, llamadas, imágenes y adjuntos quedan fuera del alcance inicial. Se usará un alias de 2 a 24 letras, números, guiones o guiones bajos. Se normalizará a minúsculas, sin distinguir Ana de ana. El primer ingreso registra el alias y los siguientes recuperan su historial.

El acceso por alias no incorpora contraseña ni verifica identidad: está pensado para una demostración en una red de confianza. Cualquier persona que conozca un alias sin sesión activa puede entrar con él. El protocolo TCP tampoco cifra el tráfico. No se presentará esta versión como un servicio público ni como un equivalente de la seguridad de WhatsApp.

Solo habrá una sesión simultánea por alias. Se podrán enviar mensajes a contactos registrados que estén desconectados. Los usuarios nuevos aparecerán en la lista de todos los clientes. No se permitirá enviarse mensajes a uno mismo ni escribir a un alias inexistente.

Un mensaje se considera no leído hasta que el destinatario abre su conversación con la ventana activa. Se guardará la lectura en el servidor para conservarla tras reconectar. El servidor confirmará el envío únicamente después de guardarlo. El cliente mostrará una marca para guardado y dos para leído; no habrá un estado independiente de entrega al dispositivo.

El servidor escuchará en el puerto 3333 por defecto; se podrá cambiar por argumento. El cliente permitirá indicar dirección y puerto. Los mensajes tendrán hasta 4000 caracteres, incluidos saltos de línea. Enter enviará y Shift+Enter insertará una línea. Al perder la conexión se informará al usuario y se ofrecerá volver al acceso; no se reenviará automáticamente un mensaje cuyo resultado sea incierto.

## 6. Comunicación y concurrencia

Cada conexión utilizará texto UTF-8 delimitado por saltos de línea. Los campos se separarán con tabulaciones y los textos variables viajarán codificados en Base64 para que las tabulaciones o saltos de línea del mensaje no rompan el protocolo. Base64 es una codificación, no un cifrado.

Las órdenes principales serán HELLO (ingresar), SEND (enviar), READ (marcar conversación leída) y BYE (salir). Las respuestas incluirán WELCOME, USERS, MSG, HISTORY_BEGIN, HISTORY_END, READ_OK, RECEIPT y ERROR. Los identificadores y las fechas de los mensajes los asignará el servidor; la identidad del emisor se tomará de la conexión, nunca de un campo enviado por el cliente.

Servidor aceptará sockets y ServerThread procesará cada conexión. El alta, el envío y la lectura se coordinarán mediante métodos synchronized para mantener orden y consistencia. Las escrituras de cada socket también se serializarán. Un límite de longitud de línea y un tiempo máximo para el saludo evitarán entradas excesivas y conexiones sin identificar.

En el cliente, ClientThread recibirá en segundo plano y una cola de salida enviará sin bloquear Swing. Las modificaciones visuales se ejecutarán en el Event Dispatch Thread. El historial recibido se distinguirá de los mensajes nuevos para evitar notificaciones falsas al ingresar.

## 7. Archivos y recuperación

Solo el servidor guardará los datos persistentes. Por defecto utilizará `datos` dentro del proyecto servidor al iniciarlo con los scripts suministrados; se podrá elegir otra ruta mediante argumento.

`usuarios.txt` guardará un alias por línea. `mensajes.txt` guardará identificador, fecha en milisegundos, emisor, destinatario y contenido; los tres textos estarán en Base64. `lecturas.txt` guardará los identificadores de mensajes leídos. Todos serán archivos de texto UTF-8 inspeccionables sin un motor de base de datos.

Al arrancar, el servidor cargará usuarios, mensajes y lecturas, validará las relaciones y continuará desde el último identificador. Si un archivo contiene datos inválidos, detendrá el inicio con un error y su número de línea, para no ocultar una pérdida de información. Una operación que no se pueda guardar devolverá un error al cliente. Los respaldos se harán con el servidor detenido, copiando juntos los tres archivos. Esta persistencia sencilla no ofrece transacciones ni recuperación ante un corte de energía a mitad de escritura.

## 8. Interfaz prevista

La ventana de acceso incluirá alias, dirección del servidor y puerto, con validación y mensajes de conexión claros. La ventana principal mostrará el usuario propio y un botón para salir, una búsqueda de contactos, indicador de conexión y contadores rojos. El área de conversación tendrá nombre y presencia del contacto, burbujas, hora, fecha y confirmaciones de lectura.

Los mensajes propios estarán a la derecha con fondo verde claro; los recibidos, a la izquierda con fondo blanco. No se incluirán botones de llamadas o adjuntos sin funcionamiento. La búsqueda filtrará los contactos por alias. El historial podrá desplazarse y los mensajes largos se ajustarán al ancho disponible.

## 9. Orden de implementación y comprobación

1. Crear los dos proyectos Eclipse y los scripts de compilación y ejecución.
2. Implementar modelos, archivos y servidor con un hilo por cliente.
3. Comprobar mensajes privados, múltiples clientes, alias duplicados, entradas inválidas y desconexiones.
4. Implementar conexión y pantalla de chat Swing con el aspecto acordado.
5. Comprobar historial, mensajes sin conexión, lecturas y reinicio del servidor.
6. Generar los JAR y una guía para abrirlos en Eclipse o ejecutarlos por consola.

Las pruebas de integración usarán sockets reales y directorios temporales, sin alterar los datos de uso. Incluirán tres usuarios para comprobar que una conversación privada no llega a un tercero, recuperación tras reinicio y persistencia de no leídos. La interfaz se comprobará mediante una captura y una prueba de interacción cuando el entorno permita abrir ventanas.

## 10. Criterios de aceptación y defensa

El trabajo estará listo para una demostración cuando puedan abrirse un servidor y al menos tres clientes independientes; enviar mensajes privados; cambiar de contacto sin mezclar historiales; aumentar y limpiar los contadores; recibir mensajes enviados durante una desconexión; y cerrar y abrir el servidor conservando usuarios, mensajes y lecturas.

La guía explicará cómo seguir un mensaje desde el botón Enviar hasta el archivo y el destinatario. También señalará dónde cambiar el puerto, el límite de caracteres, los colores y las reglas de validación. La simplicidad de la arquitectura facilitará las modificaciones presenciales exigidas por el docente.
