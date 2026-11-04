package dev.idex.android.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class PokemonStat(val name: String, val value: Int)

@Serializable
data class Pokemon(
    val id: Int,
    val englishName: String,
    val chineseName: String,
    val japaneseName: String,
    val types: List<String>,
    val height: Double,
    val weight: Double,
    val englishDescription: String,
    val chineseDescription: String,
    val japaneseDescription: String,
    val englishGenus: String,
    val chineseGenus: String,
    val japaneseGenus: String,
    val stats: List<PokemonStat>,
) {
    val number: String get() = "%03d".format(Locale.US, id)

    fun name(language: AppLanguage): String = language.text(englishName, chineseName, japaneseName)

    fun description(language: AppLanguage): String =
        language.text(englishDescription, chineseDescription, japaneseDescription)

    fun genus(language: AppLanguage): String = language.text(englishGenus, chineseGenus, japaneseGenus)

    fun typeNames(language: AppLanguage): List<String> = types.map { typeName(it, language) }

    fun spokenEntry(language: AppLanguage): String = when (language) {
        AppLanguage.ZhHans ->
            "$chineseName。全国图鉴编号$id。$chineseGenus。属性，${typeNames(language).joinToString("，")}。$chineseDescription"
        AppLanguage.Ja ->
            "$japaneseName。全国図鑑番号$id。$japaneseGenus。${typeNames(language).joinToString("、")}タイプ。$japaneseDescription"
        AppLanguage.En ->
            "$englishName. Pokédex number $id. $englishGenus. ${typeNames(language).joinToString(" and ")} type. $englishDescription"
    }
}

class PokemonCatalog(val entries: List<Pokemon>) {
    private val byId = entries.associateBy { it.id }

    fun pokemon(id: Int): Pokemon? = byId[id]

    fun search(query: String, language: AppLanguage): List<Pokemon> {
        val value = query.trim()
        if (value.isEmpty()) return entries
        val numberQuery = value.trim('#')
        val numeric = numberQuery.toIntOrNull()
        return entries.filter { pokemon ->
            pokemon.englishName.contains(value, ignoreCase = true) ||
                pokemon.chineseName.contains(value) ||
                pokemon.japaneseName.contains(value) ||
                pokemon.number.contains(numberQuery) ||
                (numeric != null && pokemon.id == numeric) ||
                pokemon.typeNames(language).any { it.contains(value, ignoreCase = true) }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): PokemonCatalog {
            val decoded = json.decodeFromString<List<Pokemon>>(text).sortedBy { it.id }
            return PokemonCatalog(decoded)
        }
    }
}
