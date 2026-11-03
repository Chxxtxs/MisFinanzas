package com.misfinanzas.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Guarda todo en un archivo JSON privado de la app, dentro del celular. */
class Almacen(private val carpeta: File) {
    private val archivo = File(carpeta, "datos.json")

    fun cargar(): Datos {
        if (!archivo.exists()) return Datos()
        return try {
            desdeJson(archivo.readText())
        } catch (e: Exception) {
            // Nunca perder datos: si el archivo está dañado se aparta una copia antes de empezar de cero.
            archivo.copyTo(File(carpeta, "datos_danado_${System.currentTimeMillis()}.json"), overwrite = true)
            Datos()
        }
    }

    fun guardar(datos: Datos) {
        val temporal = File(carpeta, "datos.json.tmp")
        temporal.writeText(aJson(datos))
        if (!temporal.renameTo(archivo)) {
            archivo.delete()
            temporal.renameTo(archivo)
        }
    }

    companion object {
        fun aJson(datos: Datos): String {
            val etiquetas = JSONArray()
            datos.etiquetas.forEach { e ->
                etiquetas.put(
                    JSONObject()
                        .put("id", e.id)
                        .put("nombre", e.nombre)
                        .put("emoji", e.emoji)
                        .put("color", e.color)
                )
            }
            val gastos = JSONArray()
            datos.gastos.forEach { g ->
                gastos.put(
                    JSONObject()
                        .put("id", g.id)
                        .put("monto", g.monto)
                        .put("descripcion", g.descripcion)
                        .put("etiquetas", JSONArray(g.etiquetas))
                        .put("fecha", g.fecha)
                )
            }
            val deudas = JSONArray()
            datos.deudas.forEach { d ->
                deudas.put(
                    JSONObject()
                        .put("id", d.id)
                        .put("amigo", d.amigo)
                        .put("monto", d.monto)
                        .put("pagado", d.pagado)
                        .put("descripcion", d.descripcion)
                        .put("tipo", d.tipo.clave)
                        .put("fecha", d.fecha)
                )
            }
            return JSONObject()
                .put("version", 2)
                .put("etiquetas", etiquetas)
                .put("gastos", gastos)
                .put("deudas", deudas)
                .toString()
        }

        fun desdeJson(texto: String): Datos {
            val raiz = JSONObject(texto)
            val gastos = raiz.optJSONArray("gastos") ?: JSONArray()
            val deudas = raiz.optJSONArray("deudas") ?: JSONArray()
            // La versión 1 no guardaba etiquetas: se usan las de siempre.
            val etiquetas = raiz.optJSONArray("etiquetas")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Etiqueta(
                        id = o.getString("id"),
                        nombre = o.getString("nombre"),
                        emoji = o.optString("emoji"),
                        color = o.getLong("color"),
                    )
                }
            } ?: etiquetasPorDefecto
            return Datos(
                etiquetas = etiquetas,
                gastos = (0 until gastos.length()).map { i ->
                    val o = gastos.getJSONObject(i)
                    val ids = o.optJSONArray("etiquetas")
                        ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                        ?: listOf(o.optString("categoria", "otro")) // versión 1: una sola categoría
                    Gasto(
                        id = o.getLong("id"),
                        monto = o.getDouble("monto"),
                        descripcion = o.optString("descripcion"),
                        etiquetas = ids,
                        fecha = o.getLong("fecha"),
                    )
                },
                deudas = (0 until deudas.length()).map { i ->
                    val o = deudas.getJSONObject(i)
                    Deuda(
                        id = o.getLong("id"),
                        amigo = o.getString("amigo"),
                        monto = o.getDouble("monto"),
                        pagado = o.optDouble("pagado", 0.0),
                        descripcion = o.optString("descripcion"),
                        tipo = TipoDeuda.desdeClave(o.optString("tipo")),
                        fecha = o.getLong("fecha"),
                    )
                },
            )
        }
    }
}
