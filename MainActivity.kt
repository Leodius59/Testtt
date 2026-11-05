package fr.surveypilot.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import fr.surveypilot.app.data.ProfileFact
import fr.surveypilot.app.data.SurveyDatabase
import fr.surveypilot.app.logic.LocalAnswerAdvisor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = SurveyDatabase.get(this).profileDao()
        setContent {
            MaterialTheme {
                val facts by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
                SurveyPilotScreen(
                    facts = facts,
                    onSave = { category, question, answer ->
                        lifecycleScope.launch { dao.insert(ProfileFact(category = category, question = question, answer = answer)) }
                    },
                    onDelete = { id -> lifecycleScope.launch { dao.delete(id) } },
                    onClear = { lifecycleScope.launch { dao.deleteAll() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyPilotScreen(
    facts: List<ProfileFact>,
    onSave: (String, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("Profil") }
    var storedQuestion by remember { mutableStateOf("") }
    var storedAnswer by remember { mutableStateOf("") }
    var surveyQuestion by remember { mutableStateOf("") }
    var advice by remember { mutableStateOf<LocalAnswerAdvisor.Advice?>(null) }
    var ocrStatus by remember { mutableStateOf("Choisis une capture d'écran d'un sondage AttaPoll pour en extraire le texte.") }
    var isReading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isReading = true
            ocrStatus = "Lecture de l'image en cours…"
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val extracted = result.text.trim()
                        surveyQuestion = extracted
                        advice = null
                        ocrStatus = if (extracted.isBlank()) {
                            "Aucun texte détecté. Essaie une capture plus nette et recadrée."
                        } else {
                            "Texte détecté (${extracted.length} caractères). Vérifie-le avant analyse."
                        }
                        isReading = false
                        recognizer.close()
                    }
                    .addOnFailureListener { error ->
                        ocrStatus = "Échec de lecture : ${error.localizedMessage ?: "image non reconnue"}"
                        isReading = false
                        recognizer.close()
                    }
            } catch (error: Exception) {
                ocrStatus = "Impossible d'ouvrir cette image : ${error.localizedMessage ?: "format non pris en charge"}"
                isReading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("SurveyPilot · assistant hors ligne") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Ton profil local", style = MaterialTheme.typography.titleLarge)
                Text("Enregistre uniquement des informations exactes que tu as confirmées. Elles restent dans la base locale de l'application.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(category, { category = it }, label = { Text("Catégorie (ex. travail, logement)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(storedQuestion, { storedQuestion = it }, label = { Text("Question / information concernée") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(storedAnswer, { storedAnswer = it }, label = { Text("Réponse exacte") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (storedQuestion.isNotBlank() && storedAnswer.isNotBlank()) {
                            onSave(category.ifBlank { "Profil" }, storedQuestion.trim(), storedAnswer.trim())
                            storedQuestion = ""; storedAnswer = ""
                        }
                    },
                    enabled = storedQuestion.isNotBlank() && storedAnswer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enregistrer dans mon profil") }
            }
            item {
                HorizontalDivider()
                Text("Lire un sondage (OCR local)", style = MaterialTheme.typography.titleLarge)
                Text("Fais une capture d'écran dans AttaPoll, puis sélectionne-la ici. La reconnaissance du texte est effectuée sur le téléphone, sans envoi à un serveur.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isReading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isReading) "Lecture en cours…" else "Choisir une capture d'écran") }
                Text(ocrStatus, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Question reconnue", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = surveyQuestion,
                    onValueChange = { surveyQuestion = it; advice = null },
                    label = { Text("Texte extrait ou question saisie") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Button(
                    onClick = { advice = LocalAnswerAdvisor.advise(surveyQuestion, facts) },
                    enabled = surveyQuestion.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Rechercher dans mon profil") }
                advice?.let { result ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Correspondance : ${result.confidence}", style = MaterialTheme.typography.titleMedium)
                            result.matchedQuestion?.let { Text("Information de référence : $it", style = MaterialTheme.typography.bodySmall) }
                            Text(result.answer ?: "Aucune réponse proposée", style = MaterialTheme.typography.bodyLarge)
                            Text(result.explanation, style = MaterialTheme.typography.bodySmall)
                            Text("Vérifie que la réponse correspond exactement à ta situation. SurveyPilot ne remplit ni ne soumet le sondage.", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            item {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Informations enregistrées (${facts.size})", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onClear, enabled = facts.isNotEmpty()) { Text("Tout effacer") }
                }
            }
            items(facts, key = { it.id }) { fact ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(fact.category, style = MaterialTheme.typography.labelMedium)
                        Text(fact.question, style = MaterialTheme.typography.titleSmall)
                        Text(fact.answer)
                        TextButton(onClick = { onDelete(fact.id) }) { Text("Supprimer") }
                    }
                }
            }
            item {
                Text("Confidentialité : profil et analyse conservés localement. OCR embarqué, sans clé API ni permission Internet. L'OCR de cette version lit les captures d'écran choisies, pas l'écran en direct. Aucune réponse n'est envoyée automatiquement.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
