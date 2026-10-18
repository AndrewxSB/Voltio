# Voltio

Medidor de corriente de batería (tipo Ampere) con interfaz moderna en Jetpack Compose.

## Opción A: Android Studio
1. Abre la carpeta en Android Studio (Ladybug o superior) y deja que sincronice Gradle.
2. Activa en el S24: Ajustes > Acerca del teléfono > Información de software > toca 7 veces "Número de compilación"; luego Opciones de desarrollador > Depuración USB.
3. Conéctalo por USB y pulsa Run.
4. Para sacar el APK: Build > Build APK(s) -> app/build/outputs/apk/debug/app-debug.apk

## Opción B: sin instalar nada (GitHub)
1. Crea un repositorio en GitHub y sube todo el contenido de esta carpeta (incluida .github).
2. Pestaña Actions > "Build APK" > espera ~3 min.
3. Descarga el artefacto "Voltio-apk", pásalo al móvil e instálalo (permite "instalar apps desconocidas").
