package dev.idex.android.data

import java.util.Locale

/** Interface language. Catalog names stay searchable in all three at once. */
enum class AppLanguage(val id: String, val title: String, val speechTag: String) {
    En("en", "English", "en-US"),
    ZhHans("zh-Hans", "简体中文", "zh-CN"),
    Ja("ja", "日本語", "ja-JP");

    fun text(english: String, chinese: String, japanese: String): String = when (this) {
        En -> english
        ZhHans -> chinese
        Ja -> japanese
    }

    fun speechLocale(): Locale = when (this) {
        En -> Locale.US
        ZhHans -> Locale.SIMPLIFIED_CHINESE
        Ja -> Locale.JAPAN
    }

    companion object {
        fun fromId(id: String?): AppLanguage? = entries.firstOrNull { it.id == id }

        fun systemDefault(): AppLanguage = when (Locale.getDefault().language) {
            "zh" -> ZhHans
            "ja" -> Ja
            else -> En
        }
    }
}

fun statTitle(name: String, language: AppLanguage): String = when (name) {
    "hp" -> language.text("HP", "体力", "HP")
    "attack" -> language.text("ATTACK", "攻击", "こうげき")
    "defense" -> language.text("DEFENSE", "防御", "ぼうぎょ")
    "special-attack" -> language.text("SP. ATK", "特攻", "とくこう")
    "special-defense" -> language.text("SP. DEF", "特防", "とくぼう")
    "speed" -> language.text("SPEED", "速度", "すばやさ")
    else -> name.uppercase(Locale.US)
}

fun typeName(type: String, language: AppLanguage): String = when (language) {
    AppLanguage.En -> type.uppercase(Locale.US)
    AppLanguage.ZhHans -> CHINESE_TYPES[type] ?: type
    AppLanguage.Ja -> JAPANESE_TYPES[type] ?: type
}

private val CHINESE_TYPES = mapOf(
    "normal" to "一般",
    "fighting" to "格斗",
    "flying" to "飞行",
    "poison" to "毒",
    "ground" to "地面",
    "rock" to "岩石",
    "bug" to "虫",
    "ghost" to "幽灵",
    "steel" to "钢",
    "fire" to "火",
    "water" to "水",
    "grass" to "草",
    "electric" to "电",
    "psychic" to "超能力",
    "ice" to "冰",
    "dragon" to "龙",
    "dark" to "恶",
    "fairy" to "妖精",
)

private val JAPANESE_TYPES = mapOf(
    "normal" to "ノーマル",
    "fighting" to "かくとう",
    "flying" to "ひこう",
    "poison" to "どく",
    "ground" to "じめん",
    "rock" to "いわ",
    "bug" to "むし",
    "ghost" to "ゴースト",
    "steel" to "はがね",
    "fire" to "ほのお",
    "water" to "みず",
    "grass" to "くさ",
    "electric" to "でんき",
    "psychic" to "エスパー",
    "ice" to "こおり",
    "dragon" to "ドラゴン",
    "dark" to "あく",
    "fairy" to "フェアリー",
)
