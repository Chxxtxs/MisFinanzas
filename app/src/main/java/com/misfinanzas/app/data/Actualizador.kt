package com.misfinanzas.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.misfinanzas.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Lanzamiento(val version: String, val urlApk: String, val notas: String)

/** Busca, descarga e instala versiones nuevas publicadas como releases en GitHub. */
object Actualizador {
    private const val API = "https://api.github.com/repos/${BuildConfig.REPO}/releases/latest"

    val versionActual: String get() = BuildConfig.VERSION_NAME

    /** El último release si es más nuevo que la app instalada; null si ya estás al día. Lanza excepción sin internet. */
    suspend fun buscar(): Lanzamiento? = withContext(Dispatchers.IO) {
        val conexion = (URL(API).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (conexion.responseCode == 404) return@withContext null // aún no hay releases
            if (conexion.responseCode !in 200..299) error("GitHub respondió ${conexion.responseCode}")
            val json = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            val version = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?: return@withContext null
            if (esMasNueva(version, versionActual)) {
                Lanzamiento(version, apk.getString("browser_download_url"), json.optString("body"))
            } else null
        } finally {
            conexion.disconnect()
        }
    }

    /** "1.10" > "1.9"; compara número por número. */
    fun esMasNueva(candidata: String, actual: String): Boolean {
        val a = candidata.split('.').map { it.toIntOrNull() ?: 0 }
        val b = actual.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun descargar(context: Context, url: String, progreso: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val carpeta = File(context.cacheDir, "actualizacion").apply { mkdirs() }
        val destino = File(carpeta, "MisFinanzas.apk")
        val conexion = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            if (conexion.responseCode !in 200..299) error("Descarga falló (${conexion.responseCode})")
            val total = conexion.contentLengthLong
            conexion.inputStream.use { entrada ->
                destino.outputStream().use { salida ->
                    val buffer = ByteArray(64 * 1024)
                    var leidos = 0L
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        salida.write(buffer, 0, n)
                        leidos += n
                        if (total > 0) progreso(leidos.toFloat() / total)
                    }
                }
            }
            destino
        } finally {
            conexion.disconnect()
        }
    }

    /** Android pide permiso una vez para que esta app pueda instalar APKs. */
    fun puedeInstalar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun pedirPermisoInstalar(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun instalar(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
