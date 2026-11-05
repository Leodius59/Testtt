package fr.surveypilot.app.logic

import fr.surveypilot.app.data.ProfileFact
import java.text.Normalizer

/**
 * Offline and explainable advisor. It only reuses facts explicitly saved by
 * the user. It never guesses missing personal information or submits answers.
 */
object LocalAnswerAdvisor {
    data class Advice(
        val answer: String?,
        val explanation: String,
        val confidence: String,
        val matchedQuestion: String? = null
    )

    private val stopWords = setOf(
        "quel", "quelle", "quels", "quelles", "est", "sont", "etes", "avez",
        "vous", "votre", "vos", "des", "dans", "pour", "avec", "une", "les",
        "sur", "qui", "que", "quoi", "comment", "combien", "depuis", "faites",
        "fait", "cela", "ceci", "aux", "par", "pas", "plus", "moins", "mon", "ma", "mes"
    )

    fun advise(question: String, facts: List<ProfileFact>): Advice {
        val normalizedQuestion = normalize(question)
        if (normalizedQuestion.isBlank()) {
            return Advice(null, "Saisis une question pour commencer.", "—")
        }

        val questionTokens = tokens(normalizedQuestion)
        if (questionTokens.isEmpty()) {
            return Advice(null, "La question est trop courte pour être rapprochée du profil.", "Aucune")
        }

        val candidates = facts
            .filter { it.confirmedByUser && it.answer.isNotBlank() }
            .map { fact -> fact to similarity(questionTokens, tokens(normalize(fact.question + " " + fact.category)), normalizedQuestion, normalize(fact.question)) }
            .sortedByDescending { it.second }

        val best = candidates.firstOrNull()
            ?: return Advice(null, "Aucune information confirmée n'est enregistrée dans ton profil.", "Aucune")

        val (fact, score) = best
        // Conservative threshold: avoid presenting a weak textual match as a fact.
        if (score < 0.28) {
            return Advice(null, "Je ne trouve pas de correspondance suffisamment fiable. Réponds toi-même ou ajoute l'information exacte à ton profil.", "Aucune")
        }

        val confidence = when {
            score >= 0.72 -> "Élevée"
            score >= 0.45 -> "Moyenne"
            else -> "Faible — vérification indispensable"
        }
        return Advice(
            answer = fact.answer,
            explanation = "Cette proposition vient de ton profil enregistré (« ${fact.question} »). Une ressemblance de formulation ne garantit pas que les deux questions demandent exactement la même chose.",
            confidence = confidence,
            matchedQuestion = fact.question
        )
    }

    private fun similarity(a: Set<String>, b: Set<String>, fullQuestion: String, storedQuestion: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val recall = intersection / a.size
        val precision = intersection / b.size
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
        val phraseBonus = if (storedQuestion.length >= 8 && fullQuestion.contains(storedQuestion)) 0.35 else 0.0
        return (f1 + phraseBonus).coerceAtMost(1.0)
    }

    private fun tokens(value: String): Set<String> = value.split(" ")
        .filter { it.length > 2 && it !in stopWords }
        .toSet()

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ").trim()
}
