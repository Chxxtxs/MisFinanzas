@file:OptIn(ExperimentalTextApi::class)

package com.misfinanzas.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misfinanzas.app.R

/*
 * Identidad "boleta con stickers": tinta azul marino, papel azulado, limón para lo importante,
 * tomate para lo que debes y menta para lo que te deben.
 */

@Immutable
data class Paleta(
    /** Contorno y sombra dura de los stickers (igual en claro y oscuro). */
    val contorno: Color,
    /** Texto sobre limón u otros fondos de color: siempre tinta oscura. */
    val tinta: Color,
    val limon: Color,
    val tomate: Color,
    val menta: Color,
    val lila: Color,
    /** Texto de montos a favor / en contra, con contraste suficiente sobre el fondo. */
    val aFavor: Color,
    val enContra: Color,
    /** Fondo de tarjetas y filas destacadas. */
    val tarjeta: Color,
)

private val Tinta = Color(0xFF1B1F4B)

private val PaletaClara = Paleta(
    contorno = Tinta,
    tinta = Tinta,
    limon = Color(0xFFFFDD3C),
    tomate = Color(0xFFFF5B3A),
    menta = Color(0xFF22C08A),
    lila = Color(0xFFB6A6FF),
    aFavor = Color(0xFF0B7A54),
    enContra = Color(0xFFD2391A),
    tarjeta = Color.White,
)

private val PaletaOscura = PaletaClara.copy(
    contorno = Color(0xFF07081C),
    aFavor = Color(0xFF4BE0A6),
    enContra = Color(0xFFFF8266),
    tarjeta = Color(0xFF1C2050),
)

private val Claro = lightColorScheme(
    primary = Tinta,
    onPrimary = Color(0xFFF2F4FF),
    primaryContainer = Color(0xFFFFDD3C),
    onPrimaryContainer = Tinta,
    secondary = Color(0xFFD2391A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3DDFF),
    onSecondaryContainer = Tinta,
    tertiary = Color(0xFF0B7A54),
    tertiaryContainer = Color(0xFFCFF5E6),
    onTertiaryContainer = Tinta,
    background = Color(0xFFF2F4FF),
    onBackground = Tinta,
    surface = Color(0xFFF2F4FF),
    onSurface = Tinta,
    surfaceVariant = Color(0xFFE5E8F8),
    onSurfaceVariant = Color(0xFF5A5E85),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FF),
    surfaceContainer = Color(0xFFECEEFB),
    surfaceContainerHigh = Color(0xFFE5E8F8),
    surfaceContainerHighest = Color(0xFFDDE1F5),
    outline = Color(0xFF8A8EB3),
    outlineVariant = Color(0xFFCDD1EA),
    error = Color(0xFFD2391A),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFDD3C),
    onPrimary = Tinta,
    primaryContainer = Color(0xFF3A3F7A),
    onPrimaryContainer = Color(0xFFEEF0FF),
    secondary = Color(0xFFFF8266),
    onSecondary = Tinta,
    secondaryContainer = Color(0xFF3A3F7A),
    onSecondaryContainer = Color(0xFFEEF0FF),
    tertiary = Color(0xFF4BE0A6),
    tertiaryContainer = Color(0xFF1E4B45),
    onTertiaryContainer = Color(0xFFEEF0FF),
    background = Color(0xFF12153A),
    onBackground = Color(0xFFEEF0FF),
    surface = Color(0xFF12153A),
    onSurface = Color(0xFFEEF0FF),
    surfaceVariant = Color(0xFF252A5E),
    onSurfaceVariant = Color(0xFFA9AED6),
    surfaceContainerLowest = Color(0xFF0E1030),
    surfaceContainerLow = Color(0xFF181C48),
    surfaceContainer = Color(0xFF1C2050),
    surfaceContainerHigh = Color(0xFF252A5E),
    surfaceContainerHighest = Color(0xFF2F346B),
    outline = Color(0xFF6E73A6),
    outlineVariant = Color(0xFF34396F),
    error = Color(0xFFFF8266),
)

// Bricolage Grotesque es variable: el eje "opsz" ajusta el dibujo para texto chico o grande.
private fun bricolage(peso: Int, opsz: Float) = Font(
    R.font.bricolage,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(peso),
        FontVariation.Setting("opsz", opsz),
    ),
)

/** Para montos y títulos. */
val Titular = FontFamily(
    bricolage(500, 72f),
    bricolage(600, 72f),
    bricolage(700, 72f),
    bricolage(800, 72f),
)

/** Para texto corrido y etiquetas. */
private val Texto = FontFamily(
    bricolage(400, 14f),
    bricolage(500, 14f),
    bricolage(600, 14f),
    bricolage(700, 14f),
)

private fun titular(tamano: Int, peso: Int, espaciado: Float = 0f, alto: Int = (tamano * 1.15f).toInt()) = TextStyle(
    fontFamily = Titular,
    fontWeight = FontWeight(peso),
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    letterSpacing = espaciado.sp,
)

private fun texto(tamano: Int, peso: Int, alto: Int) = TextStyle(
    fontFamily = Texto,
    fontWeight = FontWeight(peso),
    fontSize = tamano.sp,
    lineHeight = alto.sp,
)

private val Tipografia = Typography(
    displayLarge = titular(64, 800, -2.5f),
    displayMedium = titular(52, 800, -2f),
    displaySmall = titular(40, 800, -1.4f),
    headlineLarge = titular(32, 800, -1f),
    headlineMedium = titular(28, 800, -0.8f),
    headlineSmall = titular(24, 700, -0.5f),
    titleLarge = titular(22, 800, -0.4f),
    titleMedium = titular(17, 700, -0.2f),
    titleSmall = texto(14, 700, 20),
    bodyLarge = texto(16, 400, 23),
    bodyMedium = texto(14, 400, 20),
    bodySmall = texto(12, 400, 17),
    labelLarge = texto(14, 600, 20),
    labelMedium = texto(12, 600, 16),
    labelSmall = texto(11, 600, 14),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(50),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalPaleta = staticCompositionLocalOf { PaletaClara }

@Composable
fun TemaFinanzas(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    CompositionLocalProvider(LocalPaleta provides if (oscuro) PaletaOscura else PaletaClara) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            typography = Tipografia,
            shapes = Formas,
            content = content,
        )
    }
}

val paleta: Paleta
    @Composable @ReadOnlyComposable
    get() = LocalPaleta.current

/** Verde: plata que te deben. */
val colorAFavor: Color
    @Composable @ReadOnlyComposable
    get() = LocalPaleta.current.aFavor

/** Rojo: plata que debes. */
val colorEnContra: Color
    @Composable @ReadOnlyComposable
    get() = LocalPaleta.current.enContra
