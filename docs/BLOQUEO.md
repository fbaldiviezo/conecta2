# Bloqueo de la carpeta de datos

La versión de las pruebas usa un solo proceso servidor por carpeta de datos. Dentro de ese proceso, los métodos `synchronized` de `Servidor` ordenan las escrituras de los clientes. Si se ejecutan dos procesos sobre la misma carpeta, ambos pueden asignar el mismo ID o escribir en el mismo archivo.

La implementación anterior abre `servidor.lock` con `FileChannel` al arrancar e intenta obtener un bloqueo exclusivo mediante `tryLock()`. Si otro servidor con bloqueo ya usa esa carpeta, el segundo se detiene. Al cerrar el canal, se libera el bloqueo y el archivo puede reutilizarse. [Copia de ArchivoDatos con bloqueo](../referencias/bloqueo-archivos/ArchivoDatos.java).

## Recuperar la versión con bloqueo

1. Detén los servidores que usen la carpeta de datos. Desde la raíz del proyecto, restaura la clase guardada.

   ```powershell
   Copy-Item -LiteralPath .\referencias\bloqueo-archivos\ArchivoDatos.java -Destination .\conecta2-servidor\src\ArchivoDatos.java
   ```

2. En `conecta2-servidor/src/Servidor.java`, vuelve a declarar `ArchivoDatos` dentro del `try` del socket para que Java libere el bloqueo al salir. El inicio del bloque queda así.

   ```java
   try (ArchivoDatos datos = new ArchivoDatos(ruta);
        ServerSocket serverSocket = new ServerSocket(puerto)) {
       Servidor servidor = new Servidor(datos);
   ```

3. Recompila el servidor desde la raíz con JDK 21 o superior y ejecuta `iniciar-servidor.ps1`.

   ```powershell
   New-Item -ItemType Directory -Force -Path .\tmp\servidor-con-bloqueo | Out-Null
   $fuentesBloqueo = Get-ChildItem -LiteralPath .\conecta2-servidor\src -Filter '*.java'
   javac --release 21 -encoding UTF-8 -d .\tmp\servidor-con-bloqueo @($fuentesBloqueo.FullName)
   if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del servidor.' }
   jar --create --file .\conecta2-servidor\Conecta2-Servidor.jar --main-class Servidor -C .\tmp\servidor-con-bloqueo .
   ```

Todas las instancias que intenten usar la misma carpeta deben emplear la variante con bloqueo. Los archivos de usuarios, mensajes y lecturas se pueden usar directamente con ambas variantes.
