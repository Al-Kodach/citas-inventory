package com.citacomsys.pharmacy.api.model

import kotlinx.serialization.Serializable

@Serializable
enum class TherapeuticClassification(val dbValue: String, val displayName: String) {
    ANALGESICS("Analgesics", "Analgesics & Anti-inflammatories"),
    ANTIBIOTICS("Antibiotics", "Antibiotics & Antimicrobials"),
    CARDIOVASCULAR("Cardiovascular", "Cardiovascular Medications"),
    DIABETES("Diabetes", "Anti-Diabetics"),
    GASTROINTESTINAL("Gastrointestinal", "Gastrointestinal Systems"),
    RESPIRATORY("Respiratory", "Respiratory & Anti-allergy"),
    SUPPLEMENTS("Supplements", "Vitamins & Dietary Supplements");

    companion object {
        fun fromCategory(category: String): TherapeuticClassification? = when (category) {
            "Analgesics" -> ANALGESICS
            "Antibiotics" -> ANTIBIOTICS
            "Cardiovascular" -> CARDIOVASCULAR
            "Anti-Diabetics" -> DIABETES
            "Gastrointestinal" -> GASTROINTESTINAL
            "Respiratory", "Antihistamines" -> RESPIRATORY
            "Supplements" -> SUPPLEMENTS
            else -> null
        }
    }
}
