package com.example.kucharz2.data

import java.text.Normalizer

object IngredientQueryExpander {
    private const val PASTA_GROUP_TRIGGER = "makaron"

    private val pastaGroup = listOf(
        "makaron penne",
        "spaghetti",
        "fusilli",
        "makaron wstążki",
        "makaron kokardki",
        "makaron ryżowy",
        "makaron muszelki",
        "lasagne",
        "tortellini",
        "łazanki",
        "makaron kolanka",
        "groszek ptysiowy",
        "orzo",
        "makaron chiński",
        "zupki chińskie",
        "cannelloni",
        "tortelini z mięsem",
        "makaron sojowy",
        "soba",
        "makaron jajeczny",
        "ravioli",
        "gnocchi",
        "makaron czekoladowy",
        "udon",
        "kluski",
        "zacierki",
        "wonton",
        "makaron bezglutenowy",
        "shirataki"
    )

    fun expandForApiQuery(ingredients: List<String>): List<String> {
        val hasGenericPasta = ingredients.any { it.normalizedIngredientKey() == PASTA_GROUP_TRIGGER }
        if (!hasGenericPasta) return ingredients

        return (ingredients + pastaGroup).distinctBy { it.normalizedIngredientKey() }
    }

    private fun String.normalizedIngredientKey(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9ąćęłńóśźż ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
