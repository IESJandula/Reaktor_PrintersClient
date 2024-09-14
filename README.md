
# PrintersClient

El proyecto `PrintersClient` es un microservicio diseñado para coordinar con [PrintersServer](https://github.com/IESJandula/Reaktor_PrintersServer) y gestionar las tareas de impresión en una red. Este proyecto depende de [BaseServer](https://github.com/IESJandula/Base_Server/) para funcionalidades comunes. PrintersClient se encarga de forma de periódica de preguntar a PrintersServer si hay tareas de impresión por realizar. Además, informa también de forma periódica el estado de las impresoras.

## Descripción de los Servicios y Componentes

### InicializacionSistema
`InicializacionSistema` es una clase encargada de la inicialización y configuración del sistema cuando se inicia el microservicio. Actualmente se encarga de montar la estructura de carpetas externa para el fat-jar, es decir, copiar el ejecutable de C# para que sea accesible por dicho fat-jar.

## Data Transfer Objects (DTOs)

Los DTOs son objetos que se utilizan para transferir datos entre los sistemas de cliente y servidor. En el proyecto `PrintersClient`, los DTOs en el paquete `dto` son:

- **DtoPrinter**: Representa los datos de una impresora local, como el nombre, el estado, la cola de impresión y otros detalles relevantes.
- **DtoPrintAction**: Utilizado para describir las acciones de impresión que deben realizarse, incluyendo el ID de la tarea, el usuario que la solicitó, el número de copias, si es en blanco y negro, orientación, etcétera.

## Clase `Print`

La clase `Print` se encarga de gestionar las tareas de impresión que se envían desde el servidor. Esta clase contiene los métodos necesarios para buscar tareas de impresión, procesarlas y comunicarse con el servidor para reportar el estado de las mismas.

### Principales Métodos de `Print`

- **`imprimir()`**: Método público que se ejecuta de manera periódica (según la configuración en el archivo `application.yml`). Este método solicita al servidor las tareas de impresión pendientes, las procesa e imprime los documentos.
- **`buscarTareaParaImprimir()`**: Método privado, llamado por imprimir(), que solicita al servidor las tareas de impresión pendientes y devuelve un objeto `DtoPrintAction` que contiene los detalles de la tarea.
- **`imprimirDocumento(DtoPrintAction dtoPrintAction)`**: Método privado, llamado por imprimir(), que realiza la impresión de un documento basado en la configuración proporcionada en el objeto `DtoPrintAction`.
- **`enviarRespuestaAlServidor()`**: Método privado, llamado por imprimir(), que envía al servidor el resultado de la impresión, ya sea éxito o error.

## Clase `SendPrinters`

`SendPrinters` es una clase que se encarga de enviar al servidor la información de las impresoras disponibles en el cliente.

### Principales Métodos de `SendPrinters`

- **`sendPrinters()`**: Método público diseñado para ejecutarse periódicamente. Recopila la información de todas las impresoras conectadas y disponibles en el sistema y envía esa información al servidor `PrintersServer`.
- **`enviarPeticionPost()`**: Método privado, llamado por sendPrinters(), que envía una solicitud HTTP POST al servidor con la lista de impresoras en formato JSON.

## Clase `PrinterInfoService`

`PrinterInfoService` es una clase que proporciona métodos para obtener información detallada sobre las impresoras disponibles en el cliente, llamando de forma secuencial a un programa diseñado en C#.

### Principales Métodos de `PrinterInfoService`

- **`obtenerInfoImpresora(PrintService printer)`**: Utiliza un comando de la línea de comandos para llamar a ConsoleApp1 y así obtener detalles sobre la impresora especificada, como su estado, cola de impresión, entre otros, y devuelve un objeto `DtoPrinter` con esta información.

## Aplicación `ConsoleApp1`

`ConsoleApp1` es una pequeña aplicación de consola escrita en C# que se utiliza para obtener información detallada sobre una impresora específica en Windows, como su estado, puerto de conexión, y dirección IP (si aplica). La aplicación ejecuta consultas WMI para recuperar esta información.

### Principales Funcionalidades de `ConsoleApp1`

- **Obtener el estado de la impresora**: Usa la clase `ManagementObjectSearcher` para consultar el estado de la impresora y otros detalles relacionados.
- **Convertir códigos de estado**: El método `ConvertStatus` convierte los códigos de estado numéricos de la impresora a mensajes descriptivos.

## Variables de Configuración

Las variables anotadas con `@Value` en este proyecto deben configurarse adecuadamente para que el microservicio `PrintersClient` funcione correctamente. Estas variables se definen en el archivo `application.yml` del microservicio.

### Detalles de las Variables `@Value` y cómo obtenerlas:

1. **`reaktor.printers_server_url`**: URL del servidor de impresoras.
   - **Cómo configurarlo**: Define la URL del servidor de impresoras, por ejemplo: `http://localhost:8083`.
   - **Dónde almacenarlo**: En el archivo `application.yml`.

2. **`reaktor.fixedDelayString.print`**: Intervalo de tiempo para ejecutar la tarea de impresión.
   - **Cómo configurarlo**: Define el tiempo de retraso en milisegundos entre ejecuciones de tareas de impresión.
   - **Dónde almacenarlo**: En el archivo `application.yml`.

3. **`reaktor.fixedDelayString.sendPrinters`**: Intervalo de tiempo para enviar información de impresoras.
   - **Cómo configurarlo**: Define el tiempo de retraso en milisegundos entre ejecuciones de tareas de envío de información de impresoras.
   - **Dónde almacenarlo**: En el archivo `application.yml`.

4. **`reaktor.bannedPrinters`**: Lista de nombres de impresoras excluidas, ya que por defecto, el ordenador donde tenemos instalado este software, devuelve impresoras software, no físicas, por ejemplo, "Imprimir a PDF".
   - **Cómo configurarlo**: Define los nombres de impresoras que no deben ser consideradas para las tareas de impresión.
   - **Dónde almacenarlo**: En el archivo `application.yml`.

## Dependencias

Este proyecto depende de [BaseServer](https://github.com/IESJandula/Base_Server/) para funcionalidades básicas como la autorización, almacenamiento de sesión y actualización de JARs.

## Creación de Elementos en la Colección de Firebase

Para que el sistema funcione correctamente, es necesario crear una colección en Firebase llamada `usuarios` donde se almacenarán los datos de los usuarios. Sigue los siguientes pasos para crear elementos en esta colección:

1. **Accede a la consola de Firebase**: Ve a [Firebase Console](https://console.firebase.google.com/) e inicia sesión con tu cuenta de Google.

2. **Selecciona tu proyecto**: Haz clic en tu proyecto para abrir el panel de control.

3. **Ve a Firestore Database**: En el menú de la izquierda, selecciona **Firestore Database** y haz clic en **Crear base de datos** si aún no lo has hecho. Asegúrate de seleccionar el modo de producción.

4. **Crear la colección `usuarios`**: 
   - Haz clic en **Iniciar colección** y escribe `usuarios` como nombre de la colección.
   - Haz clic en **Siguiente** para añadir el primer documento.

5. **Añadir un documento**:
   - Define el **ID del documento**: Este será el UID del usuario, que puedes obtener desde la pestaña de Firebase Authentication.
   - Añade los siguientes campos al documento:
     - **email** (tipo: `string`): El correo electrónico del usuario.
     - **nombre** (tipo: `string`): El nombre del usuario.
     - **apellidos** (tipo: `string`): Los apellidos del usuario.
     - **roles** (tipo: `array`): Lista de roles asignados al usuario, por ejemplo: `["CLIENTE_IMPRESORA"]`.

6. **Guardar el documento**: Haz clic en **Guardar** para crear el documento en la colección `usuarios`.

Repite estos pasos para cada usuario que necesites agregar al sistema.
