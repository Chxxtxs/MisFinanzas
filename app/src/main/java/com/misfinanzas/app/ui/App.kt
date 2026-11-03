@file:OptIn(ExperimentalMaterial3Api::class)

package com.misfinanzas.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import com.misfinanzas.app.data.Actualizador
import com.misfinanzas.app.data.Datos
import com.misfinanzas.app.data.claveAmigo
import com.misfinanzas.app.data.resumirAmigos
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val datos by vm.datos.collectAsStateWithLifecycle()
    val cargado by vm.cargado.collectAsStateWithLifecycle()
    val mes by vm.mes.collectAsStateWithLifecycle()

    if (!cargado) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }

    BackHandler(enabled = vm.pila.size > 1) { vm.atras() }
    val amigos = remember(datos) { resumirAmigos(datos.deudas).map { it.nombre }.sorted() }

    when (val p = vm.pila.last()) {
        Pantalla.Inicio -> Inicio(vm, datos, mes)

        is Pantalla.EditarGasto -> FormGasto(
            gasto = p.id?.let { id -> datos.gastos.find { it.id == id } },
            etiquetas = datos.etiquetas,
            amigos = amigos,
            onGuardar = { monto, descripcion, etiquetas, fecha, partes ->
                vm.guardarGasto(p.id, monto, descripcion, etiquetas, fecha, partes)
                vm.atras()
            },
            onCrearEtiqueta = { nombre, emoji, color -> vm.guardarEtiqueta(null, nombre, emoji, color) },
            onEliminar = p.id?.let { id -> { vm.eliminarGasto(id); vm.atras() } },
            onCerrar = vm::atras,
        )

        is Pantalla.EditarDeuda -> FormDeuda(
            deuda = p.id?.let { id -> datos.deudas.find { it.id == id } },
            amigoInicial = p.amigo,
            tipoInicial = p.tipo,
            amigos = amigos,
            onGuardar = { nombres, monto, descripcion, tipo, fecha ->
                vm.guardarDeuda(p.id, nombres, monto, descripcion, tipo, fecha)
                vm.atras()
            },
            onEliminar = p.id?.let { id -> { vm.eliminarDeuda(id); vm.atras() } },
            onCerrar = vm::atras,
        )

        is Pantalla.Amigo -> DetalleAmigo(
            nombre = p.nombre,
            deudas = datos.deudas.filter { claveAmigo(it.amigo) == claveAmigo(p.nombre) },
            onAtras = vm::atras,
            onEditar = { vm.ir(Pantalla.EditarDeuda(it)) },
            onNueva = { tipo -> vm.ir(Pantalla.EditarDeuda(null, p.nombre, tipo)) },
            onAbonar = vm::abonar,
            onSaldarTodo = { vm.saldarAmigo(p.nombre) },
        )

        Pantalla.Etiquetas -> PantallaEtiquetas(
            datos = datos,
            onGuardar = { id, nombre, emoji, color -> vm.guardarEtiqueta(id, nombre, emoji, color) },
            onEliminar = vm::eliminarEtiqueta,
            onAtras = vm::atras,
        )
    }

    DialogoActualizacion(
        estado = vm.actualizacion,
        onDescargar = { vm.descargarActualizacion(it.lanzamiento) },
        onCerrar = { vm.actualizacion = EstadoActualizacion.Inactivo },
    )
}

private data class Pestana(val titulo: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana("Resumen", Icons.Filled.Home),
    Pestana("Gastos", Icons.AutoMirrored.Filled.List),
    Pestana("Amigos", Icons.Filled.Person),
)

@Composable
private fun Inicio(vm: AppViewModel, datos: Datos, mes: YearMonth) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var porImportar by remember { mutableStateOf<Uri?>(null) }

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportar(uri) { ok ->
            Toast.makeText(context, if (ok) "Respaldo guardado" else "No se pudo guardar el respaldo", Toast.LENGTH_SHORT).show()
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        porImportar = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.pestana == 0) "Mis Finanzas" else pestanas[vm.pestana].titulo) },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Buscar actualización") },
                            onClick = {
                                menu = false
                                vm.buscarActualizacion()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Etiquetas") },
                            onClick = {
                                menu = false
                                vm.ir(Pantalla.Etiquetas)
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Guardar respaldo") },
                            onClick = {
                                menu = false
                                exportar.launch("misfinanzas-respaldo-${LocalDate.now()}.json")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Restaurar respaldo") },
                            onClick = {
                                menu = false
                                importar.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Versión ${Actualizador.versionActual}") },
                            onClick = {},
                            enabled = false,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                pestanas.forEachIndexed { i, p ->
                    NavigationBarItem(
                        selected = vm.pestana == i,
                        onClick = { vm.pestana = i },
                        icon = { Icon(p.icono, contentDescription = null) },
                        label = { Text(p.titulo) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (vm.pestana == 2) {
                ExtendedFloatingActionButton(
                    onClick = { vm.ir(Pantalla.EditarDeuda(null)) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva deuda") },
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { vm.ir(Pantalla.EditarGasto(null)) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo gasto") },
                )
            }
        },
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (vm.pestana) {
            0 -> PestanaResumen(
                datos = datos,
                mes = mes,
                onMesAnterior = vm::mesAnterior,
                onMesSiguiente = vm::mesSiguiente,
                onVerAmigos = { vm.pestana = 2 },
                onVerGastos = { vm.pestana = 1 },
                modifier = mod,
            )
            1 -> PestanaGastos(
                datos = datos,
                mes = mes,
                onMesAnterior = vm::mesAnterior,
                onMesSiguiente = vm::mesSiguiente,
                onAbrir = { vm.ir(Pantalla.EditarGasto(it)) },
                modifier = mod,
            )
            else -> PestanaAmigos(
                datos = datos,
                onAbrir = { vm.ir(Pantalla.Amigo(it)) },
                modifier = mod,
            )
        }
    }

    porImportar?.let { uri ->
        AlertDialog(
            onDismissRequest = { porImportar = null },
            title = { Text("¿Restaurar respaldo?") },
            text = { Text("Esto reemplaza todos los gastos y deudas actuales por los del archivo.") },
            confirmButton = {
                TextButton(onClick = {
                    porImportar = null
                    vm.importar(uri) { ok ->
                        Toast.makeText(context, if (ok) "Respaldo restaurado" else "El archivo no es un respaldo válido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { porImportar = null }) { Text("Cancelar") } },
        )
    }
}
