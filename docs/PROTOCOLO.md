# Comunicación de Conecta2 en texto

Actualización: 8 de noviembre de 2026.

El cliente y el servidor usan sockets TCP y texto UTF-8. Cada orden ocupa una línea; las partes se separan con una tabulación. No se usa Base64 ni cifrado. Los alias, mensajes y errores se pueden leer directamente.

En los ejemplos, [TAB] representa una tabulación real.

## Lo que envía el cliente

```text
INGRESAR[TAB]ana
ENVIAR[TAB]bob[TAB]Hola, ¿cómo estás?
LEER[TAB]15
SALIR
```

- INGRESAR: registra el alias si es nuevo o recupera su historial. Solo se admite una conexión simultánea por alias.
- ENVIAR: indica destinatario y texto. El servidor toma el emisor de la conexión, guarda y notifica a los dos participantes.
- LEER: indica el ID de un mensaje recibido. El servidor comprueba que pertenece al destinatario, guarda la lectura y avisa a ambos.
- SALIR: desconecta. Cerrar el socket produce el mismo efecto.

## Lo que responde el servidor

```text
BIENVENIDO[TAB]ana
CLIENTES[TAB]ana[TAB]true[TAB]bob[TAB]false
MENSAJE[TAB]15[TAB]1794186000000[TAB]ana[TAB]bob[TAB]false[TAB]Hola, ¿cómo estás?
LEIDO[TAB]15
LISTO
ERROR[TAB]Ese alias ya está conectado.
```

MENSAJE lleva ID, fecha en milisegundos, emisor, destinatario, estado de lectura y texto. Se usa tanto para el historial como para mensajes nuevos. LEIDO actualiza el mismo mensaje en los dos clientes y reemplaza las dos confirmaciones distintas de la versión anterior.

Al ingresar, el servidor envía BIENVENIDO, los mensajes del historial, CLIENTES y finalmente LISTO. Ese último aviso permite cargar la historia sin marcarla como leída antes de abrir una conversación. CLIENTES también se envía cuando alguien entra, sale o se registra; contiene pares de alias y presencia (true o false).

## Texto y separadores

El texto del mensaje es el último campo. Se usa split("\t", 3) al recibir ENVIAR y split("\t", 7) al recibir MENSAJE: una tabulación dentro del texto no crea campos extra.

Para conservar los saltos de línea anteriores, solo se representan tres caracteres de manera visible: un salto como \n, un retorno como \r y una barra invertida como \\. Los métodos enUnaLinea y restaurarSaltos hacen esas sustituciones. Permiten enviar una orden por línea y conservar literalmente textos como C:\nueva\ruta; no ocultan el contenido. Acentos, nombres y emojis viajan como texto UTF-8.

## Archivos

- usuarios.txt: un alias por línea.
- mensajes.txt: cabecera "# Conecta2 - mensajes en texto", seguida por filas id[TAB]fecha[TAB]emisor[TAB]destinatario[TAB]texto.
- lecturas.txt: un ID leído por línea.

La versión actual usa un servidor por carpeta de datos. La variante con bloqueo se conserva en [Bloqueo de archivos](BLOQUEO.md).

El servidor confirma un mensaje después de guardarlo. Si la conexión se corta antes de recibir la confirmación, revisa el historial al reconectar antes de reenviarlo.

## Dónde seguir la lógica

1. VentanaChatRed.enviarMensaje construye ENVIAR.
2. ClientThread.enviar lo escribe por el socket.
3. ServerThread.run separa los campos y llama al servidor.
4. Servidor.enviarMensaje guarda y notifica.
5. VentanaChatRed.recibir añade MENSAJE al historial.

La lectura utiliza LEER con un ID y una única respuesta LEIDO. No hay intervalos de IDs ni confirmaciones separadas para emisor y destinatario.
