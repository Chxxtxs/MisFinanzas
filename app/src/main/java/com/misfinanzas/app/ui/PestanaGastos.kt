package com.misfinanzas.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.Etiqueta
import com.misfinanzas.app.data.Gasto
import java.time.YearMonth

@Composable
fun PestanaGastos(
    datos: Datos,
    mes: YearMonth,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onAbrir: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtro by rememberSaveable { mutableStateOf<String?>(null) }
    val categoria = filtro?.let { id -> datos.etiquetas.find { it.id == id } }

    val gastos = remember(datos, mes, categoria) {
        datos.gastos
            .filter { it.fecha.aMes() == mes && (categoria == null || categoria.id in it.etiquetas) }
            .sortedByDescending { it.fecha }
    }
    val porDia = remember(gastos) { gastos.groupBy { it.fecha.aFecha() } }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { SelectorMes(mes, onMesAnterior, onMesSiguiente, Modifier.padding(horizontal = 16.dp)) }

        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = categoria == null, onClick = { filtro = null }, label = { Text("Todas") })
                datos.etiquetas.forEach { c ->
                    FilterChip(
                        selected = categoria == c,
                        onClick = { filtro = if (categoria == c) null else c.id },
                        label = { Text("${c.icono} ${c.nombre}") },
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Rotulo(if (categoria == null) "Total del mes" else "Total en ${categoria.nombre.lowercase(LocaleEs)}", Modifier.weight(1f))
                Text(dinero(gastos.sumOf { it.monto }), style = MaterialTheme.typography.titleMedium)
            }
        }

        if (gastos.isEmpty()) {
            item {
                EstadoVacio(
                    categoria?.icono ?: "🧾",
                    "Nada por aquí",
                    if (categoria == null) "No hay gastos anotados este mes." else "No hay gastos con esta etiqueta este mes.",
                )
            }
        }

        porDia.forEach { (dia, lista) ->
            item(key = "dia-$dia") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                ) {
                    Text(
                        etiquetaDia(dia),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        dinero(lista.sumOf { it.monto }),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(lista, key = { it.id }) { gasto -> FilaGasto(gasto, datos.etiquetasDe(gasto)) { onAbrir(gasto.id) } }
        }
    }
}

@Composable
private fun FilaGasto(gasto: Gasto, etiquetas: List<Etiqueta>, onClick: () -> Unit) {
    val principal = etiquetas.first()
    val nombres = etiquetas.joinToString(", ") { it.nombre }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Insignia(principal.icono, Color(principal.color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                gasto.descripcion.ifBlank { nombres },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${etiquetas.joinToString(" ") { it.icono }}  $nombres  ·  ${gasto.fecha.aHora()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(dinero(gasto.monto), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
