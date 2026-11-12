package com.nexia.ai

import android.os.Bundle
import com.nexia.ai.ai.CloudAiEngine
import com.nexia.ai.ai.HybridAiManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.nexia.ai.google.GoogleAuthorization
import com.nexia.ai.google.GoogleFeature
import com.nexia.ai.google.GoogleApi
import java.util.concurrent.Executors
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101323)
private val Surface = Color(0xFF20243C)
private val Accent = Color(0xFFB8B9FF)
private val Muted = Color(0xFFB9BDD3)

class MainActivity : ComponentActivity() {
    private lateinit var googleAuth: GoogleAuthorization
    private var pendingFeature: GoogleFeature? = null
    private var googleStatus by mutableStateOf("Sin conectar")
    private var drivePreview by mutableStateOf("")
    // In-memory only: never save access tokens in preferences, logs, or chat history.
    private var driveToken: String? = null
    private val io = Executors.newSingleThreadExecutor()
    private val aiManager = HybridAiManager(CloudAiEngine(BuildConfig.AI_BACKEND_URL))
    private var aiReply by mutableStateOf("")
    private var aiBusy by mutableStateOf(false)
    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val token = googleAuth.resultFromIntent(result.data).accessToken
                if (token.isNullOrBlank()) googleStatus = "Google no devolvió un token"
                else {
                    if (pendingFeature == GoogleFeature.DRIVE) driveToken = token
                    googleStatus = "Autorizado: ${pendingFeature?.label ?: "Google"}"
                }
            } catch (e: Exception) { googleStatus = "No se pudo completar la autorización" }
        } else googleStatus = "Autorización cancelada"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuth = GoogleAuthorization(this)
        setContent { MaterialTheme {
            NexiaApp(googleStatus, drivePreview, aiReply, aiBusy,
                onAskAi = { prompt ->
                    aiBusy = true
                    io.execute {
                        val reply = try { aiManager.answer(prompt) } catch (_: Exception) { "Error al consultar la IA" }
                        runOnUiThread { aiReply = reply; aiBusy = false }
                    }
                },
                onAuthorize = { feature ->
                    pendingFeature = feature
                    googleAuth.request(feature,
                        onResolution = { pending ->
                            try { consentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build()) }
                            catch (e: Exception) { googleStatus = "No se pudo abrir Google" }
                        },
                        onToken = { token ->
                            if (feature == GoogleFeature.DRIVE) driveToken = token
                            googleStatus = "Autorizado: ${feature.label}"
                        },
                        onError = { googleStatus = it }
                    )
                },
                onDriveTest = {
                    val token = driveToken
                    if (token == null) drivePreview = "Primero autoriza Drive"
                    else io.execute {
                        val output = try { GoogleApi.listDriveMetadata(token) }
                                     catch (e: Exception) { "Error de conexión" }
                        runOnUiThread { drivePreview = output.take(1800) }
                    }
                },
                onDisconnect = {
                    googleAuth.revoke(onDone = {
                        driveToken = null
                        googleStatus = "Acceso revocado"
                        drivePreview = ""
                    }, onError = { googleStatus = it })
                }
            )
        } }
    }

    override fun onDestroy() { io.shutdown(); super.onDestroy() }
}

@Composable
fun NexiaApp(
    googleStatus: String = "Sin conectar",
    drivePreview: String = "",
    aiReply: String = "",
    aiBusy: Boolean = false,
    onAskAi: (String) -> Unit = {},
    onAuthorize: (GoogleFeature) -> Unit = {},
    onDriveTest: () -> Unit = {},
    onDisconnect: () -> Unit = {}
) {
    var section by remember { mutableStateOf("Inicio") }
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    LaunchedEffect(aiReply) { if (aiReply.isNotBlank()) messages.add("NEXIA" to aiReply) }
    val features = listOf(
        "Chat inteligente" to "Chat",
        "IA sin internet" to "Local",
        "Asistente de voz" to "Voz",
        "Crear imágenes" to "Imágenes",
        "Analizar documentos" to "Documentos",
        "Historial" to "Historial",
        "Google Hub" to "Google"
    )
    Column(Modifier.fillMaxSize().background(Background).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦ NEXIA AI", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("● Híbrida", color = Color(0xFF88E5BD), fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        if (section == "Inicio") {
            Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("¡Hola! ¿En qué puedo ayudarte?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Pregunta, crea, aprende y explora con inteligencia artificial.", color = Muted)
            }
            Spacer(Modifier.height(20.dp))
            features.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    pair.forEach { (label, destination) ->
                        Column(Modifier.weight(1f).height(104.dp).background(Surface, RoundedCornerShape(16.dp)).clickable { section = destination }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text("✦", color = Accent, fontSize = 24.sp)
                            Text(label, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { section = "Chat" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Abrir chat", color = Background)
            }
        } else {
            TextButton(onClick = { section = "Inicio" }) { Text("← Volver", color = Accent) }
            Text(section, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (section == "Google") {
                Text("Estado: $googleStatus", color = Muted)
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GoogleFeature.entries.toList()) { feature ->
                        OutlinedButton(onClick = { onAuthorize(feature) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Autorizar ${feature.label}")
                        }
                    }
                    item {
                        Button(onClick = onDriveTest, modifier = Modifier.fillMaxWidth()) {
                            Text("Probar lectura de Drive")
                        }
                        if (drivePreview.isNotBlank()) Text(drivePreview, color = Muted)
                        TextButton(onClick = onDisconnect) { Text("Revocar acceso de NEXIA") }
                    }
                }
            } else if (section == "Chat") {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages.toList()) { (role, body) ->
                        Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                            Text(role, color = Accent, fontWeight = FontWeight.Bold)
                            Text(body, color = Color.White)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Escribe tu mensaje", color = Muted) }, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White), singleLine = true)
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = !aiBusy, onClick = {
                        val message = input.trim()
                        if (message.isNotEmpty()) {
                            messages.add("Tú" to message)
                            onAskAi(message)
                            input = ""
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text(if (aiBusy) "..." else "Enviar", color = Background) }
                }
            } else {
                Text("Módulo preparado para la siguiente fase de desarrollo.", color = Muted)
                Text("Todavía no hay un modelo de IA conectado.", color = Muted)
            }
        }
    }
}
