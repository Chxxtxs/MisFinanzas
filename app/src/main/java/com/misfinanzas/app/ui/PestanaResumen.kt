package com.misfinanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.Etiqueta
import com.misfinanzas.app.data.ID_HORMIGA
import com.misfinanzas.app.data.resumirAmigos
import com.misfinanzas.app.ui.theme.colorAFavor
import com.misfinanzas.app.ui.theme.colorEnContra
import com.misfinanzas.app.ui.theme.paleta
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SelectorMes(mes, onMesAnterior, onMesSiguiente) }

        item {
            Boleta(
                total = total,
                totalAnterior = totalAnterior,
                mes = mes,
                cantidad = gastosMes.size,
                onClick = onVerGastos,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BloqueSaldo("Te deben", teDeben, colorAFavor, paleta.menta, Modifier.weight(1f), onVerAmigos)
                BloqueSaldo("Debes", debes, colorEnContra, paleta.tomate, Modifier.weight(1f), onVerAmigos)
            }
        }

        porEtiqueta.firstOrNull { it.first.id == ID_HORMIGA }?.let { (_, monto, cantidad) ->
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(paleta.tarjeta, RoundedCornerShape(18.dp))
                        .border(2.dp, paleta.limon, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🐜", fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "${dinero(monto)} en gastos hormiga",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "$cantidad ${if (cantidad == 1) "compra chica" else "compras chicas"}" +
                                if (total > 0) ", el ${(monto / total * 100).roundToInt()}% de lo que llevas este mes." else ".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (porEtiqueta.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("En qué se fue", style = MaterialTheme.typography.headlineSmall)
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
                    "🧾",
                    "Boleta en blanco",
                    "Todavía no anotas gastos en ${nombreMes(mes, conAno = false).lowercase(LocaleEs)}. Toca Anotar gasto.",
                )
            }
        }
    }
}

/** El total del mes como una boleta amarilla cortada. */
@Composable
private fun Boleta(
    total: Double,
    totalAnterior: Double,
    mes: YearMonth,
    cantidad: Int,
    onClick: () -> Unit,
) {
    val forma = remember { FormaBoleta() }
    val tinta = paleta.tinta
    val hoy = LocalDate.now()
    val dias = if (mes == YearMonth.from(hoy)) hoy.dayOfMonth else mes.lengthOfMonth()
    val textoTotal = dinero(total)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(end = 5.dp, bottom = 5.dp)
            .sticker(paleta.limon, paleta.contorno, forma, sombra = 5.dp, grosor = 2.5.dp)
            .clip(forma)
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 30.dp),
    ) {
        Text(
            if (mes == YearMonth.from(hoy)) "Llevas gastado" else "Gastaste",
            style = MaterialTheme.typography.titleMedium,
            color = tinta,
        )
        Text(
            textoTotal,
            style = when {
                textoTotal.length > 11 -> MaterialTheme.typography.displaySmall
                textoTotal.length > 8 -> MaterialTheme.typography.displayMedium
                else -> MaterialTheme.typography.displayLarge
            },
            color = tinta,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
        Spacer(Modifier.height(14.dp))
        LineaPunteada(tinta.copy(alpha = 0.45f))
        Spacer(Modifier.height(10.dp))
        LineaBoleta("Gastos anotados", cantidad.toString())
        if (total > 0) LineaBoleta("Promedio por día", dinero(total / dias))
        if (totalAnterior > 0) {
            val cambio = ((total - totalAnterior) / totalAnterior * 100).roundToInt()
            val valor = when {
                cambio > 0 -> "▲ $cambio% más"
                cambio < 0 -> "▼ ${abs(cambio)}% menos"
                else -> "Igual"
            }
            LineaBoleta("Contra ${nombreMes(mes.minusMonths(1), conAno = false).lowercase(LocaleEs)}", valor)
        }
    }
}

@Composable
private fun LineaBoleta(nombre: String, valor: String) {
    val tinta = paleta.tinta
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(nombre, style = MaterialTheme.typography.bodyMedium, color = tinta.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleSmall, color = tinta)
    }
}

/** Bloque de "Te deben" / "Debes", con el monto en el color que corresponde. */
@Composable
fun BloqueSaldo(
    titulo: String,
    monto: Double,
    colorTexto: Color,
    colorFondo: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier
            .background(colorFondo.copy(alpha = 0.16f), forma)
            .border(2.dp, colorFondo.copy(alpha = 0.55f), forma)
            .clip(forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            dinero(monto),
            style = MaterialTheme.typography.headlineSmall,
            color = if (monto > 0.005) colorTexto else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun FilaEtiqueta(etiqueta: Etiqueta, monto: Double, cantidad: Int, fraccion: Float) {
    val color = Color(etiqueta.color)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Insignia(etiqueta.icono, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(etiqueta.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(dinero(monto), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraccion.coerceIn(0.03f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    if (cantidad == 1) "1 gasto" else "$cantidad gastos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(fraccion * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
