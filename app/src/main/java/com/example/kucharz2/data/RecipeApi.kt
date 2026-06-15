package com.example.kucharz2.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApi {
    @POST("dyn/results")
    @FormUrlEncoded
    suspend fun getSupercookResults(
        @Field("kitchen") kitchen: String,
        @Field("focus") focus: String = "",
        @Field("exclude") exclude: String = "",
        @Field("kw") keyword: String = "",
        @Field("catname") categoryName: String = "",
        @Field("start") start: Int = 0,
        @Field("limit") limit: Int = 50,
        @Field("fave") favorite: Boolean = false,
        @Field("app") app: Int = 1,
        @Field("needsimage") needsImage: Int = 1,
        @Field("lang") lang: String = "pl",
        @Field("cv") cv: Int = 2
    ): Response<ResponseBody>

    @POST("dyn/details")
    @FormUrlEncoded
    suspend fun getSupercookRecipeDetails(
        @Field("rid") recipeId: String,
        @Field("lang") lang: String = "pl",
        @Field("cv") cv: Int = 2
    ): Response<ResponseBody>

    @GET("recipes")
    suspend fun getRecipes(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ResponseBody>

    @GET("recipes/{recipe_id}")
    suspend fun getRecipe(@Path("recipe_id") recipeId: String): Response<ResponseBody>

    @GET("search")
    suspend fun searchRecipes(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50
    ): Response<ResponseBody>

    @GET("ingredients")
    suspend fun searchByIngredient(
        @Query("ingredient") ingredient: String,
        @Query("limit") limit: Int = 50
    ): Response<ResponseBody>

    @POST("recipes/by-ingredients")
    suspend fun filterByIngredients(
        @Query("limit") limit: Int = 50,
        @Body request: IngredientFilterRequest
    ): Response<ResponseBody>

    @POST("recipes/by-available-ingredients")
    suspend fun recipesByAvailableIngredients(
        @Body request: AvailableIngredientsRequest
    ): Response<ResponseBody>
}
