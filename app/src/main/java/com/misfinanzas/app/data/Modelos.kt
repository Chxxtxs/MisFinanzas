package com.misfinanzas.app.data

/** Etiqueta de gasto. Un gasto puede tener varias; el usuario puede crear las suyas. */
data class Etiqueta(
    val id: String,
    val nombre: String,
    val emoji: String,
    val color: Long,
) {
    /** El emoji, o la inicial del nombre si no tiene. */
    val icono: String get() = emoji.ifBlank { nombre.take(1).uppercase() }
}

/** Las ids coinciden con las antiguas categorías, así los datos viejos se migran solos. */
val etiquetasPorDefecto = listOf(
    Etiqueta("comida", "Comida", "🍔", 0xFFE8833A),
    Etiqueta("hormiga", "Hormiga", "🐜", 0xFFA0724F),
    Etiqueta("salida", "Salidas", "🎉", 0xFF9B5DE5),
    Etiqueta("transporte", "Transporte", "🚌", 0xFF3A86FF),
    Etiqueta("cuentas", "Cuentas", "🧾", 0xFF13A89E),
    Etiqueta("compras", "Compras", "🛍️", 0xFFE5487A),
    Etiqueta("otro", "Otro", "📦", 0xFF7D8A87),
)

const val ID_HORMIGA = "hormiga"

/** Para gastos que quedaron sin etiquetas (por ejemplo, si se borró la única que tenían). */
val etiquetaVacia = Etiqueta("", "Sin etiqueta", "🏷️", 0xFF7D8A87)

/** Colores para elegir al crear una etiqueta. */
val coloresEtiqueta = listOf(
    0xFFE8833A, 0xFFA0724F, 0xFF9B5DE5, 0xFF3A86FF, 0xFF13A89E, 0xFFE5487A,
    0xFF2E9E4F, 0xFFD64545, 0xFFE0A800, 0xFF4F5BD5, 0xFF0096C7, 0xFF7D8A87,
)

data class Gasto(
    val id: Long,
    val monto: Double,
    val descripcion: String,
    val etiquetas: List<String>,
    val fecha: Long,
)

enum class TipoDeuda(val clave: String) {
    /** Un amigo me debe a mí. */
    ME_DEBE("me_debe"),

    /** Yo le debo a un amigo. */
    LE_DEBO("le_debo");

    companion object {
        fun desdeClave(clave: String?): TipoDeuda = entries.firstOrNull { it.clave == clave } ?: ME_DEBE
    }
}

data class Deuda(
    val id: Long,
    val amigo: String,
    val monto: Double,
    val pagado: Double,
    val descripcion: String,
    val tipo: TipoDeuda,
    val fecha: Long,
) {
    val pendiente: Double get() = (monto - pagado).coerceAtLeast(0.0)
    val saldada: Boolean get() = pendiente < 0.005
}

data class Datos(
    val gastos: List<Gasto> = emptyList(),
    val deudas: List<Deuda> = emptyList(),
    val etiquetas: List<Etiqueta> = etiquetasPorDefecto,
) {
    /** Etiquetas del gasto en el orden en que se eligieron; nunca vacía. */
    fun etiquetasDe(gasto: Gasto): List<Etiqueta> =
        gasto.etiquetas.mapNotNull { id -> etiquetas.find { it.id == id } }.ifEmpty { listOf(etiquetaVacia) }
}

data class ResumenAmigo(
    val nombre: String,
    val meDebe: Double,
    val leDebo: Double,
) {
    /** Positivo: el amigo me debe. Negativo: yo le debo. */
    val neto: Double get() = meDebe - leDebo
}

/** Clave para agrupar a un amigo sin importar mayúsculas o espacios. */
fun claveAmigo(nombre: String): String = nombre.trim().lowercase()

fun resumirAmigos(deudas: List<Deuda>): List<ResumenAmigo> =
    deudas.groupBy { claveAmigo(it.amigo) }.map { (_, lista) ->
        ResumenAmigo(
            nombre = lista.maxBy { it.fecha }.amigo.trim(),
            meDebe = lista.filter { it.tipo == TipoDeuda.ME_DEBE }.sumOf { it.pendiente },
            leDebo = lista.filter { it.tipo == TipoDeuda.LE_DEBO }.sumOf { it.pendiente },
        )
    }
