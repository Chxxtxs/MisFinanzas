package com.misfinanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.compositeOver
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
        contentPadding = PaddingValues(bottom = 112.dp),
    ) {
        item { SelectorMes(mes, onMesAnterior, onMesSiguiente, Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) }

        item {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChipSticker("Todo", categoria == null, { filtro = null })
                datos.etiquetas.forEach { c ->
                    ChipSticker(
                        "${c.icono} ${c.nombre}",
                        categoria == c,
                        { filtro = if (categoria == c) null else c.id },
                        color = Color(c.color).copy(alpha = 0.55f).compositeOver(Color.White),
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    if (categoria == null) "Total del mes" else "Total en ${categoria.nombre.lowercase(LocaleEs)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                )
                Text(dinero(gastos.sumOf { it.monto }), style = MaterialTheme.typography.headlineMedium)
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
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(etiquetaDia(dia), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
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
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Insignia(principal.icono, Color(principal.color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                gasto.descripcion.ifBlank { etiquetas.joinToString(", ") { it.nombre } },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                etiquetas.take(3).forEach { e ->
                    Text(
                        e.nombre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .background(Color(e.color).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(dinero(gasto.monto), style = MaterialTheme.typography.titleMedium)
            Text(
                gasto.fecha.aHora(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
