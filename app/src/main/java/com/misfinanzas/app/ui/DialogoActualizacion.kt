package com.misfinanzas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.data.Actualizador
import kotlin.math.roundToInt

/** Muestra el paso actual de la búsqueda / descarga / instalación de una versión nueva. */
@Composable
fun DialogoActualizacion(
    estado: EstadoActualizacion,
    onDescargar: (EstadoActualizacion.Disponible) -> Unit,
    onCerrar: () -> Unit,
) {
    val context = LocalContext.current
    val cerrar = @Composable { TextButton(onClick = onCerrar) { Text("Cerrar") } }

    when (estado) {
        EstadoActualizacion.Inactivo -> Unit

        EstadoActualizacion.Buscando -> AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Buscando actualización…") },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
            confirmButton = {},
            dismissButton = { cerrar() },
        )

        EstadoActualizacion.AlDia -> AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Estás al día") },
            text = { Text("Tienes la última versión (${Actualizador.versionActual}).") },
            confirmButton = { cerrar() },
        )

        is EstadoActualizacion.Disponible -> AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Versión ${estado.lanzamiento.version} disponible") },
            text = {
                Column(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Tienes la ${Actualizador.versionActual}. Tus datos se mantienen al actualizar.")
                    if (estado.lanzamiento.notas.isNotBlank()) {
                        Text(
                            estado.lanzamiento.notas,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onDescargar(estado) }) { Text("Descargar") } },
            dismissButton = { TextButton(onClick = onCerrar) { Text("Ahora no") } },
        )

        is EstadoActualizacion.Descargando -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Descargando… ${(estado.progreso * 100).roundToInt()}%") },
            text = { LinearProgressIndicator(progress = { estado.progreso }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {},
        )

        is EstadoActualizacion.Lista -> {
            val permitido = Actualizador.puedeInstalar(context)
            AlertDialog(
                onDismissRequest = onCerrar,
                title = { Text("Lista para instalar") },
                text = {
                    Text(
                        if (permitido) "Toca Instalar y luego confirma en la ventana de Android."
                        else "Android necesita que permitas a Mis Finanzas instalar apps. Activa el permiso y vuelve aquí."
                    )
                },
                confirmButton = {
                    if (permitido) {
                        TextButton(onClick = { Actualizador.instalar(context, estado.apk) }) { Text("Instalar") }
                    } else {
                        TextButton(onClick = { Actualizador.pedirPermisoInstalar(context) }) { Text("Dar permiso") }
                    }
                },
                dismissButton = { cerrar() },
            )
        }

        is EstadoActualizacion.Fallo -> AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Ups") },
            text = { Text(estado.mensaje) },
            confirmButton = { cerrar() },
        )
    }
}
