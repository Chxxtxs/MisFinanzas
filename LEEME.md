# Mis Finanzas

App Android nativa (Kotlin + Jetpack Compose) para anotar gastos con etiquetas y llevar las cuentas con amigos.
Tus datos se guardan solo en el celular. La app solo usa internet para buscar actualizaciones en este repositorio.

## Instalar
Descarga `MisFinanzas.apk` desde la sección **Releases** del repo, ábrelo en el celular y acepta
"Instalar apps de origen desconocido". Después se actualiza desde la app: menú ⋮ → **Buscar actualización**.

## Publicar una versión nueva
1. Sube `versionCode` (+1) y `versionName` (ej. `1.2`) en `app/build.gradle.kts`.
2. Compila:
   ```bash
   JAVA_HOME=/c/Users/miran/AndroidTools/jdk ./gradlew assembleRelease
   cp app/build/outputs/apk/release/app-release.apk MisFinanzas.apk
   ```
3. Sube el código y crea el release. El tag debe ser `v` + `versionName`:
   ```bash
   git push
   gh release create v1.2 MisFinanzas.apk --title "Mis Finanzas 1.2" --notes "Qué cambió"
   ```
El botón de la app compara su versión con el último release y descarga el `.apk` adjunto.

## Importante: la llave de firma
`misfinanzas.jks` + `keystore.properties` (no están en git) firman la app. **No los borres ni pierdas**:
Android solo instala una versión nueva encima de la actual (sin perder datos) si está firmada con la misma llave.
Guárdalos en un lugar seguro aparte (por ejemplo, un pendrive o tu Drive personal).

## Respaldo de datos
Menú ⋮ → "Guardar respaldo" exporta un `.json`; "Restaurar respaldo" lo carga de vuelta (por ejemplo, en un celular nuevo).

## Créditos
Tipografía: [Bricolage Grotesque](https://github.com/ateliertriay/bricolage), licencia SIL Open Font License 1.1.
