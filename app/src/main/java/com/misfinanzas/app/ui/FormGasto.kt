@file:OptIn(ExperimentalLayoutApi::class)

package com.misfinanzas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Etiqueta
import com.misfinanzas.app.data.Gasto
import java.io.Serializable
import java.time.LocalDate
import kotlin.math.floor

/** Un amigo dentro de un gasto dividido: nombre y lo que te debe (como texto del campo). */
private data class ParteAmigo(val nombre: String = "", val monto: String = "") : Serializable

@Composable
fun FormGasto(
    gasto: Gasto?,
    etiquetas: List<Etiqueta>,
    amigos: List<String>,
    onGuardar: (monto: Double, descripcion: String, etiquetas: List<String>, fecha: Long, partes: List<Pair<String, Double>>) -> Unit,
    onCrearEtiqueta: (nombre: String, emoji: String, color: Long) -> String,
    onEliminar: (() -> Unit)?,
    onCerrar: () -> Unit,
) {
    var montoTxt by rememberSaveable { mutableStateOf(gasto?.let { montoParaEditar(it.monto) } ?: "") }
    var descripcion by rememberSaveable { mutableStateOf(gasto?.descripcion ?: "") }
    var elegidas by rememberSaveable { mutableStateOf(gasto?.etiquetas?.filter { id -> etiquetas.any { it.id == id } } ?: emptyList()) }
    var dia by rememberSaveable { mutableStateOf((gasto?.fecha?.aFecha() ?: LocalDate.now()).toEpochDay()) }
    var dividir by rememberSaveable { mutableStateOf(false) }
    var partes by rememberSaveable { mutableStateOf(listOf(ParteAmigo())) }
    var intentado by rememberSaveable { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val monto = parsearMonto(montoTxt)
    val montosPartes = partes.map { parsearMonto(it.monto) }
    val sumaPartes = montosPartes.sumOf { it ?: 0.0 }
    val partesValidas = partes.all { it.nombre.isNotBlank() } && montosPartes.all { it != null } &&
        (monto == null || sumaPartes <= monto + 0.005)

    val errorMonto = if (intentado && monto == null) "Escribe cuánto gastaste" else null
    val errorEtiquetas = intentado && elegidas.isEmpty()

    fun actualizarParte(i: Int, cambio: (ParteAmigo) -> ParteAmigo) {
        partes = partes.mapIndexed { j, p -> if (j == i) cambio(p) else p }
    }

    PantallaFormulario(
        titulo = if (gasto == null) "Nuevo gasto" else "Editar gasto",
        onCerrar = onCerrar,
        onGuardar = {
            intentado = true
            if (monto != null && elegidas.isNotEmpty() && (!dividir || partesValidas)) {
                onGuardar(
                    monto,
                    descripcion.trim(),
                    elegidas,
                    fechaAMillis(LocalDate.ofEpochDay(dia), gasto?.fecha),
                    if (dividir) partes.mapIndexed { i, p -> p.nombre.trim() to montosPartes[i]!! } else emptyList(),
                )
            }
        },
        accionExtra = {
            if (onEliminar != null) {
                IconButton(onClick = { confirmarBorrado = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                }
            }
        },
    ) {
        CampoMonto(montoTxt, { montoTxt = it }, "Monto", errorMonto)

        SelectorEtiquetas(
            etiquetas = etiquetas,
            elegidas = elegidas,
            onCambio = { elegidas = it },
            onCrear = onCrearEtiqueta,
            error = errorEtiquetas,
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("¿En qué? (opcional)") },
            placeholder = { Text("Ej: café, bencina, cine…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        SelectorFecha(LocalDate.ofEpochDay(dia)) { dia = it.toEpochDay() }

        // Solo al crear: pagaste tú y uno o más amigos te deben una parte.
        if (gasto == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pagué por otras personas", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Anota cuánto te debe cada uno y queda en Amigos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = dividir, onCheckedChange = { dividir = it })
                    }

                    if (dividir) {
                        partes.forEachIndexed { i, parte ->
                            if (i > 0) HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Amigo ${i + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (partes.size > 1) {
                                    IconButton(onClick = { partes = partes.filterIndexed { j, _ -> j != i } }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Quitar amigo ${i + 1}")
                                    }
                                }
                            }
                            val otros = partes.filterIndexed { j, _ -> j != i }.map { claveAmigoUi(it.nombre) }
                            CampoAmigo(
                                valor = parte.nombre,
                                onCambio = { nuevo -> actualizarParte(i) { it.copy(nombre = nuevo) } },
                                amigos = amigos.filter { claveAmigoUi(it) !in otros },
                                error = if (intentado && parte.nombre.isBlank()) "Escribe el nombre" else null,
                            )
                            CampoMonto(
                                valor = parte.monto,
                                onCambio = { nuevo -> actualizarParte(i) { it.copy(monto = nuevo) } },
                                etiqueta = "¿Cuánto te debe?",
                                error = if (intentado && montosPartes[i] == null) "Escribe cuánto te debe" else null,
                                grande = false,
                            )
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { partes = partes + ParteAmigo() }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("Agregar amigo")
                            }
                            if (monto != null) {
                                TextButton(onClick = {
                                    // Total dividido entre los amigos y tú, redondeado a pesos.
                                    val cada = floor(monto / (partes.size + 1))
                                    partes = partes.map { it.copy(monto = montoParaEditar(cada)) }
                                }) { Text("Partes iguales") }
                            }
                        }

                        if (monto != null) {
                            val texto = if (sumaPartes > monto + 0.005) {
                                "Las partes suman ${dinero(sumaPartes)}, más que el total."
                            } else {
                                "Tu gasto quedará en ${dinero(monto - sumaPartes)}; tus amigos te deberán ${dinero(sumaPartes)} en total."
                            }
                            Text(
                                texto,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sumaPartes > monto + 0.005) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmarBorrado && onEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar este gasto?") },
            text = { Text("No se puede deshacer.") },
            confirmButton = { TextButton(onClick = onEliminar) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

private fun claveAmigoUi(nombre: String) = nombre.trim().lowercase()

/** Campo de nombre con sugerencias de amigos ya anotados. */
@Composable
fun CampoAmigo(valor: String, onCambio: (String) -> Unit, amigos: List<String>, error: String?) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text("Nombre") },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    val sugerencias = amigos
        .filter { it.contains(valor.trim(), ignoreCase = true) && !it.equals(valor.trim(), ignoreCase = true) }
        .take(8)
    if (sugerencias.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sugerencias.forEach { nombre ->
                FilterChip(selected = false, onClick = { onCambio(nombre) }, label = { Text(nombre) })
            }
        }
    }
}
