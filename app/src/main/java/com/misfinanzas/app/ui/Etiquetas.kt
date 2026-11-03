@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.misfinanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.Etiqueta
import com.misfinanzas.app.data.coloresEtiqueta

/** Pantalla para ver, crear, editar y borrar etiquetas. */
@Composable
fun PantallaEtiquetas(
    datos: Datos,
    onGuardar: (id: String?, nombre: String, emoji: String, color: Long) -> Unit,
    onEliminar: (String) -> Unit,
    onAtras: () -> Unit,
) {
    var editando by remember { mutableStateOf<Etiqueta?>(null) }
    var creando by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf<Etiqueta?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Etiquetas", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
        floatingActionButton = {
            BotonSticker("Nueva etiqueta", { creando = true }, icono = Icons.Filled.Add)
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    "Un gasto puede tener varias etiquetas. Toca una para editarla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(datos.etiquetas, key = { it.id }) { e ->
                val usos = datos.gastos.count { e.id in it.etiquetas }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editando = e }
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Insignia(e.icono, Color(e.color))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.nombre, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (usos == 1) "1 gasto" else "$usos gastos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { borrando = e }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${e.nombre}")
                    }
                }
            }
        }
    }

    if (creando) {
        DialogoEtiqueta(
            etiqueta = null,
            onGuardar = { nombre, emoji, color -> onGuardar(null, nombre, emoji, color); creando = false },
            onCancelar = { creando = false },
        )
    }
    editando?.let { e ->
        DialogoEtiqueta(
            etiqueta = e,
            onGuardar = { nombre, emoji, color -> onGuardar(e.id, nombre, emoji, color); editando = null },
            onCancelar = { editando = null },
        )
    }
    borrando?.let { e ->
        val usos = datos.gastos.count { e.id in it.etiquetas }
        AlertDialog(
            onDismissRequest = { borrando = null },
            title = { Text("¿Eliminar \"${e.nombre}\"?") },
            text = {
                Text(
                    if (usos == 0) "Ningún gasto la usa."
                    else "Se quitará de $usos ${if (usos == 1) "gasto" else "gastos"}. Los gastos no se borran."
                )
            },
            confirmButton = { TextButton(onClick = { onEliminar(e.id); borrando = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { borrando = null }) { Text("Cancelar") } },
        )
    }
}

/** Diálogo para crear (etiqueta nula) o editar una etiqueta. */
@Composable
fun DialogoEtiqueta(
    etiqueta: Etiqueta?,
    onGuardar: (nombre: String, emoji: String, color: Long) -> Unit,
    onCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(etiqueta?.nombre ?: "") }
    var emoji by remember { mutableStateOf(etiqueta?.emoji ?: "") }
    var color by remember { mutableStateOf(etiqueta?.color ?: coloresEtiqueta.random()) }
    var intentado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (etiqueta == null) "Nueva etiqueta" else "Editar etiqueta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Insignia(emoji.ifBlank { nombre.take(1).uppercase().ifBlank { "?" } }, Color(color), 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it.take(24) },
                        label = { Text("Nombre") },
                        isError = intentado && nombre.isBlank(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(8) },
                    label = { Text("Emoji (opcional)") },
                    placeholder = { Text("Ej: ☕ 🐶 🎮 💊") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Rotulo("Color")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    coloresEtiqueta.forEach { c ->
                        val elegido = c == color
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Color(c), CircleShape)
                                .then(
                                    if (elegido) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (elegido) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intentado = true
                if (nombre.isNotBlank()) onGuardar(nombre, emoji, color)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/** Chips para elegir varias etiquetas, con un atajo para crear una nueva. */
@Composable
fun SelectorEtiquetas(
    etiquetas: List<Etiqueta>,
    elegidas: List<String>,
    onCambio: (List<String>) -> Unit,
    onCrear: (nombre: String, emoji: String, color: Long) -> String,
    error: Boolean,
) {
    var creando by remember { mutableStateOf(false) }
    Column {
        Rotulo(if (error) "Elige al menos una etiqueta" else "Etiquetas (puedes elegir varias)")
        Spacer(Modifier.size(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            etiquetas.forEach { e ->
                val elegida = e.id in elegidas
                ChipSticker(
                    "${e.icono}  ${e.nombre}",
                    elegida,
                    { onCambio(if (elegida) elegidas - e.id else elegidas + e.id) },
                    color = Color(e.color).copy(alpha = 0.5f).compositeOver(Color.White),
                )
            }
            ChipSticker("+ Nueva", false, { creando = true })
        }
        if (error) {
            Text(
                "Toca una o más opciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (creando) {
        DialogoEtiqueta(
            etiqueta = null,
            onGuardar = { nombre, emoji, color ->
                val id = onCrear(nombre, emoji, color)
                onCambio(elegidas + id)
                creando = false
            },
            onCancelar = { creando = false },
        )
    }
}
