package com.misfinanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.ResumenAmigo
import com.misfinanzas.app.data.resumirAmigos
import com.misfinanzas.app.ui.theme.colorAFavor
import com.misfinanzas.app.ui.theme.colorEnContra
import kotlin.math.abs

@Composable
fun PestanaAmigos(
    datos: Datos,
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val amigos = remember(datos) { resumirAmigos(datos.deudas) }
    val pendientes = amigos.filter { abs(it.neto) >= 0.005 }.sortedByDescending { abs(it.neto) }
    val alDia = amigos.filter { abs(it.neto) < 0.005 }.sortedBy { it.nombre.lowercase() }
    val teDeben = pendientes.sumOf { it.neto.coerceAtLeast(0.0) }
    val debes = pendientes.sumOf { (-it.neto).coerceAtLeast(0.0) }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TarjetaSaldo("Te deben", teDeben, colorAFavor, Modifier.weight(1f))
                TarjetaSaldo("Debes", debes, colorEnContra, Modifier.weight(1f))
            }
        }

        if (amigos.isEmpty()) {
            item {
                EstadoVacio(
                    "🤝",
                    "Cuentas claras",
                    "Toca \"Nueva deuda\" para anotar quién te debe o a quién le debes.",
                )
            }
        }

        if (pendientes.isNotEmpty()) {
            item { Encabezado("Pendientes") }
            items(pendientes, key = { "p-" + it.nombre.lowercase() }) { FilaAmigo(it) { onAbrir(it.nombre) } }
        }
        if (alDia.isNotEmpty()) {
            item { Encabezado("A mano") }
            items(alDia, key = { "a-" + it.nombre.lowercase() }) { FilaAmigo(it) { onAbrir(it.nombre) } }
        }
    }
}

@Composable
private fun TarjetaSaldo(titulo: String, monto: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(16.dp)) {
            Rotulo(titulo)
            Text(dinero(monto), style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilaAmigo(amigo: ResumenAmigo, onClick: () -> Unit) {
    val (texto, color) = textoSaldo(amigo.neto)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Insignia(amigo.nombre.take(1).uppercase(), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(amigo.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (abs(amigo.neto) >= 0.005) {
            Text(dinero(abs(amigo.neto)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

/** Frase y color según el saldo neto con un amigo. */
@Composable
fun textoSaldo(neto: Double): Pair<String, Color> = when {
    neto >= 0.005 -> "Te debe" to colorAFavor
    neto <= -0.005 -> "Le debes" to colorEnContra
    else -> "Están a mano" to MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun Separador() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
