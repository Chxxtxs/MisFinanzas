package com.misfinanzas.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.ResumenAmigo
import com.misfinanzas.app.data.resumirAmigos
import com.misfinanzas.app.ui.theme.colorAFavor
import com.misfinanzas.app.ui.theme.colorEnContra
import com.misfinanzas.app.ui.theme.paleta
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
        contentPadding = PaddingValues(bottom = 112.dp),
    ) {
        item {
            Text(
                "Amigos",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BloqueSaldo("Te deben", teDeben, colorAFavor, paleta.menta, Modifier.weight(1f))
                BloqueSaldo("Debes", debes, colorEnContra, paleta.tomate, Modifier.weight(1f))
            }
        }

        if (amigos.isEmpty()) {
            item {
                EstadoVacio(
                    "🤝",
                    "Cuentas claras",
                    "Nadie te debe y no le debes a nadie. Toca Anotar deuda cuando alguien quede debiendo.",
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
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilaAmigo(amigo: ResumenAmigo, onClick: () -> Unit) {
    val (texto, color) = textoSaldo(amigo.neto)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(amigo.nombre)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(amigo.nombre, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (abs(amigo.neto) >= 0.005) {
            Text(dinero(abs(amigo.neto)), style = MaterialTheme.typography.titleMedium, color = color)
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
