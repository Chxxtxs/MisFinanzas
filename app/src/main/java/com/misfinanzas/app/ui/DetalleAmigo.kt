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
import androidx.compose.material3.TopAppBarDefaults
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
import com.misfinanzas.app.ui.theme.paleta
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                val (texto, color) = textoSaldo(neto)
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(nombre, 64.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(nombre, style = MaterialTheme.typography.headlineLarge)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(texto, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (abs(neto) >= 0.005) {
                        Text(dinero(abs(neto)), style = MaterialTheme.typography.displayMedium, color = color)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { onNueva(TipoDeuda.ME_DEBE) }, modifier = Modifier.weight(1f)) {
                            Text("Me debe más")
                        }
                        OutlinedButton(onClick = { onNueva(TipoDeuda.LE_DEBO) }, modifier = Modifier.weight(1f)) {
                            Text("Le debo más")
                        }
                    }
                    if (pendientes.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        BotonSticker(
                            "Saldar todo",
                            { confirmarSaldar = true },
                            fondo = paleta.menta,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
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
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                deuda.descripcion.ifBlank { if (meDebe) "Me debe" else "Le debo" },
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (deuda.saldada) TextDecoration.LineThrough else null,
            )
            val fecha = fechaCorta(deuda.fecha.aFecha()).let { if (it == "Hoy" || it == "Ayer") it.lowercase(LocaleEs) else "el $it" }
            Text(
                "${if (meDebe) "Me debe" else "Le debo"} desde $fecha",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!deuda.saldada && deuda.pagado > 0.005) {
                Text(
                    "Ya pagó ${dinero(deuda.pagado)}, quedan ${dinero(deuda.pendiente)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(dinero(deuda.monto), style = MaterialTheme.typography.titleMedium, color = color)
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
