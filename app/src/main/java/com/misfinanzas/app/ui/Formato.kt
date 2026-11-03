package com.misfinanzas.app.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

val LocaleEs: Locale = Locale.forLanguageTag("es-CL")

private fun esEntero(v: Double) = abs(v - v.roundToLong()) < 0.005

/** 15000.0 -> "$15.000"; 1234.5 -> "$1.234,50" */
fun dinero(v: Double): String {
    val nf = NumberFormat.getNumberInstance(LocaleEs)
    val decimales = if (esEntero(v)) 0 else 2
    nf.minimumFractionDigits = decimales
    nf.maximumFractionDigits = decimales
    return (if (v < -0.004) "-$" else "$") + nf.format(abs(v))
}

/**
 * Acepta "5000", "5.000", "$5.000", "12,5" o "1.234,50".
 * Un punto seguido de exactamente 3 dígitos se toma como separador de miles.
 */
fun parsearMonto(texto: String): Double? {
    var t = texto.trim().replace("$", "").replace(" ", "")
    if (t.isEmpty()) return null
    t = when {
        '.' in t && ',' in t -> t.replace(".", "").replace(',', '.')
        ',' in t -> t.replace(',', '.')
        '.' in t -> {
            val partes = t.split('.')
            if (partes.size > 2 || partes.last().length == 3) t.replace(".", "") else t
        }
        else -> t
    }
    return t.toDoubleOrNull()?.takeIf { it > 0 }
}

fun montoParaEditar(v: Double): String =
    if (esEntero(v)) v.roundToLong().toString() else String.format(Locale.US, "%.2f", v).replace('.', ',')

private fun capitalizar(s: String) = s.replaceFirstChar { it.titlecase(LocaleEs) }

fun nombreMes(m: YearMonth, conAno: Boolean = true): String {
    val nombre = capitalizar(m.month.getDisplayName(TextStyle.FULL, LocaleEs))
    return if (conAno) "$nombre ${m.year}" else nombre
}

// Sin el mes: la lista ya está agrupada por mes.
private val formatoDia = DateTimeFormatter.ofPattern("EEEE d", LocaleEs)
private val formatoCorto = DateTimeFormatter.ofPattern("d MMM yyyy", LocaleEs)
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", LocaleEs)

fun etiquetaDia(fecha: LocalDate): String {
    val hoy = LocalDate.now()
    return when (fecha) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> capitalizar(fecha.format(formatoDia))
    }
}

fun fechaCorta(fecha: LocalDate): String = when (fecha) {
    LocalDate.now() -> "Hoy"
    LocalDate.now().minusDays(1) -> "Ayer"
    else -> fecha.format(formatoCorto)
}

fun Long.aFecha(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun Long.aHora(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatoHora)

fun Long.aMes(): YearMonth = YearMonth.from(aFecha())

/**
 * Convierte la fecha elegida en el formulario a milisegundos.
 * Conserva la hora original si la fecha no cambió; si es hoy usa la hora actual.
 */
fun fechaAMillis(fecha: LocalDate, original: Long?): Long = when {
    original != null && original.aFecha() == fecha -> original
    fecha == LocalDate.now() -> System.currentTimeMillis()
    else -> fecha.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
