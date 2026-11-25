# Conecta2 — contexto y decisiones del proyecto

Actualizado el 25 de noviembre de 2026. Este archivo resume las decisiones vigentes para continuar el trabajo. Contrastar siempre con el código actual y las nuevas instrucciones del usuario.

## Forma de trabajar

- El usuario quiere soluciones simples, fáciles de explicar y modificar en la defensa con el docente. Aplicar la skill local `.agents/skills/ponytail/SKILL.md`.
- Hacer cambios pequeños y completos. Usar las bibliotecas del JDK y la estructura existente. Evitar frameworks, capas y opciones innecesarias.
- Explicar los cambios y las comprobaciones en el chat, en español. Crear documentos adicionales cuando el usuario los solicite.
- Revisar los cambios locales antes de editar. Conservar las modificaciones del usuario, incluidos sus comentarios en la interfaz.
- El usuario eliminó los archivos de `pruebas/`. No restaurarlos automáticamente desde Git. Las comprobaciones posteriores se han hecho en `tmp/`, con datos aislados.

## Estructura y tecnología

- Dos proyectos Eclipse independientes, `conecta2-cliente` y `conecta2-servidor`, cada uno con su propio `src`, `bin` y configuración Eclipse.
- Java 21 como versión objetivo, fuentes UTF-8. Se ha compilado con JDK 25 usando `--release 21`.
- Clases en el paquete por defecto, siguiendo el estilo del ejemplo del docente en `referencias/elzapato`.
- Swing para la interfaz, sockets TCP para comunicar los proyectos y un `ServerThread` por conexión.
- Los proyectos compilan por separado. Cada uno tiene su propia clase `Mensaje`.

## Inicio y salida

- El cliente se ejecuta con exactamente un alias, por ejemplo `java -jar conecta2-cliente/Cliente.jar JOSE` desde la raíz.
- Alias ausente, inválido o argumentos extra producen un error en consola y código de salida `1`.
- El cliente conecta directamente con `localhost:3333`. La ventana `VentanaConexion` fue eliminada.
- Los alias admiten de 2 a 24 letras, números, guiones o guiones bajos. El servidor los normaliza con NFC y minúsculas. Una sola sesión simultánea por alias.
- `iniciar-cliente.ps1 -Alias JOSE` utiliza el mismo JAR y la misma entrada.
- El servidor se ejecuta con `java -jar conecta2-servidor/Conecta2-Servidor.jar 3333 conecta2-servidor/datos`.
- `iniciar-servidor.ps1` establece por defecto el puerto 3333 y la carpeta de datos dentro del proyecto servidor. Al ejecutar Java directamente, una ruta relativa depende del directorio de trabajo.
- Escribir `bye` o cerrar con la X termina el cliente. La salida envía las órdenes pendientes y `SALIR` antes de cerrar el socket.
- El botón Salir fue retirado por el usuario y quedó comentado. Conservar esa decisión.

## Reglas de conversación

- Cada sesión empieza sin contacto seleccionado y permite enviar a todos los demás clientes conectados.
- Un envío general incluye únicamente a quienes están conectados en ese momento. Los desconectados no reciben ni acumulan una copia de ese envío.
- Tras seleccionar un contacto, esa sesión queda en modo privado. Puede cambiar de contacto, pero no volver al envío general.
- El botón Todos está oculto y sus líneas están comentadas expresamente por pedido del usuario. Mantener comentadas su declaración, creación, incorporación a la ventana, evento y habilitación.
- `contactoActivo` conserva la selección aunque se intente quitar con ratón o teclado. El servidor también rechaza `ABRIR` vacío o un envío general después de elegir contacto.
- Los mensajes privados pueden enviarse a un usuario registrado desconectado. Se recuperan al volver a entrar.
- La lista de contactos se forma con quienes enviaron mensajes al usuario, incluido el historial recuperado. Comienza vacía si no existen mensajes recibidos.
- Abrir una conversación con la ventana activa marca como leídos sus mensajes recibidos. Los contadores se muestran en rojo entre paréntesis.
- El historial muestra los mensajes antiguos y nuevos de la conversación. Los mensajes propios indican Guardado o Leído.
- Al salir de una conversación privada, se guarda y envía un aviso al contacto seleccionado si sigue conectado. Una salida en modo general no genera ese aviso.

## Único archivo de datos vigente

La única base de datos activa es `conecta2-servidor/datos/datos.txt`. Contiene usuarios y mensajes en UTF-8, con tabulaciones reales entre campos.

```text
USUARIO\tjose
USUARIO\tpedro
MENSAJE\t1\t1795500000000\tjose\tpedro\tfalse\tHola Pedro
```

En este ejemplo `\t` representa una tabulación. Un mensaje contiene ID, fecha Unix en milisegundos, emisor, destinatario, leído y texto, precedidos por el tipo `MENSAJE`.

- El estado leído es estrictamente `true` o `false` y se guarda en el propio mensaje.
- El registro de un usuario se escribe antes de los mensajes que lo utilizan. Al reescribir, se guardan primero todos los usuarios y después los mensajes ordenados por ID.
- El registro y el envío agregan una línea al final. Marcar una lectura reescribe todo `datos.txt` mediante `guardarTodo()`.
- Se conserva la escritura directa, sin archivo temporal, por decisión del usuario. Un corte durante la reescritura puede dejar el archivo incompleto.
- Si falla el guardado de una lectura, se restaura su estado en memoria y se propaga el error. Las confirmaciones se envían después de guardar.
- Al cargar, se validan registros, alias, participantes, IDs, fechas y estados. Un archivo inválido detiene el inicio indicando la línea. El siguiente ID continúa desde el último guardado.
- El texto es legible. Se representan saltos y barras mediante `\n`, `\r` y `\\`. `enUnaLinea` y `restaurarSaltos` conservan esos caracteres sin Base64.
- El texto es el último campo. Mantener los límites de `split` para conservar las tabulaciones dentro del mensaje.
- Se admiten mensajes de hasta 4000 unidades UTF-16. Se rechazan texto vacío, solo espacios y NUL.
- Los antiguos `usuarios.txt`, `mensajes.txt`, `lecturas.txt` y `servidor.lock` se retiraron de la carpeta activa.
- La última unificación conservó 4 usuarios y 4 mensajes con sus estados. El usuario había autorizado reiniciar un historial anterior, pero la unificación más reciente conservó los datos existentes.

## Concurrencia y comunicación

- Se usará un solo proceso servidor por carpeta de datos. Los métodos `synchronized` de `Servidor` coordinan sus clientes.
- Se quitó `FileLock` por pedido del usuario. La copia histórica está en `referencias/bloqueo-archivos/ArchivoDatos.java`, con instrucciones en `docs/BLOQUEO.md`.
- Esa copia usa un formato antiguo de persistencia. Antes de recuperarla, adaptarla al archivo único actual y revisar sus instrucciones de cierre.
- El acceso es por alias, sin contraseña, y TCP transmite texto sin cifrado. El proyecto está destinado a una práctica en una red de confianza.
- `ClientThread` recibe en segundo plano, encola los envíos y entrega eventos a Swing mediante `invokeLater`.
- Su hilo receptor mantiene vivo el proceso hasta cerrar la conexión. Preservar la salida ordenada al modificar los hilos.

Las órdenes actuales del cliente son `INGRESAR`, `ENVIAR`, `ABRIR`, `LEER` y `SALIR`. `ENVIAR` lleva destinatario y texto, con destinatario vacío únicamente para el modo general inicial. `LEER` lleva el ID del mensaje recibido.

El servidor responde con `BIENVENIDO`, `CLIENTES`, `MENSAJE`, `LISTO`, `ENVIADO`, `LEIDO` y `ERROR`.

- `LISTO` termina la carga inicial del historial.
- `CLIENTES` publica pares de alias y presencia, que no se guardan como estado permanente.
- La línea `MENSAJE` tiene el mismo formato en el archivo y en el socket.
- `ENVIADO` confirma la operación completa. Un envío general produce varias copias `MENSAJE`, por lo que el cliente debe limpiar y desbloquear el editor al recibir `ENVIADO`.
- `LEIDO` notifica el mismo ID al lector y al emisor. Solo el destinatario puede marcarlo como leído.

## Interfaz y archivos de referencia

- La ventana activa es `VentanaChatRed`, con cabeceras verdes, fondo beige, lista a la izquierda, historial HTML y campo de una línea con botón triangular.
- Mantener el escape HTML al presentar mensajes recibidos. El texto de ayuda del campo se dibuja y no forma parte de su contenido.
- `docs/casosPrueba.pdf` contiene la secuencia del docente con JOSE, RAUL y PEDRO. Las instrucciones recientes del usuario sobre archivo único y envío general solo al inicio son posteriores a ese PDF.
- `docs/PROYECTO.md`, el PDF de propuesta, `README.md` y otras notas describen etapas anteriores. Verificar sus afirmaciones antes de usarlas, especialmente nombres de JAR, login, persistencia y botón Todos.

## Compilación y comprobación

- Compilar todos los `.java` de cada `src` con `javac --release 21 -encoding UTF-8 -d <carpeta-nueva> <fuentes>`.
- Usar carpetas de compilación nuevas dentro de `tmp/` para evitar empaquetar clases antiguas de `bin`, como la ventana de conexión eliminada.
- Empaquetar el cliente como `conecta2-cliente/Cliente.jar`, con `--main-class Cliente`, y el servidor como `conecta2-servidor/Conecta2-Servidor.jar`, con `--main-class Servidor`.
- Actualizar el JAR correspondiente después de modificar sus fuentes. Si Windows lo mantiene abierto, solicitar cerrar ese proceso antes de reemplazarlo.
- Para verificaciones de persistencia y sockets, usar una carpeta de datos aislada bajo `tmp` y puertos libres. Evitar introducir usuarios de prueba en los datos de uso.
- Las últimas comprobaciones están en `tmp/probar_archivo_unico.py`, `tmp/ProbarSeleccion.java` y `tmp/ProbarCargaUnica.java`. Son auxiliares ignorados por Git y pueden desaparecer al limpiar.
- Otros auxiliares antiguos de `tmp` pueden esperar los formatos de dos o tres archivos. Revisarlos antes de usarlos como referencia para una regresión.
- Cubrir archivo único, lectura tras reiniciar, texto con caracteres especiales, exclusión de desconectados del envío general, bloqueo del regreso a Todos y cambio entre contactos privados.
- Cuando cambie el inicio o la salida, comprobar argumentos obligatorios, `bye`, la X y terminación del proceso.

## Estado de Git al escribir este archivo

- Rama de trabajo `v2-casos-prueba`.
- `main` y `v1.0` apuntan a `074f1dd`, primera versión guardada.
- `v2.0` apunta a `76d446c`, adaptación inicial a los casos de prueba.
- Los cambios posteriores de entrada por alias, eliminación del login y botones, archivo único y selección irreversible siguen en el directorio de trabajo. No asumir que están incluidos en `v2.0`.
- El estado local incluye eliminaciones del usuario en `pruebas/`, cambios de JAR y el reemplazo del cliente antiguo por `Cliente.jar`. Inspeccionar `git status` antes de continuar.
- En sesiones anteriores, el entorno permitió leer `.git` pero rechazó escribir `index.lock`. El usuario pudo hacer los commits desde su consola. Si vuelve a ocurrir, informar el error concreto y comprobar después si el commit realmente existe.
- `bin`, los datos de uso y `tmp` están ignorados. `Cliente.jar` tiene una excepción en `.gitignore`. El JAR del servidor ya estaba versionado.
