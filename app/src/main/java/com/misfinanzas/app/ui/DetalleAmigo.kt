@file:OptIn(ExperimentalMaterial3Api::class)

package com.misfinanzas.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Deuda
import com.misfinanzas.app.data.TipoDeuda
import com.misfinanzas.app.ui.theme.colorAFavor
import com.misfinanzas.app.ui.theme.colorEnContra
import kotlin.math.abs

@Composable
fun DetalleAmigo(
    nombre: String,
    deudas: List<Deuda>,
    onAtras: () -> Unit,
    onEditar: (Long) -> Unit,
    onNueva: (TipoDeuda) -> Unit,
    onAbonar: (Long, Double) -> Unit,
    onSaldarTodo: () -> Unit,
) {
    val pendientes = deudas.filter { !it.saldada }.sortedByDescending { it.fecha }
    val historial = deudas.filter { it.saldada }.sortedByDescending { it.fecha }
    val neto = pendientes.sumOf { if (it.tipo == TipoDeuda.ME_DEBE) it.pendiente else -it.pendiente }
    var abonando by remember { mutableStateOf<Deuda?>(null) }
    var confirmarSaldar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombre) },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                val (texto, color) = textoSaldo(neto)
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Insignia(nombre.take(1).uppercase(), MaterialTheme.colorScheme.primary, 56.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (abs(neto) >= 0.005) {
                            Text(dinero(abs(neto)), style = MaterialTheme.typography.displaySmall, color = color)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onNueva(TipoDeuda.ME_DEBE) }, modifier = Modifier.weight(1f)) {
                                Text("Me debe más")
                            }
                            FilledTonalButton(onClick = { onNueva(TipoDeuda.LE_DEBO) }, modifier = Modifier.weight(1f)) {
                                Text("Le debo más")
                            }
                        }
                        if (pendientes.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { confirmarSaldar = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Saldar todo")
                            }
                        }
                    }
                }
            }

            if (pendientes.isNotEmpty()) {
                item { Titulo("Pendientes") }
                items(pendientes, key = { it.id }) { d ->
                    FilaDeuda(d, onClick = { onEditar(d.id) }, onAbonar = { abonando = d })
                }
            }
            if (historial.isNotEmpty()) {
                item { Titulo("Pagadas") }
                items(historial, key = { it.id }) { d -> FilaDeuda(d, onClick = { onEditar(d.id) }, onAbonar = null) }
            }
            if (deudas.isEmpty()) {
                item { EstadoVacio("🤝", "Sin movimientos", "No hay deudas con $nombre.") }
            }
        }
    }

    abonando?.let { d ->
        DialogoAbono(
            deuda = d,
            onConfirmar = { monto -> onAbonar(d.id, monto); abonando = null },
            onCancelar = { abonando = null },
        )
    }

    if (confirmarSaldar) {
        AlertDialog(
            onDismissRequest = { confirmarSaldar = false },
            title = { Text("¿Saldar todo con $nombre?") },
            text = { Text("Todas las deudas pendientes quedarán como pagadas.") },
            confirmButton = { TextButton(onClick = { onSaldarTodo(); confirmarSaldar = false }) { Text("Saldar") } },
            dismissButton = { TextButton(onClick = { confirmarSaldar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilaDeuda(deuda: Deuda, onClick: () -> Unit, onAbonar: (() -> Unit)?) {
    val meDebe = deuda.tipo == TipoDeuda.ME_DEBE
    val color = if (deuda.saldada) MaterialTheme.colorScheme.onSurfaceVariant else if (meDebe) colorAFavor else colorEnContra
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                deuda.descripcion.ifBlank { if (meDebe) "Me debe" else "Le debo" },
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (deuda.saldada) TextDecoration.LineThrough else null,
            )
            val detalle = buildString {
                append(if (meDebe) "Me debe" else "Le debo")
                append("  ·  ")
                append(fechaCorta(deuda.fecha.aFecha()))
                if (!deuda.saldada && deuda.pagado > 0.005) append("  ·  quedan ${dinero(deuda.pendiente)}")
            }
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(dinero(deuda.monto), fontWeight = FontWeight.SemiBold, color = color)
            if (onAbonar != null) {
                OutlinedButton(
                    onClick = onAbonar,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) { Text("Pagar", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun DialogoAbono(deuda: Deuda, onConfirmar: (Double) -> Unit, onCancelar: () -> Unit) {
    var montoTxt by remember { mutableStateOf(montoParaEditar(deuda.pendiente)) }
    val monto = parsearMonto(montoTxt)
    val error = when {
        monto == null -> "Escribe un monto"
        monto > deuda.pendiente + 0.005 -> "Máximo ${dinero(deuda.pendiente)}"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (deuda.tipo == TipoDeuda.ME_DEBE) "¿Cuánto te pagó?" else "¿Cuánto pagaste?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Pendiente: ${dinero(deuda.pendiente)}. Puedes registrar un pago parcial.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CampoMonto(montoTxt, { montoTxt = it }, "Monto pagado", error, grande = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (error == null) onConfirmar(monto!!) }, enabled = error == null) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
