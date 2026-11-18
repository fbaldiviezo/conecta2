# Conecta2

Chat Java con **dos proyectos Eclipse independientes**, cliente y servidor. Usa Swing, sockets TCP, un hilo por conexión y archivos de texto UTF-8, sin base de datos ni dependencias adicionales.

El acceso y el chat comparten cabeceras verdes, fondo beige y botones verde vivo. El acceso muestra solo alias, servidor, puerto y el botón Entrar. El chat sigue la interfaz suministrada, con contadores rojos, historial HTML y botón triangular.

## Ejecutar

Necesitas JDK 21 o superior, con java, javac y jar en PATH. Los JAR incluidos están actualizados; puedes iniciar directamente:

```powershell
.\iniciar-servidor.ps1
```

En otra consola, abre un cliente; repite para abrir más ventanas:

```powershell
.\iniciar-cliente.ps1
```

Usa alias distintos, como ana y bob, dirección localhost y puerto 3333. Selecciona un contacto, escribe y pulsa Enter o el botón triangular. La escritura es de una sola línea, según el nuevo diseño. Salir vuelve al acceso; si se corta la conexión, cambia a Entrar.

Para compilar después de modificar el código, cierra antes las consolas y ventanas de Conecta2:

```powershell
.\compilar.ps1
```

Puedes compilar un proyecto por separado con -Proyecto cliente o -Proyecto servidor. Si PowerShell bloquea un script, ejecútalo así:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar-servidor.ps1
```

También puedes usar los JAR directamente:

```powershell
java -jar conecta2-servidor/Conecta2-Servidor.jar 3333 conecta2-servidor/datos
java -jar conecta2-cliente/Conecta2-Cliente.jar
```

## Abrir en Eclipse

1. Ve a **File > Import > General > Existing Projects into Workspace**.
2. Importa conecta2-cliente y conecta2-servidor.
3. Configura Java 21 o superior y codificación UTF-8.
4. Ejecuta Servidor.java y luego Cliente.java como Java Application.

Las clases permanecen en el paquete por defecto, como en el ejemplo del docente. VentanaChatRed adapta el diseño recibido a esa estructura. Ambos proyectos compilan independientemente.

En Eclipse, el servidor acepta los argumentos "puerto directorioDatos". La ruta relativa se resuelve desde el directorio de ejecución; usa una ruta absoluta para compartir los mismos datos que los scripts.

## Comunicación y datos

Las órdenes son texto legible: INGRESAR, ENVIAR, LEER y SALIR. El contenido y los nombres se transmiten y guardan sin Base64. [El protocolo](docs/PROTOCOLO.md) incluye ejemplos y explica las respuestas.

Los scripts guardan en conecta2-servidor/datos:

| Archivo | Contenido |
| --- | --- |
| usuarios.txt | Un alias por línea. |
| mensajes.txt | ID, fecha, emisor, destinatario y texto legible, separados con tabulaciones. |
| lecturas.txt | IDs de mensajes leídos. |

Usa un solo servidor por carpeta de datos. La versión con `FileLock` y los pasos para recuperarla están en [Bloqueo de archivos](docs/BLOQUEO.md).

Los saltos de línea del historial anterior se conservan usando \n y \r; una barra invertida se representa con \\. Estas sustituciones permiten mantener una orden por línea. Los mensajes normales se ven directamente, por ejemplo ana, bob y Hola.

Se convirtieron los **9 mensajes existentes**, conservando usuarios, IDs y lecturas. La copia original anterior a esa conversión está en referencias/respaldo-datos-antes-texto-20261108. Esa copia usa el formato antiguo; no la reemplaces directamente sobre el archivo nuevo.

Los mensajes privados, el historial por contacto, los envíos a usuarios desconectados y los contadores siguen funcionando. Abrir una conversación con la ventana activa marca sus mensajes como leídos. Los mensajes propios muestran Guardado o Leído.

El acceso sigue siendo por alias, sin contraseña. Los datos viajan en texto para la práctica en una red de confianza. Para respaldar, detén el servidor y copia juntos los tres archivos .txt. Si un envío quedó sin confirmar, revisa el historial antes de repetirlo.

## Estructura

| Ubicación | Contenido |
| --- | --- |
| conecta2-cliente/src | Cliente, ClientThread, VentanaConexion, VentanaChatRed, Mensaje y Estilo. |
| conecta2-servidor/src | Servidor, ServerThread, ArchivoDatos y Mensaje. Los alias se guardan como cadenas y se validan en ArchivoDatos. |
| docs/PROTOCOLO.md | Comunicación actual en texto, con ejemplos. |
| docs/VERIFICACION.md | Comprobaciones de funcionamiento. |
| docs/PROYECTO.md y docs/Conecta2-propuesta.pdf | Propuesta inicial del 31/10, anterior a esta simplificación. |
| referencias | Enunciado original, ejemplo del docente y respaldo anterior a la conversión. |

La interfaz ya no requiere Avatar, BurbujaMensaje, RenderContacto y Contacto de la primera versión. Las carpetas bin se generan al compilar; tmp contiene solo material de comprobación local.

## Ejecutar entre equipos

Inicia el servidor en un equipo y usa su IP local en los otros clientes. Todos deben usar el mismo puerto:

```powershell
.\iniciar-servidor.ps1 -Puerto 4444
.\iniciar-cliente.ps1 -Servidor 192.168.1.20 -Puerto 4444
```

## Seguir el código para la defensa

El envío pasa por VentanaChatRed.enviarMensaje, ClientThread.enviar, ServerThread.run, Servidor.enviarMensaje y ArchivoDatos.guardar. El servidor responde MENSAJE y VentanaChatRed.recibir actualiza la pantalla. La lectura usa LEER con el ID y una respuesta LEIDO para ambos participantes.

La nueva ventana mantiene los métodos del diseño: configurarVentana, inicializarComponentes y asignarEventos. No carga datos de prueba: los contactos y mensajes llegan del servidor real.
