package com.misfinanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.Etiqueta
import com.misfinanzas.app.data.ID_HORMIGA
import com.misfinanzas.app.data.resumirAmigos
import com.misfinanzas.app.ui.theme.colorAFavor
import com.misfinanzas.app.ui.theme.colorEnContra
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PestanaResumen(
    datos: Datos,
    mes: YearMonth,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onVerAmigos: () -> Unit,
    onVerGastos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gastosMes = remember(datos, mes) { datos.gastos.filter { it.fecha.aMes() == mes } }
    val total = gastosMes.sumOf { it.monto }
    val totalAnterior = remember(datos, mes) {
        datos.gastos.filter { it.fecha.aMes() == mes.minusMonths(1) }.sumOf { it.monto }
    }
    // Un gasto con varias etiquetas suma completo en cada una.
    val porEtiqueta = remember(gastosMes, datos.etiquetas) {
        gastosMes.flatMap { g -> datos.etiquetasDe(g).map { it to g } }
            .groupBy({ it.first }, { it.second })
            .map { (etiqueta, lista) -> Triple(etiqueta, lista.sumOf { it.monto }, lista.size) }
            .sortedByDescending { it.second }
    }
    val hayVarias = gastosMes.any { it.etiquetas.size > 1 }
    val amigos = remember(datos) { resumirAmigos(datos.deudas) }
    val teDeben = amigos.sumOf { it.neto.coerceAtLeast(0.0) }
    val debes = amigos.sumOf { (-it.neto).coerceAtLeast(0.0) }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectorMes(mes, onMesAnterior, onMesSiguiente) }

        item {
            TarjetaTotal(
                total = total,
                totalAnterior = totalAnterior,
                mes = mes,
                cantidad = gastosMes.size,
                onClick = onVerGastos,
            )
        }

        item { TarjetaAmigos(teDeben, debes, onVerAmigos) }

        porEtiqueta.firstOrNull { it.first.id == ID_HORMIGA }?.let { (_, monto, cantidad) ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🐜", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "$cantidad ${if (cantidad == 1) "gasto hormiga sumó" else "gastos hormiga sumaron"} " +
                                "${dinero(monto)} este mes" +
                                if (total > 0) " (${(monto / total * 100).roundToInt()}% del total)." else ".",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        if (porEtiqueta.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("Por etiqueta", style = MaterialTheme.typography.titleMedium)
                    if (hayVarias) {
                        Text(
                            "Los gastos con varias etiquetas cuentan en cada una.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(porEtiqueta, key = { "et-" + it.first.id }) { (etiqueta, monto, cantidad) ->
                FilaEtiqueta(etiqueta, monto, cantidad, if (total > 0) (monto / total).toFloat() else 0f)
            }
        } else {
            item {
                EstadoVacio(
                    "🌱",
                    "Sin gastos en ${nombreMes(mes, conAno = false).lowercase(LocaleEs)}",
                    "Toca \"Nuevo gasto\" para anotar el primero.",
                )
            }
        }
    }
}

@Composable
private fun TarjetaTotal(
    total: Double,
    totalAnterior: Double,
    mes: YearMonth,
    cantidad: Int,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Gastaste en ${nombreMes(mes, conAno = false).lowercase(LocaleEs)}", style = MaterialTheme.typography.bodyMedium)
            Text(dinero(total), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))

            val hoy = LocalDate.now()
            val dias = if (mes == YearMonth.from(hoy)) hoy.dayOfMonth else mes.lengthOfMonth()
            val partes = buildList {
                add("$cantidad ${if (cantidad == 1) "gasto" else "gastos"}")
                if (total > 0) add("${dinero(total / dias)} al día")
                if (totalAnterior > 0) {
                    val cambio = ((total - totalAnterior) / totalAnterior * 100).roundToInt()
                    val flecha = if (cambio > 0) "▲" else if (cambio < 0) "▼" else "="
                    add("$flecha ${abs(cambio)}% vs. ${nombreMes(mes.minusMonths(1), conAno = false).lowercase(LocaleEs)}")
                }
            }
            Text(
                partes.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun TarjetaAmigos(teDeben: Double, debes: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                Rotulo("Te deben")
                Text(dinero(teDeben), style = MaterialTheme.typography.titleLarge, color = colorAFavor)
            }
            Box(Modifier.width(1.dp).height(44.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                Rotulo("Debes")
                Text(dinero(debes), style = MaterialTheme.typography.titleLarge, color = colorEnContra)
            }
        }
    }
}

@Composable
private fun FilaEtiqueta(cat: Etiqueta, monto: Double, cantidad: Int, fraccion: Float) {
    val color = Color(cat.color)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Insignia(cat.icono, color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(cat.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(dinero(monto), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraccion.coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${(fraccion * 100).roundToInt()}%  ·  $cantidad ${if (cantidad == 1) "gasto" else "gastos"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
