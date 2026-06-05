package com.flame.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flame.recorder.data.ai.AiManager
import com.flame.recorder.data.local.RecordingDatabase
import com.flame.recorder.data.model.Recording
import com.flame.recorder.data.preference.AppPreferences
import com.flame.recorder.service.AudioRecordingService
import com.flame.recorder.ui.theme.FlameRecorderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

class MainViewModel(private val context: Context) : ViewModel() {
    private val db = RecordingDatabase.getDatabase(context)
    val dao = db.recordingDao()
    val preferences = AppPreferences(context)
    private val aiManager = AiManager(preferences)

    val savedRecordings = dao.getAllSavedRecordings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val temporaryRecordings = dao.getTemporaryRecordings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recycledRecordings = dao.getRecycledRecordings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordingState = AudioRecordingService.recordingState
    val elapsedTime = AudioRecordingService.elapsedTime
    val amplitude = AudioRecordingService.amplitude

    // Stable backing properties for native Kotlin get/set syntax
    private val _isAiProcessing = mutableStateOf(false)
    var isAiProcessing: Boolean
        get() = _isAiProcessing.value
        set(newValue) { _isAiProcessing.value = newValue }

    var mediaPlayer: MediaPlayer? = null

    private val _currentlyPlayingId = mutableStateOf<Long?>(null)
    var currentlyPlayingId: Long?
        get() = _currentlyPlayingId.value
        set(newValue) { _currentlyPlayingId.value = newValue }

    private val _isPlaying = mutableStateOf(false)
    var isPlaying: Boolean
        get() = _isPlaying.value
        set(newValue) { _isPlaying.value = newValue }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.purgeOldRecycled(System.currentTimeMillis(), 30L * 24 * 60 * 60 * 1000L)
        }
    }

    fun startRecording() {
        val intent = Intent(context, AudioRecordingService::class.java).apply { action = "START" }
        context.startService(intent)
    }

    fun pauseRecording() {
        val intent = Intent(context, AudioRecordingService::class.java).apply { action = "PAUSE" }
        context.startService(intent)
    }

    fun resumeRecording() {
        val intent = Intent(context, AudioRecordingService::class.java).apply { action = "RESUME" }
        context.startService(intent)
    }

    fun stopRecording() {
        val intent = Intent(context, AudioRecordingService::class.java).apply { action = "STOP" }
        context.startService(intent)
    }

    fun saveDraft(recording: Recording, newName: String, aiSummary: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(recording.filePath)
            if (tempFile.exists()) {
                val finalName = newName.ifBlank { recording.name }
                val extension = tempFile.extension
                var savedPath = tempFile.absolutePath

                val uriString = preferences.storageDirectoryUri
                if (uriString.isNotBlank()) {
                    try {
                        val treeUri = Uri.parse(uriString)
                        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
                        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        val fileUri = DocumentsContract.createDocument(
                            context.contentResolver,
                            parentDocumentUri,
                            "audio/*",
                            "$finalName.$extension"
                        )
                        fileUri?.let { destUri ->
                            context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                                tempFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            tempFile.delete()
                            savedPath = destUri.toString()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        savedPath = saveToInternal(tempFile, finalName, extension)
                    }
                } else {
                    savedPath = saveToInternal(tempFile, finalName, extension)
                }

                val updated = recording.copy(
                    name = finalName,
                    filePath = savedPath,
                    isTemporary = false,
                    summary = aiSummary
                )
                dao.updateRecording(updated)
            }
        }
    }

    private fun saveToInternal(tempFile: File, finalName: String, extension: String): String {
        val permanentDir = File(context.filesDir, "recordings")
        if (!permanentDir.exists()) permanentDir.mkdirs()
        val destFile = File(permanentDir, "Rec_${System.currentTimeMillis()}.$extension")
        tempFile.copyTo(destFile, overwrite = true)
        tempFile.delete()
        return destFile.absolutePath
    }

    fun discardDraft(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(recording.filePath)
            if (file.exists()) file.delete()
            dao.deleteRecording(recording)
        }
    }

    fun runAiSummary(recording: Recording, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            this@MainViewModel.isAiProcessing = true
            try {
                val summary = if (recording.filePath.startsWith("content://")) {
                    val tempCacheFile = File(context.cacheDir, "temp_summary_audio")
                    context.contentResolver.openInputStream(Uri.parse(recording.filePath))?.use { inputStream ->
                        tempCacheFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val result = aiManager.transcribeAndSummarize(tempCacheFile)
                    tempCacheFile.delete()
                    result
                } else {
                    val file = File(recording.filePath)
                    aiManager.transcribeAndSummarize(file)
                }

                withContext(Dispatchers.Main) {
                    onComplete(summary)
                    val updated = recording.copy(summary = summary)
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.updateRecording(updated)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            } finally {
                this@MainViewModel.isAiProcessing = false
            }
        }
    }

    fun playAudio(recording: Recording) {
        if (currentlyPlayingId == recording.id) {
            if (isPlaying) {
                mediaPlayer?.pause()
                this@MainViewModel.isPlaying = false
            } else {
                mediaPlayer?.start()
                this@MainViewModel.isPlaying = true
            }
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            if (recording.filePath.startsWith("content://")) {
                setDataSource(context, Uri.parse(recording.filePath))
            } else {
                setDataSource(recording.filePath)
            }
            prepare()
            start()
            setOnCompletionListener {
                this@MainViewModel.currentlyPlayingId = null
                this@MainViewModel.isPlaying = false
            }
        }
        this@MainViewModel.currentlyPlayingId = recording.id
        this@MainViewModel.isPlaying = true
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        this@MainViewModel.currentlyPlayingId = null
        this@MainViewModel.isPlaying = false
    }

    fun moveToTrash(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashed = recording.copy(isDeleted = true, deletedTimestamp = System.currentTimeMillis())
            dao.updateRecording(trashed)
        }
    }

    fun restoreFromTrash(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            val restored = recording.copy(isDeleted = false, deletedTimestamp = 0L)
            dao.updateRecording(restored)
        }
    }

    fun deletePermanently(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            if (recording.filePath.startsWith("content://")) {
                try {
                    val fileUri = Uri.parse(recording.filePath)
                    DocumentsContract.deleteDocument(context.contentResolver, fileUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val file = File(recording.filePath)
                if (file.exists()) file.delete()
            }
            dao.deleteRecording(recording)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlameRecorderTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel {
        MainViewModel(context.applicationContext)
    }
    var activeTab by remember { mutableIntStateOf(0) }

    val permissions = remember {
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var permissionsGranted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(permissions)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                    label = { Text("Record") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                    label = { Text("List") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Trash") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!permissionsGranted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Mic and Notification permissions are required.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                when (activeTab) {
                    0 -> RecordTab(viewModel)
                    1 -> ListTab(viewModel)
                    2 -> TrashTab(viewModel)
                    3 -> SettingsTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun RecordTab(viewModel: MainViewModel) {
    val state by viewModel.recordingState.collectAsState()
    val time by viewModel.elapsedTime.collectAsState()
    val amp by viewModel.amplitude.collectAsState()
    val drafts by viewModel.temporaryRecordings.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val targetAmp = if (state is AudioRecordingService.RecordingState.Recording) amp else 0f
    val animatedAmp by animateFloatAsState(
        targetValue = targetAmp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    var maxSeenAmp by remember { mutableFloatStateOf(1000f) }
    LaunchedEffect(amp) {
        if (amp > maxSeenAmp) {
            maxSeenAmp = amp
        }
    }
    LaunchedEffect(state) {
        if (state is AudioRecordingService.RecordingState.Idle) {
            maxSeenAmp = 1000f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (state) {
                is AudioRecordingService.RecordingState.Recording -> "Recording Active"
                is AudioRecordingService.RecordingState.Paused -> "Recording Paused"
                else -> "Ready to Record"
            },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = formatDuration(time),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2

                if (state is AudioRecordingService.RecordingState.Recording) {
                    val normalizedAmp = (animatedAmp / maxSeenAmp).coerceIn(0.05f, 1f)
                    val barCount = 30
                    val barWidth = width / (barCount * 1.5f)
                    for (i in 0 until barCount) {
                        val waveScale = sin(i.toFloat() / barCount * Math.PI).toFloat()
                        val rippleScale = 0.4f + 0.6f * sin(i * 0.4f - idlePhase * 1.5f)
                        val barHeight = (height * 0.8f) * normalizedAmp * waveScale * rippleScale
                        val x = i * (barWidth * 1.5f) + barWidth
                        drawLine(
                            color = primaryColor,
                            start = Offset(x, midY - barHeight / 2),
                            end = Offset(x, midY + barHeight / 2),
                            strokeWidth = barWidth
                        )
                    }
                } else {
                    for (waveIndex in 0..2) {
                        val amplitude = 25f / (waveIndex + 1)
                        val frequency = 0.015f * (waveIndex + 1)
                        var prevX = 0f
                        var prevY = midY
                        for (x in 0..width.toInt() step 5) {
                            val y = midY + sin(x * frequency + idlePhase + (waveIndex * 1.5f)) * amplitude
                            drawLine(
                                color = primaryColor.copy(alpha = 1f / (waveIndex + 1)),
                                start = Offset(prevX, prevY),
                                end = Offset(x.toFloat(), y),
                                strokeWidth = if (waveIndex == 0) 4f else 2f
                            )
                            prevX = x.toFloat()
                            prevY = y
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state != AudioRecordingService.RecordingState.Idle) {
                IconButton(
                    onClick = {
                        if (state is AudioRecordingService.RecordingState.Recording) viewModel.pauseRecording()
                        else viewModel.resumeRecording()
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (state is AudioRecordingService.RecordingState.Recording) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Button(
                    onClick = { viewModel.stopRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(36.dp))
                }
            } else {
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(36.dp))
                }
            }
        }

        if (drafts.isNotEmpty()) {
            TemporaryReviewDialog(drafts.first(), viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporaryReviewDialog(recording: Recording, viewModel: MainViewModel) {
    var title by remember { mutableStateOf(recording.name) }
    var summaryText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(onClick = { viewModel.saveDraft(recording, title, summaryText) }) {
                Text("Save Recording")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.discardDraft(recording) }) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        title = { Text("Unsaved Recording Draft") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Recording Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.runAiSummary(recording,
                            onComplete = { summaryText = it },
                            onError = { errorText = it }
                        )
                    },
                    enabled = !viewModel.isAiProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isAiProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Summarize with AI")
                    }
                }

                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                summaryText?.let {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("AI Summary Preview:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn {
                            item { Text(it, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ListTab(viewModel: MainViewModel) {
    val items by viewModel.savedRecordings.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recordings saved.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(items, key = { it.id }) { rec ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = rec.name,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatDuration(rec.durationMs)} | ${File(rec.filePath).name.substringAfterLast(".", "AUDIO").uppercase()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { viewModel.playAudio(rec) }) {
                                    Icon(
                                        imageVector = if (viewModel.currentlyPlayingId == rec.id && viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = { viewModel.moveToTrash(rec) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                val pathForDisplay = if (rec.filePath.startsWith("content://")) {
                                    Uri.parse(rec.filePath).path ?: rec.filePath
                                } else {
                                    rec.filePath
                                }
                                Text("Storage Path:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = pathForDisplay, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Summary:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))

                                if (rec.summary.isNullOrBlank()) {
                                    Button(
                                        onClick = {
                                            viewModel.runAiSummary(rec,
                                                onComplete = { /* Auto flow refresh */ },
                                                onError = { /* Error feedback handled */ }
                                            )
                                        },
                                        enabled = !viewModel.isAiProcessing,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        if (viewModel.isAiProcessing) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Summarize with AI", fontSize = 14.sp)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = rec.summary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { shareAudioFile(context, rec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Recording", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrashTab(viewModel: MainViewModel) {
    val items by viewModel.recycledRecordings.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle Bin is empty.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(items, key = { it.id }) { rec ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = rec.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Will be cleared in 30 days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.restoreFromTrash(rec) }) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.deletePermanently(rec) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp) // Enlarged icon size for better touch targeting
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf(viewModel.preferences.aiPrompt) }

    var quality by remember { mutableStateOf(viewModel.preferences.audioQuality) }
    var format by remember { mutableStateOf(viewModel.preferences.audioFormat) }
    var storageUriStr by remember { mutableStateOf(viewModel.preferences.storageDirectoryUri) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.preferences.storageDirectoryUri = it.toString()
            storageUriStr = it.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Audio Parameters", fontWeight = FontWeight.Bold, fontSize = 18.sp) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Quality Settings")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { q ->
                        FilterChip(
                            selected = quality == q,
                            onClick = {
                                quality = q
                                viewModel.preferences.audioQuality = q
                            },
                            label = { Text(q) }
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Audio Format")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("M4A", "3GP", "WAV", "MP3").forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = {
                                format = f
                                viewModel.preferences.audioFormat = f
                            },
                            label = { Text(f) }
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage Directory", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                val displayPath = if (storageUriStr.isBlank()) {
                    "Default Internal App Storage"
                } else {
                    Uri.parse(storageUriStr).path ?: storageUriStr
                }
                Text(
                    text = "Current Path: $displayPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Custom Directory")
                }
                if (storageUriStr.isNotBlank()) {
                    TextButton(
                        onClick = {
                            viewModel.preferences.storageDirectoryUri = ""
                            storageUriStr = ""
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to Default", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("AI Custom Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp) }

        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    viewModel.preferences.aiPrompt = it
                },
                label = { Text("Summary Instruction Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun shareAudioFile(context: Context, recording: Recording) {
    try {
        val uri: Uri = if (recording.filePath.startsWith("content://")) {
            Uri.parse(recording.filePath)
        } else {
            val file = File(recording.filePath)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Audio via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}