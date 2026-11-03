@file:OptIn(ExperimentalMaterial3Api::class)

package com.misfinanzas.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misfinanzas.app.ui.theme.paleta
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- Lenguaje visual: stickers y boleta ----------

/**
 * Sticker: fondo de color, contorno de tinta y una sombra dura desplazada (sin difuminar).
 * La sombra se dibuja fuera del borde, así que el contenedor debe dejarle espacio.
 */
fun Modifier.sticker(
    fondo: Color,
    contorno: Color,
    forma: Shape,
    sombra: Dp = 4.dp,
    grosor: Dp = 2.dp,
): Modifier = this
    .drawBehind {
        val outline = forma.createOutline(size, layoutDirection, this)
        val d = sombra.toPx()
        if (d > 0f) translate(d, d) { drawOutline(outline, contorno) }
        drawOutline(outline, fondo)
    }
    .border(grosor, contorno, forma)

/** Rectángulo con esquinas superiores redondeadas y borde inferior dentado, como una boleta cortada. */
class FormaBoleta(private val radio: Dp = 22.dp, private val diente: Dp = 9.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radio.toPx() }
        val z = with(density) { diente.toPx() }
        val w = size.width
        val h = size.height
        val dientes = max(1, (w / (z * 2.2f)).roundToInt())
        val ancho = w / dientes
        val path = Path().apply {
            moveTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false)
            lineTo(w, h - z)
            for (i in dientes - 1 downTo 0) {
                lineTo(i * ancho + ancho / 2, h)
                lineTo(i * ancho, h - z)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Línea punteada, como la que separa los ítems de una boleta. */
@Composable
fun LineaPunteada(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
    }
}

/** Botón principal: un sticker que se "pega" a la página al presionarlo. */
@Composable
fun BotonSticker(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    fondo: Color = paleta.limon,
    compacto: Boolean = false,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val sombraBase = if (compacto) 3.dp else 4.dp
    val hundido by animateDpAsState(if (presionado) sombraBase else 0.dp, label = "hundido")
    val forma = RoundedCornerShape(if (compacto) 12.dp else 16.dp)
    Row(
        modifier
            .offset(hundido, hundido)
            .sticker(fondo, paleta.contorno, forma, sombra = sombraBase - hundido)
            .clip(forma)
            .clickable(interactionSource = interaccion, indication = null, onClick = onClick)
            .padding(
                horizontal = if (compacto) 14.dp else 20.dp,
                vertical = if (compacto) 8.dp else 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides paleta.tinta) {
            if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto, style = MaterialTheme.typography.titleMedium, color = paleta.tinta)
        }
    }
}

/** Chip con forma de sticker. Elegido: color de fondo y sombra; sin elegir: solo contorno. */
@Composable
fun ChipSticker(
    texto: String,
    elegido: Boolean,
    onClick: () -> Unit,
    color: Color = paleta.limon,
) {
    val forma = RoundedCornerShape(50)
    val contorno = paleta.contorno
    Box(
        Modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .then(
                if (elegido) Modifier.sticker(color, contorno, forma, sombra = 3.dp, grosor = 2.dp)
                else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), forma)
            )
            .clip(forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (elegido) paleta.tinta else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Cuadro de color con un emoji o una inicial. */
@Composable
fun Insignia(texto: String, color: Color, tamano: Dp = 44.dp, forma: Shape = RoundedCornerShape(30)) {
    Box(
        Modifier
            .size(tamano)
            .background(color.copy(alpha = 0.22f), forma),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, fontSize = (tamano.value * 0.46f).sp, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** Avatar de amigo: círculo de color sólido con la inicial, color estable según el nombre. */
@Composable
fun Avatar(nombre: String, tamano: Dp = 44.dp) {
    val colores = listOf(paleta.limon, paleta.menta, paleta.lila, paleta.tomate, Color(0xFF7CC6FF), Color(0xFFFFA8D9))
    val color = colores[Math.floorMod(nombre.trim().lowercase().hashCode(), colores.size)]
    Box(
        Modifier
            .size(tamano)
            .background(color, CircleShape)
            .border(2.dp, paleta.contorno, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nombre.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontSize = (tamano.value * 0.42f).sp,
            color = paleta.tinta,
        )
    }
}

// ---------- Encabezados y estados ----------

@Composable
fun SelectorMes(
    mes: YearMonth,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            nombreMes(mes, conAno = false),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            mes.year.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp).weight(1f),
        )
        BotonRedondo(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", onAnterior)
        Spacer(Modifier.width(8.dp))
        BotonRedondo(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", onSiguiente, enabled = mes < YearMonth.now())
    }
}

@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, onClick: () -> Unit, enabled: Boolean = true) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .size(40.dp)
            .border(2.dp, color, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = descripcion, tint = color)
    }
}

@Composable
fun EstadoVacio(emoji: String, titulo: String, detalle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// ---------- Formularios ----------

/** Esqueleto común de los formularios: barra con cerrar / guardar y contenido desplazable. */
@Composable
fun PantallaFormulario(
    titulo: String,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    accionExtra: @Composable () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                },
                actions = {
                    accionExtra()
                    BotonSticker("Guardar", onGuardar, compacto = true, modifier = Modifier.padding(end = 12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = contenido,
        )
    }
}

private fun soloMonto(texto: String) = texto.filter { it.isDigit() || it == '.' || it == ',' }

/** Campo de monto. El grande se escribe directo sobre la página, como en una boleta; el chico es un campo normal. */
@Composable
fun CampoMonto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    grande: Boolean = true,
    enfocar: Boolean = false,
) {
    val foco = remember { FocusRequester() }
    if (enfocar) LaunchedEffect(Unit) { foco.requestFocus() }
    if (!grande) {
        OutlinedTextField(
            value = valor,
            onValueChange = { onCambio(soloMonto(it)) },
            label = { Text(etiqueta) },
            prefix = { Text("$ ") },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = modifier.fillMaxWidth().focusRequester(foco),
        )
        return
    }
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val linea = if (error != null) MaterialTheme.colorScheme.error else colorTexto
    val estilo = MaterialTheme.typography.displayMedium.copy(color = colorTexto)
    Column(modifier.fillMaxWidth()) {
        Rotulo(etiqueta)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$", style = estilo, color = if (valor.isEmpty()) MaterialTheme.colorScheme.outline else colorTexto)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                if (valor.isEmpty()) Text("0", style = estilo, color = MaterialTheme.colorScheme.outlineVariant)
                BasicTextField(
                    value = valor,
                    onValueChange = { onCambio(soloMonto(it)) },
                    textStyle = estilo,
                    singleLine = true,
                    cursorBrush = SolidColor(colorTexto),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(linea, RoundedCornerShape(2.dp)))
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun SelectorFecha(fecha: LocalDate, onCambio: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), forma)
            .clip(forma)
            .clickable { abierto = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Fecha", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(fechaCorta(fecha), style = MaterialTheme.typography.titleMedium)
    }
    if (abierto) {
        // El DatePicker trabaja con medianoche UTC.
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        onCambio(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}
