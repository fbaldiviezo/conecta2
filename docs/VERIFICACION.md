# Verificación de Conecta2

## Actualización del 8 de noviembre de 2026

Se compilaron los dos proyectos y sus JAR con --release 21, usando el JDK 25 disponible. El JAR del cliente contiene solo las clases actuales; se excluyeron las salidas antiguas de la interfaz reemplazada.

Se comprobó el protocolo en texto con tres clientes y sockets TCP reales: acceso sin bloqueo por un saludo incompleto, alias duplicado, privacidad, acentos y emoji, tabulaciones, saltos de línea y barras invertidas literales. Se verificaron destinatarios inválidos, autoenvío, texto vacío o excesivo, órdenes inválidas, mensajes sin conexión y recuperación tras reiniciar. También se simuló un fallo de escritura y no se confirmó un mensaje que no pudo guardarse.

La lectura usa una respuesta común para emisor y destinatario. Se comprobó que un tercero no puede marcar un mensaje ajeno como leído y que repetir una lectura no duplica su registro persistente.

La prueba de Swing se ejecutó contra el servidor real, entrando por VentanaConexion. Verificó VentanaChatRed, contadores por contacto, lectura al seleccionar, envío por botón y Enter, confirmación que limpia el campo y salida hacia el acceso. También comprobó que las etiquetas HTML recibidas se muestran literalmente, las rutas con barras no se alteran y el texto del placeholder puede enviarse si el usuario lo escribe.

Se inspeccionaron las capturas del acceso y del chat en 750 × 520 y 600 × 420. El acceso conserva su diseño. El chat sigue las cabeceras, colores, lista y campo de escritura suministrados.

El historial existente se convirtió con un bloqueo de datos y copia previa de los tres archivos en referencias/respaldo-datos-antes-texto-20261108. Se conservaron los 9 mensajes, sus IDs, usuarios y lecturas.

El registro siguiente corresponde a la primera implementación. Sus referencias a Base64 y las pruebas antiguas describen la versión anterior.

---

# Registro de la implementación inicial

Fecha: 31 de octubre de 2026. Entorno: Windows, Eclipse Temurin JDK 25.0.4; compilación con `--release 21` y UTF-8. Los `.class` tienen versión 65, correspondiente a Java 21. No se ejecutó un runtime Java 21 separado; la ejecución se verificó con el JDK disponible.

## Compilación

`compilar.ps1` compiló ambos proyectos de manera independiente y generó `Conecta2-Servidor.jar` y `Conecta2-Cliente.jar`. El cliente no requiere clases del proyecto servidor ni viceversa. Las configuraciones de Eclipse corresponden a dos proyectos Java 21 separados.

## Integración con servidor real: 12 comprobaciones aprobadas

1. Ingreso de tres usuarios, normalización de alias y aceptación de nuevos clientes aunque otro socket no complete su saludo.
2. Rechazo de una segunda conexión con el mismo alias, sin distinguir mayúsculas.
3. Envío privado con acentos, emoji, saltos de línea y tabulaciones; un tercer usuario no recibe el contenido.
4. Persistencia de la lectura, confirmación al lector y aviso al emisor; repetir READ no crea lecturas nuevas.
5. Rechazo de destinatario inexistente, autoenvío, texto vacío o excesivo, Base64 inválido y orden desconocida.
6. Mensaje enviado con el destinatario desconectado y recuperación de la historia con estados de lectura correctos.
7. Fallo real de escritura del archivo sin devolver una confirmación falsa del mensaje.
8. Veinticuatro envíos concurrentes con identificadores únicos y orden consistente para el destinatario.
9. Rechazo de otro proceso servidor sobre el mismo directorio de datos.
10. Reinicio del servidor conservando usuarios, 26 mensajes y 25 mensajes pendientes.
11. Segundo reinicio conservando la lectura de una conversación y los 12 pendientes de otra.
12. Detección de una fila corrupta al arrancar, con número de línea y salida de error.

Comando reproducible: `python pruebas/integracion.py`, después de compilar. Usa únicamente la biblioteca estándar de Python y directorios aislados bajo `tmp`; no modifica los datos de uso del servidor.

## Cliente Swing contra servidor real

Se ejecutó `PruebaInterfaz.java`, que abre el acceso y se conecta utilizando el ClientThread real. Se comprobaron el inicio, la recepción de mensajes de dos usuarios, los contadores separados, la lectura al abrir una conversación, la conservación de los pendientes de otra, el envío desde el botón, la confirmación que limpia el editor, la lectura de nuevos mensajes y la búsqueda de contactos. La prueba termina saliendo de la sesión y cerrando el servidor temporal.

Se inspeccionaron las imágenes del acceso y del chat en 1120 × 760 y 900 × 600. Se corrigieron las marcas de lectura para dibujarlas con Graphics2D sin depender de glifos de la fuente. El historial dispone de desplazamiento vertical cuando no cabe a tamaño mínimo.

Comando reproducible en una sesión de Windows con escritorio: `probar.ps1 -Interfaz`. Esta orden compila, ejecuta las 12 comprobaciones y agrega la prueba gráfica. Las imágenes se generan en `tmp/interfaz-*` como material de revisión local.

## Documento de propuesta

El PDF de cuatro páginas se generó antes de la codificación y se revisaron visualmente todas sus páginas. El texto editable está en `docs/PROYECTO.md`. La propuesta documenta los requisitos del enunciado y distingue las decisiones asumidas para sus lagunas.

## Alcance de la verificación

Las pruebas se realizaron en un equipo usando sockets TCP reales por localhost. No se ha probado entre equipos físicos distintos ni se ha abierto el proyecto en una sesión de Eclipse. La configuración de Eclipse y la versión del bytecode se comprobaron en archivos. Las limitaciones de acceso por alias, ausencia de cifrado y almacenamiento sencillo están explicadas en la propuesta y en README.md.
