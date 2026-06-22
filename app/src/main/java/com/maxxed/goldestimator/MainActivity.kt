package com.maxxed.goldestimator

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { GoldApp() }
    }
}

private val GoldLight = lightColorScheme(primary = Color(0xFF8A6715), secondary = Color(0xFF087A56), background = Color(0xFFF5F4EF), surface = Color.White)
private val GoldDark = darkColorScheme(primary = Color(0xFFE0BE55), secondary = Color(0xFF52D6A5), background = Color(0xFF111315), surface = Color(0xFF1A1E21))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldApp(vm: GoldViewModel = viewModel()) {
    val state by vm.state
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    var csvPending by remember { mutableStateOf("") }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(csvPending) }
    }
    MaterialTheme(colorScheme = if (dark) GoldDark else GoldLight) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(titleFor(state.screen)) },
                navigationIcon = {
                    if (state.screen != Screen.HOME) IconButton(onClick = { vm.navigate(if (state.screen in listOf(Screen.RESULTS, Screen.CORRECT, Screen.REFERENCE, Screen.CAMERA)) Screen.HOME else Screen.HOME) }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.navigate(Screen.HISTORY) }) { Icon(Icons.Default.History, "Saved batches") }
                    IconButton(onClick = { vm.navigate(Screen.SAFETY) }) { Icon(Icons.Default.Info, "Safe recovery guidance") }
                }
            )
        }) { padding ->
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
                when (state.screen) {
                    Screen.HOME -> HomeScreen(state, vm)
                    Screen.CAMERA -> CaptureScreen(state, vm)
                    Screen.REFERENCE -> ReferenceScreen(state, vm)
                    Screen.CORRECT -> CorrectionScreen(state, vm)
                    Screen.RESULTS -> ResultsScreen(state.current, onSave = vm::saveCurrent, onCopy = { copy(context, vm.summary(it)) }, onExport = {
                        csvPending = vm.csv(listOf(it)); csvLauncher.launch("${safeName(it.name)}-visual-estimate.csv")
                    })
                    Screen.HISTORY -> HistoryScreen(state, vm, onExport = { items -> csvPending = vm.csv(items); csvLauncher.launch("gold-visual-estimates.csv") })
                    Screen.COMPARE -> CompareScreen(state.saved.filter { it.id in state.compareIds })
                    Screen.SAFETY -> SafetyScreen()
                }
                if (state.busy) Surface(Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.45f)) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
        }
        state.error?.let { message -> AlertDialog(onDismissRequest = vm::clearError, confirmButton = { Button(onClick = vm::clearError) { Text("OK") } }, title = { Text("Capture check") }, text = { Text(message) }) }
    }
}

@Composable
private fun HomeScreen(state: GoldUiState, vm: GoldViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Offline visual material estimates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Capture a dry sample from six distinct angles, measure a known-size reference, review the mask and material labels, then save or export the result.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Notice("Visual estimates only. Gold-colored pixels can be pyrite, mica, staining, or other material. This app does not perform a chemical assay.")
        Button(onClick = vm::newBatch, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Start new batch") }
        OutlinedButton(onClick = { vm.navigate(Screen.HISTORY) }, modifier = Modifier.fillMaxWidth()) { Text("Saved batches (${state.saved.size})") }
        OutlinedButton(onClick = { vm.navigate(Screen.SAFETY) }, modifier = Modifier.fillMaxWidth()) { Text("Safe recovery guidance") }
        SectionCard("Before capture") {
            Text("Use diffuse light, a stable phone, a dry and reasonably clean sample, and a flat reference object whose exact length you know. Keep the reference in the same plane as the sample.")
        }
    }
}

@Composable
private fun CaptureScreen(state: GoldUiState, vm: GoldViewModel) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    if (!granted) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Camera access is required to capture sample views. Photos remain on this device and are not transmitted.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
        }
        return
    }
    val next = GoldViewModel.ANGLES.getOrElse(state.captures.size) { "Complete" }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${state.captures.size + 1} of ${GoldViewModel.ANGLES.size}: $next", style = MaterialTheme.typography.titleLarge)
            Text("Fill the frame with the sample and reference. Move around the sample for each named view.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CameraPreview(Modifier.weight(1f).fillMaxWidth(), onCaptured = vm::acceptCapture)
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Accepted ${state.captures.size}/${GoldViewModel.ANGLES.size}")
            if (state.captures.isNotEmpty()) OutlinedButton(onClick = vm::removeLastCapture) { Text("Retake last") }
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier, onCaptured: (File, CaptureQuality, CapturePose) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val worker = remember { Executors.newSingleThreadExecutor() }
    val pose = rememberOrientationPose()
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            runCatching { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture); ready = true }
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { providerFuture.get().unbindAll() }; worker.shutdown() }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.78f).aspectRatio(1f).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
        Button(enabled = ready && pose != null, onClick = {
            val poseAtCapture = pose ?: return@Button
            val dir = File(context.filesDir, "captures").apply { mkdirs() }
            val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), worker, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runCatching { BitmapPipeline.decodeSamples(file).first }.onSuccess { samples ->
                        ContextCompat.getMainExecutor(context).execute { onCaptured(file, VisualAnalysis.quality(samples), poseAtCapture) }
                    }.onFailure { file.delete() }
                }
                override fun onError(exception: ImageCaptureException) { file.delete() }
            })
        }, modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Capture") }
        if (pose == null) Text("Waiting for orientation sensor", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).background(Color.Black.copy(alpha = 0.65f)).padding(8.dp))
    }
}

@Composable
private fun rememberOrientationPose(): CapturePose? {
    val context = LocalContext.current
    var pose by remember { mutableStateOf<CapturePose?>(null) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                pose = CapturePose(
                    Math.toDegrees(orientation[0].toDouble()).let { if (it < 0) it + 360 else it },
                    Math.toDegrees(orientation[1].toDouble()),
                    Math.toDegrees(orientation[2].toDouble())
                )
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return pose
}

@Composable
private fun ReferenceScreen(state: GoldUiState, vm: GoldViewModel) {
    val bitmap = remember(state.captures.firstOrNull()) { state.captures.firstOrNull()?.let { BitmapFactory.decodeFile(it.absolutePath) } }
    var points by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var boxWidth by remember { mutableStateOf(1) }
    var referenceText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tap both ends of the known-length reference. Tap again to restart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (bitmap != null) Box(Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height).background(Color.Black).onSizeChanged { boxWidth = it.width }.pointerInput(Unit) {
            detectTapGestures { tap -> points = if (points.size >= 2) listOf(tap) else points + tap }
        }) {
            androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                points.forEach { drawCircle(Color(0xFFE0BE55), 12f, it) }
                if (points.size == 2) drawLine(Color(0xFFE0BE55), points[0], points[1], 7f)
            }
        }
        OutlinedTextField(referenceText, { referenceText = numeric(it) }, label = { Text("Known reference length (mm)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(weightText, { weightText = numeric(it) }, label = { Text("Optional measured total weight (g)") }, supportingText = { Text("Leave blank to estimate a broad volume and weight range.") }, modifier = Modifier.fillMaxWidth())
        val measuredPixels = if (points.size == 2 && bitmap != null) hypot((points[1].x - points[0].x).toDouble(), (points[1].y - points[0].y).toDouble()) * bitmap.width / boxWidth else null
        Text(if (measuredPixels == null) "Reference line not set" else "Measured line: ${measuredPixels.oneDecimal()} image pixels")
        Button(onClick = { vm.setReference(referenceText.toDoubleOrNull(), measuredPixels, weightText.toDoubleOrNull()) }, modifier = Modifier.fillMaxWidth()) { Text("Analyze on device") }
    }
}

@Composable
private fun CorrectionScreen(state: GoldUiState, vm: GoldViewModel) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Notice("Review every cluster. A gold-colored cluster is not proof of gold. Assign Unknown whenever appearance is ambiguous.")
        Text("Mask sensitivity", style = MaterialTheme.typography.titleMedium)
        val foregroundPercent = if (state.originalPixelArea == null) 0 else (((state.subjectPixelArea ?: 0.0) / state.originalPixelArea) * 100).toInt()
        Text("Adjust until the sample is included while the surrounding background is excluded. Current foreground: $foregroundPercent%")
        Slider(state.maskSensitivity.toFloat(), { vm.updateMask(it.toDouble()) }, valueRange = 0f..1f)
        state.clusters.forEach { ClusterEditor(it, state.assignments[it.id] ?: it.suggested) { material -> vm.assign(it.id, material) } }
        OutlinedTextField(name, { name = it.take(60) }, label = { Text("Batch name") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.calculate(name) }, modifier = Modifier.fillMaxWidth()) { Text("Calculate visual estimate") }
    }
}

@Composable
private fun ClusterEditor(cluster: VisualCluster, selected: Material, onSelect: (Material) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).background(Color(cluster.meanR, cluster.meanG, cluster.meanB), RoundedCornerShape(4.dp)))
            Column(Modifier.weight(1f)) { Text("Cluster ${cluster.id + 1}", fontWeight = FontWeight.Bold); Text("${cluster.share.percentText()} visible pixels; ${(cluster.confidence * 100).toInt()}% label confidence", style = MaterialTheme.typography.bodySmall) }
            Box { OutlinedButton(onClick = { expanded = true }) { Text(selected.label) }; DropdownMenu(expanded, { expanded = false }) { Material.entries.forEach { material -> DropdownMenuItem({ Text(material.label) }, onClick = { onSelect(material); expanded = false }) } } }
        }
    }
}

@Composable
private fun ResultsScreen(batch: BatchResult?, onSave: () -> Unit, onCopy: (BatchResult) -> Unit, onExport: (BatchResult) -> Unit) {
    if (batch == null) return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(batch.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Notice("Visual estimate only, not a chemical assay. Confidence ${(batch.overallConfidence * 100).toInt()}%.")
        batch.warnings.forEach { Text("- $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("Material estimates", style = MaterialTheme.typography.titleLarge)
        batch.estimates.forEach { EstimateRow(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = { onCopy(batch) }) { Icon(Icons.Default.ContentCopy, "Copy") }
            OutlinedButton(onClick = { onExport(batch) }) { Icon(Icons.Default.Download, "Export CSV") }
        }
    }
}

@Composable
private fun EstimateRow(estimate: MaterialEstimate) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(estimate.material.label, fontWeight = FontWeight.Bold, color = if (estimate.material == Material.GOLD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text("Visible share: ${(estimate.visibleShareLow * 100).oneDecimal()}-${(estimate.visibleShareHigh * 100).oneDecimal()}%")
            Text("Density range: ${estimate.densityLow.oneDecimal()}-${estimate.densityHigh.oneDecimal()} g/cm3")
            Text("Volume: ${GoldViewModel.range(estimate.volumeLowCm3, estimate.volumeHighCm3, "cm3")}")
            Text("Weight: ${GoldViewModel.range(estimate.weightLowG, estimate.weightHighG, "g")}")
            Text("Confidence: ${(estimate.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HistoryScreen(state: GoldUiState, vm: GoldViewModel, onExport: (List<BatchResult>) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.saved.isEmpty()) { Text("No saved batches yet."); return@Column }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.compareIds.size == 2, onClick = { vm.navigate(Screen.COMPARE) }) { Text("Compare selected") }
            OutlinedButton(onClick = { onExport(state.saved) }) { Text("Export all CSV") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.saved, key = { it.id }) { batch ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(batch.id in state.compareIds, { vm.toggleCompare(batch.id) })
                        Column(Modifier.weight(1f).clickable { vm.openBatch(batch) }) {
                            Text(batch.name, fontWeight = FontWeight.Bold)
                            Text(DateFormat.getDateTimeInstance().format(Date(batch.createdAt)), style = MaterialTheme.typography.bodySmall)
                            Text("Confidence ${(batch.overallConfidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.delete(batch.id) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareScreen(batches: List<BatchResult>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Notice("Side-by-side visual estimates. Differences may reflect lighting, angle coverage, masking, and material assignment as well as the samples themselves.")
        Material.entries.forEach { material ->
            SectionCard(material.label) {
                batches.forEach { batch ->
                    val estimate = batch.estimates.first { it.material == material }
                    Text("${batch.name}: ${(estimate.visibleShareLow * 100).oneDecimal()}-${(estimate.visibleShareHigh * 100).oneDecimal()}%; ${GoldViewModel.range(estimate.weightLowG, estimate.weightHighG, "g")}")
                }
            }
        }
    }
}

@Composable
private fun SafetyScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Notice("Treat every result as a visual estimate. Use a qualified laboratory assay when identification or value matters.")
        SectionCard("Safer field workflow") { Text("Keep the sample dry, use gloves and eye protection when handling dusty material, work with ventilation, and wash hands afterward. Avoid breathing fine dust.") }
        SectionCard("Recovery guidance") { Text("Use mechanical classification and water-based gravity methods such as careful panning or commercially designed gravity equipment. Follow equipment instructions and local water, land-access, and disposal rules.") }
        SectionCard("Specimens") { Text("A natural specimen may be worth more intact than its recoverable metal. Photograph and document it before altering it, and consult a reputable mineral dealer or qualified assayer for consequential decisions.") }
        SectionCard("Look-alikes") { Text("Pyrite, mica, iron staining, brass-colored minerals, wet highlights, and glare can all appear gold-colored in photographs. Re-capture under diffuse light and assign ambiguous clusters to Unknown.") }
    }
}

@Composable
private fun Notice(text: String) { Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) { Text(text, Modifier.padding(12.dp)) } }

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); content() } } }

private fun titleFor(screen: Screen) = when (screen) { Screen.HOME -> "Maxxed Gold Estimator"; Screen.CAMERA -> "Guided capture"; Screen.REFERENCE -> "Scale reference"; Screen.CORRECT -> "Review analysis"; Screen.RESULTS -> "Visual estimate"; Screen.HISTORY -> "Saved batches"; Screen.COMPARE -> "Compare batches"; Screen.SAFETY -> "Safe guidance" }
private fun numeric(value: String) = value.filter { it.isDigit() || it == '.' }.take(12)
private fun safeName(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "gold-batch" }
private fun copy(context: Context, text: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Gold visual estimate", text)) }
