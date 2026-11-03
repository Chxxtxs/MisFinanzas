package com.misfinanzas.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.data.Actualizador
import com.misfinanzas.app.data.Almacen
import com.misfinanzas.app.data.Lanzamiento
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.Deuda
import com.misfinanzas.app.data.Etiqueta
import com.misfinanzas.app.data.Gasto
import com.misfinanzas.app.data.TipoDeuda
import com.misfinanzas.app.data.claveAmigo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import kotlin.random.Random

sealed interface Pantalla {
    data object Inicio : Pantalla
    data class EditarGasto(val id: Long?) : Pantalla
    data class EditarDeuda(
        val id: Long?,
        val amigo: String = "",
        val tipo: TipoDeuda = TipoDeuda.ME_DEBE,
    ) : Pantalla
    data class Amigo(val nombre: String) : Pantalla
    data object Etiquetas : Pantalla
}

sealed interface EstadoActualizacion {
    data object Inactivo : EstadoActualizacion
    data object Buscando : EstadoActualizacion
    data object AlDia : EstadoActualizacion
    data class Disponible(val lanzamiento: Lanzamiento) : EstadoActualizacion
    data class Descargando(val progreso: Float) : EstadoActualizacion
    data class Lista(val apk: File) : EstadoActualizacion
    data class Fallo(val mensaje: String) : EstadoActualizacion
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val almacen = Almacen(app.filesDir)
    private val escritura = Mutex()

    private val _datos = MutableStateFlow(Datos())
    val datos: StateFlow<Datos> = _datos.asStateFlow()

    private val _cargado = MutableStateFlow(false)
    val cargado: StateFlow<Boolean> = _cargado.asStateFlow()

    private val _mes = MutableStateFlow(YearMonth.now())
    val mes: StateFlow<YearMonth> = _mes.asStateFlow()

    // Navegación simple: una pila de pantallas que sobrevive a rotaciones.
    val pila = mutableStateListOf<Pantalla>(Pantalla.Inicio)
    var pestana by mutableIntStateOf(0)
    var actualizacion by mutableStateOf<EstadoActualizacion>(EstadoActualizacion.Inactivo)

    fun buscarActualizacion() {
        actualizacion = EstadoActualizacion.Buscando
        viewModelScope.launch {
            actualizacion = try {
                Actualizador.buscar()?.let { EstadoActualizacion.Disponible(it) } ?: EstadoActualizacion.AlDia
            } catch (e: Exception) {
                EstadoActualizacion.Fallo("No se pudo revisar. ¿Tienes internet?")
            }
        }
    }

    fun descargarActualizacion(lanzamiento: Lanzamiento) {
        actualizacion = EstadoActualizacion.Descargando(0f)
        viewModelScope.launch {
            actualizacion = try {
                var ultimo = 0
                val apk = Actualizador.descargar(getApplication(), lanzamiento.urlApk) { p ->
                    val porcentaje = (p * 100).toInt()
                    if (porcentaje != ultimo) {
                        ultimo = porcentaje
                        actualizacion = EstadoActualizacion.Descargando(p)
                    }
                }
                EstadoActualizacion.Lista(apk)
            } catch (e: Exception) {
                EstadoActualizacion.Fallo("La descarga falló. Inténtalo de nuevo.")
            }
        }
    }

    init {
        viewModelScope.launch {
            _datos.value = withContext(Dispatchers.IO) { almacen.cargar() }
            _cargado.value = true
        }
    }

    fun ir(pantalla: Pantalla) {
        pila.add(pantalla)
    }

    fun atras() {
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
    }

    fun mesAnterior() = _mes.update { it.minusMonths(1) }
    fun mesSiguiente() = _mes.update { it.plusMonths(1) }

    private fun nuevoId(): Long = System.currentTimeMillis() * 1000 + Random.nextInt(1000)

    private fun actualizar(cambio: (Datos) -> Datos) {
        _datos.update(cambio)
        viewModelScope.launch(Dispatchers.IO) {
            // Siempre se escribe el estado más reciente, así el orden de los guardados no importa.
            escritura.withLock { almacen.guardar(_datos.value) }
        }
    }

    /**
     * Guarda un gasto. Cada parte de [partesAmigos] queda como deuda de ese amigo
     * y el gasto registrado es solo lo que te corresponde a ti.
     */
    fun guardarGasto(
        id: Long?,
        monto: Double,
        descripcion: String,
        etiquetas: List<String>,
        fecha: Long,
        partesAmigos: List<Pair<String, Double>> = emptyList(),
    ) = actualizar { d ->
        var gastos = d.gastos
        var deudas = d.deudas
        val miParte = monto - partesAmigos.sumOf { it.second }
        if (id != null) {
            gastos = gastos.map {
                if (it.id == id) it.copy(monto = monto, descripcion = descripcion, etiquetas = etiquetas, fecha = fecha) else it
            }
        } else if (miParte > 0.005) {
            gastos = gastos + Gasto(nuevoId(), miParte, descripcion, etiquetas, fecha)
        }
        if (id == null) {
            val detalle = descripcion.ifBlank {
                etiquetas.mapNotNull { e -> d.etiquetas.find { it.id == e }?.nombre }.joinToString(", ")
            }
            deudas = deudas + partesAmigos.map { (amigo, parte) ->
                Deuda(nuevoId(), amigo.trim(), parte, 0.0, detalle, TipoDeuda.ME_DEBE, fecha)
            }
        }
        d.copy(gastos = gastos, deudas = deudas)
    }

    /** Crea (id nula) o edita una etiqueta. Devuelve su id. */
    fun guardarEtiqueta(id: String?, nombre: String, emoji: String, color: Long): String {
        val idFinal = id ?: "e${nuevoId()}"
        actualizar { d ->
            val etiqueta = Etiqueta(idFinal, nombre.trim(), emoji.trim(), color)
            d.copy(
                etiquetas = if (id == null) d.etiquetas + etiqueta
                else d.etiquetas.map { if (it.id == id) etiqueta else it }
            )
        }
        return idFinal
    }

    /** Borra la etiqueta y la quita de los gastos que la tenían (los gastos no se borran). */
    fun eliminarEtiqueta(id: String) = actualizar { d ->
        d.copy(
            etiquetas = d.etiquetas.filterNot { it.id == id },
            gastos = d.gastos.map { g -> if (id in g.etiquetas) g.copy(etiquetas = g.etiquetas - id) else g },
        )
    }

    fun eliminarGasto(id: Long) = actualizar { d -> d.copy(gastos = d.gastos.filterNot { it.id == id }) }

    /** Al crear, se anota una deuda por cada amigo de [amigos], todas por el mismo [monto]. */
    fun guardarDeuda(
        id: Long?,
        amigos: List<String>,
        monto: Double,
        descripcion: String,
        tipo: TipoDeuda,
        fecha: Long,
    ) = actualizar { d ->
        val deudas = if (id != null) {
            d.deudas.map {
                if (it.id == id) {
                    it.copy(
                        amigo = amigos.first().trim(),
                        monto = monto,
                        pagado = it.pagado.coerceAtMost(monto),
                        descripcion = descripcion,
                        tipo = tipo,
                        fecha = fecha,
                    )
                } else it
            }
        } else {
            d.deudas + amigos.map { Deuda(nuevoId(), it.trim(), monto, 0.0, descripcion, tipo, fecha) }
        }
        d.copy(deudas = deudas)
    }

    fun abonar(id: Long, monto: Double) = actualizar { d ->
        d.copy(deudas = d.deudas.map {
            if (it.id == id) it.copy(pagado = (it.pagado + monto).coerceAtMost(it.monto)) else it
        })
    }

    fun saldarAmigo(nombre: String) = actualizar { d ->
        val clave = claveAmigo(nombre)
        d.copy(deudas = d.deudas.map { if (claveAmigo(it.amigo) == clave) it.copy(pagado = it.monto) else it })
    }

    fun eliminarDeuda(id: Long) = actualizar { d -> d.copy(deudas = d.deudas.filterNot { it.id == id }) }

    fun exportar(uri: Uri, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "w")!!.use {
                        it.write(Almacen.aJson(_datos.value).toByteArray())
                    }
                }.isSuccess
            }
            alTerminar(ok)
        }
    }

    fun importar(uri: Uri, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch {
            val leidos = withContext(Dispatchers.IO) {
                runCatching {
                    val texto = getApplication<Application>().contentResolver.openInputStream(uri)!!
                        .bufferedReader().use { it.readText() }
                    Almacen.desdeJson(texto)
                }.getOrNull()
            }
            if (leidos != null) actualizar { leidos }
            alTerminar(leidos != null)
        }
    }
}
