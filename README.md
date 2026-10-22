This is a Kotlin Multiplatform project targeting Android, iOS, Server.

* [/app/iosApp](./app/iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./app/shared/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Server: `./gradlew :server:run`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:shared:testAndroidHostTest`
- Server tests: `./gradlew :server:test`
- iOS tests: `./gradlew :app:shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
## Control de ventas

La aplicación incluye tres secciones:

- **Productos:** agrega nombre y precio, edita o elimina productos. Los productos del pedido abierto se bloquean para evitar cambios de precio durante la venta.
- **Nuevo pedido:** usa + y − para elegir cantidades y confirma con Cerrar pedido. Solo los pedidos confirmados cuentan como ventas.
- **Ventas:** consulta pedidos y total por fecha. La fecha se toma del dispositivo al confirmar la venta.

Los precios se calculan con centavos enteros. Cada venta conserva el nombre, precio y cantidad originales, aunque cambie el catálogo. Los productos y las ventas se guardan localmente (SharedPreferences en Android y NSUserDefaults en iOS). El pedido abierto permanece en memoria: confirma la venta antes de cerrar la aplicación. No hay sincronización entre dispositivos ni exportación en esta versión. Los importes se muestran con el símbolo $.

Validación Android:

```powershell
.\gradlew.bat :app:androidApp:assembleDebug :app:shared:testAndroidHostTest --no-configuration-cache
```

La compilación y las pruebas de iOS requieren macOS y Xcode.

### Categorías y fotos

En Agregar producto o Editar, escribe una categoría o usa Elegir existente. La subcategoría es opcional y sus opciones pertenecen a la categoría elegida. Una categoría nueva se crea al guardar el producto. Los productos anteriores aparecen como Sin categoría hasta que se editen.

Productos y Nuevo pedido permiten elegir primero la categoría y después la subcategoría. Todas muestra el catálogo completo; cambiar los filtros no modifica las cantidades del pedido actual.

Agregar foto abre la galería del dispositivo. Puedes cambiar o quitar la foto antes de guardar. La app importa una copia JPEG de hasta 640 × 640 píxeles al almacenamiento privado; no depende de acceso permanente a la imagen original. Se aceptan imágenes de hasta 20 MB. Cancelar descarta la foto nueva; reemplazarla o eliminar el producto limpia la copia que ya no se usa. Las ventas anteriores no se modifican.

La selección e importación de fotos usa [FileKit](https://github.com/vinceglb/FileKit).
