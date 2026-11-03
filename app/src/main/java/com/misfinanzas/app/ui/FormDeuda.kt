@file:OptIn(ExperimentalMaterial3Api::class)

package com.misfinanzas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.misfinanzas.app.data.Deuda
import com.misfinanzas.app.data.TipoDeuda
import java.time.LocalDate

@Composable
fun FormDeuda(
    deuda: Deuda?,
    amigoInicial: String,
    tipoInicial: TipoDeuda,
    amigos: List<String>,
    onGuardar: (amigos: List<String>, monto: Double, descripcion: String, tipo: TipoDeuda, fecha: Long) -> Unit,
    onEliminar: (() -> Unit)?,
    onCerrar: () -> Unit,
) {
    var tipo by rememberSaveable { mutableStateOf((deuda?.tipo ?: tipoInicial).clave) }
    var nombres by rememberSaveable { mutableStateOf(listOf(deuda?.amigo ?: amigoInicial)) }
    var montoTxt by rememberSaveable { mutableStateOf(deuda?.let { montoParaEditar(it.monto) } ?: "") }
    var descripcion by rememberSaveable { mutableStateOf(deuda?.descripcion ?: "") }
    var dia by rememberSaveable { mutableStateOf((deuda?.fecha?.aFecha() ?: LocalDate.now()).toEpochDay()) }
    var intentado by rememberSaveable { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val monto = parsearMonto(montoTxt)
    val tipoDeuda = TipoDeuda.desdeClave(tipo)
    val varios = nombres.size > 1

    PantallaFormulario(
        titulo = if (deuda == null) "Nueva deuda" else "Editar deuda",
        onCerrar = onCerrar,
        onGuardar = {
            intentado = true
            if (monto != null && nombres.all { it.isNotBlank() }) {
                onGuardar(nombres.map { it.trim() }, monto, descripcion.trim(), tipoDeuda, fechaAMillis(LocalDate.ofEpochDay(dia), deuda?.fecha))
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
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(TipoDeuda.ME_DEBE to (if (varios) "Me deben" else "Me debe"), TipoDeuda.LE_DEBO to (if (varios) "Les debo" else "Le debo"))
                .forEachIndexed { i, (t, texto) ->
                    SegmentedButton(
                        selected = tipoDeuda == t,
                        onClick = { tipo = t.clave },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                    ) { Text(texto) }
                }
        }

        nombres.forEachIndexed { i, nombre ->
            Column {
                if (varios) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Amigo ${i + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { nombres = nombres.filterIndexed { j, _ -> j != i } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar amigo ${i + 1}")
                        }
                    }
                }
                val otros = nombres.filterIndexed { j, _ -> j != i }.map { it.trim().lowercase() }
                CampoAmigo(
                    valor = nombre,
                    onCambio = { nuevo -> nombres = nombres.mapIndexed { j, n -> if (j == i) nuevo else n } },
                    amigos = amigos.filter { it.trim().lowercase() !in otros },
                    error = if (intentado && nombre.isBlank()) "Escribe el nombre" else null,
                )
            }
        }

        // Varias personas solo al crear: se anota una deuda por cada una.
        if (deuda == null) {
            OutlinedButton(onClick = { nombres = nombres + "" }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Agregar otro amigo")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CampoMonto(
                montoTxt,
                { montoTxt = it },
                if (varios) "Monto de cada uno" else "Monto",
                if (intentado && monto == null) "Escribe el monto" else null,
            )
            if (varios && monto != null) {
                Text(
                    "${nombres.size} deudas de ${dinero(monto)} (total ${dinero(monto * nombres.size)}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("¿Por qué? (opcional)") },
            placeholder = { Text("Ej: pizza del viernes, entrada al concierto…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        SelectorFecha(LocalDate.ofEpochDay(dia)) { dia = it.toEpochDay() }

        if (deuda != null && deuda.pagado > 0.005) {
            Text(
                "Ya se han pagado ${dinero(deuda.pagado)} de esta deuda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmarBorrado && onEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar esta deuda?") },
            text = { Text("No se puede deshacer. Si ya se pagó, mejor márcala como pagada.") },
            confirmButton = { TextButton(onClick = onEliminar) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}
