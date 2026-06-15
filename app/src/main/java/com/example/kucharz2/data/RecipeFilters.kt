package com.example.kucharz2.data

data class RecipeFilters(
    val mainIngredient: String? = null,
    val excludedIngredients: List<String> = emptyList(),
    val mealTypeTag: String? = null,
    val cuisineTag: String? = null,
    val dietTag: String? = null,
    val oneMissingIngredientOnly: Boolean = false,
    val videoOnly: Boolean = false,
    val maxIngredients: Int? = null,
    val minRatingTag: String? = null,
    val readyTimeTag: String? = null
) {
    val hasActiveFilters: Boolean
        get() = mainIngredient != null ||
            excludedIngredients.isNotEmpty() ||
            mealTypeTag != null ||
            cuisineTag != null ||
            dietTag != null ||
            oneMissingIngredientOnly ||
            videoOnly ||
            maxIngredients != null ||
            minRatingTag != null ||
            readyTimeTag != null

    fun categoryNames(): String = listOfNotNull(
        mealTypeTag,
        cuisineTag,
        dietTag,
        readyTimeTag,
        minRatingTag,
        if (videoOnly) "schema_video" else null
    ).joinToString(",")
}

data class FilterOption(
    val label: String,
    val value: String
)

object RecipeFilterOptions {
    val mealTypes = listOf(
        FilterOption("Przekąski", "ptag_appetizer and snacks"),
        FilterOption("Wypieki", "ptag_baked goods"),
        FilterOption("Śniadanie i brunch", "ptag_breakfast and brunch"),
        FilterOption("Deser", "ptag_desserts"),
        FilterOption("Obiad", "ptag_lunch"),
        FilterOption("Główne danie", "ptag_main dish"),
        FilterOption("Sałatka", "ptag_salads"),
        FilterOption("Przystawki", "ptag_side dish"),
        FilterOption("Zupy i gulasze", "ptag_soups and stews"),
        FilterOption("Specjalne okazje", "ptag_special occasions")
    )

    val cuisines = listOf(
        FilterOption("Afrykańska", "ctag_african"),
        FilterOption("Amerykańska", "ctag_american"),
        FilterOption("Azjatycka", "ctag_asian"),
        FilterOption("Australijska", "ctag_australian"),
        FilterOption("Brazylijska", "ctag_brazilian"),
        FilterOption("Karaibska", "ctag_caribbean"),
        FilterOption("Kreolska", "ctag_creole and cajun"),
        FilterOption("Angielska", "ctag_english"),
        FilterOption("Francuska", "ctag_french"),
        FilterOption("Grecka", "ctag_greek"),
        FilterOption("Indyjska", "ctag_indian"),
        FilterOption("Włoska", "ctag_italian"),
        FilterOption("Latynoamerykańska", "ctag_latin american"),
        FilterOption("Libańska", "ctag_lebanese"),
        FilterOption("Śródziemnomorska", "ctag_mediterranean"),
        FilterOption("Bliskowschodnia", "ctag_middle eastern"),
        FilterOption("Rosyjska i ukraińska", "ctag_russian and ukranian"),
        FilterOption("Szkocka", "ctag_scottish"),
        FilterOption("Hiszpańska", "ctag_spanish"),
        FilterOption("Tajska", "ctag_thai"),
        FilterOption("Turecka", "ctag_turkish")
    )

    val diets = listOf(
        FilterOption("Wegańska", "diet_vegan"),
        FilterOption("Wegetariańska", "diet_vegetarian"),
        FilterOption("Bez laktozy", "diet_lactose_free"),
        FilterOption("Bezglutenowa", "diet_gluten_free")
    )

    val readyTimes = listOf(
        FilterOption("Do 15 min", "schema_ready_in_under_15mins"),
        FilterOption("Do 30 min", "schema_ready_in_under_30mins"),
        FilterOption("Do 45 min", "schema_ready_in_under_45mins"),
        FilterOption("Do 1 godziny", "schema_ready_in_under_1hr")
    )

    val ratings = listOf(
        FilterOption("3+ gwiazdki", "schema_3plus_star_rating"),
        FilterOption("4+ gwiazdki", "schema_4plus_star_rating"),
        FilterOption("5 gwiazdek", "schema_5star_rating")
    )

    val maxIngredients = listOf(
        FilterOption("≤ 3", "3"),
        FilterOption("≤ 5", "5"),
        FilterOption("≤ 10", "10")
    )
}
