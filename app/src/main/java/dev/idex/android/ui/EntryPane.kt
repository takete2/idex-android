package dev.idex.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idex.android.data.Pokemon
import dev.idex.android.data.statTitle
import dev.idex.android.ui.sprites.SpriteStore
import dev.idex.android.ui.theme.DexColors
import java.util.Locale

@Composable
fun EntryPane(
    state: DexUiState,
    pokemon: Pokemon?,
    sprites: SpriteStore,
    showClose: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRead: () -> Unit,
    onStop: () -> Unit,
    onToggleStats: () -> Unit,
    onCatalog: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = state.language
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Screw()
            Spacer(Modifier.width(8.dp))
            PixelText(
                language.text("POKÉMON RESEARCH", "宝可梦研究终端", "ポケモンリサーチ"),
                language,
                8,
                color = DexColors.Cream.copy(0.9f),
                modifier = Modifier.weight(1f),
            )
            Indicator(Color(0xFF3DDC84), lit = true)
            Spacer(Modifier.width(8.dp))
            Screw()
        }
        Spacer(Modifier.height(12.dp))
        if (pokemon != null) {
            val status = when {
                state.identified -> language.text("IDENTIFIED", "已识别", "識別完了")
                state.confidence != null -> language.text("BEST MATCH", "最可能匹配", "最有力候補")
                else -> language.text("ARCHIVE", "档案", "図鑑")
            }
            LcdPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelText("No. ${pokemon.number}", language, 10, color = DexColors.Ink)
                        Spacer(Modifier.weight(1f))
                        PixelText(status, language, 7, color = DexColors.Ink)
                    }
                    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(DexColors.Ink.copy(0.25f)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                            CanvasRing()
                            SpriteImage(sprites, pokemon.id, null, Modifier.size(110.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            PixelText(pokemon.name(language), language, 12, color = DexColors.Ink)
                            Text(pokemon.genus(language), color = DexColors.Ink, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                pokemon.typeNames(language).forEach { type ->
                                    PixelText(
                                        type,
                                        language,
                                        6,
                                        color = DexColors.Ink,
                                        modifier = Modifier
                                            .border(1.dp, DexColors.Ink.copy(0.6f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric(language.text("HEIGHT", "身高", "たかさ"), "%.1f m".format(Locale.US, pokemon.height), language)
                        Metric(language.text("WEIGHT", "体重", "おもさ"), "%.1f kg".format(Locale.US, pokemon.weight), language)
                        Metric(language.text("SEEN", "发现", "発見"), if (state.seenIds.contains(pokemon.id)) "●" else "—", language)
                    }
                    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(DexColors.Ink.copy(0.25f)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelText(
                            if (state.showStats) language.text("BASE STATS", "基础能力", "種族値")
                            else language.text("FIELD NOTES", "观察记录", "図鑑説明"),
                            language,
                            8,
                            color = DexColors.Ink,
                            modifier = Modifier.weight(1f),
                        )
                        HardwareButton(onClick = onToggleStats, color = DexColors.LcdDeep) {
                            Icon(
                                if (state.showStats) Icons.AutoMirrored.Filled.Notes else Icons.Filled.BarChart,
                                language.text("Toggle base stats", "切换基础能力", "種族値の表示を切り替える"),
                                tint = DexColors.Ink,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.showStats) {
                        pokemon.stats.forEach { stat ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    statTitle(stat.name, language),
                                    modifier = Modifier.width(72.dp),
                                    color = DexColors.Ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                )
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(DexColors.Ink.copy(0.12f)),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = (stat.value / 255f).coerceIn(0f, 1f))
                                            .background(DexColors.Ink.copy(0.75f)),
                                    )
                                }
                                Text(
                                    stat.value.toString(),
                                    modifier = Modifier.width(32.dp).padding(start = 6.dp),
                                    color = DexColors.Ink,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    } else {
                        PixelText(
                            pokemon.description(language),
                            language,
                            8,
                            color = DexColors.Ink,
                            maxLines = 8,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HardwareButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, language.text("Previous Pokémon", "上一个宝可梦", "前のポケモン"), tint = DexColors.Cream)
                }
                HardwareButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, language.text("Next Pokémon", "下一个宝可梦", "次のポケモン"), tint = DexColors.Cream)
                }
                Spacer(Modifier.weight(1f))
                HardwareButton(onClick = onRead) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = DexColors.Cream)
                    Spacer(Modifier.width(6.dp))
                    PixelText(language.text("READ", "朗读", "読み上げ"), language, 8, color = DexColors.Cream)
                }
                HardwareButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, language.text("Stop reading", "停止朗读", "読み上げを停止"), tint = DexColors.Cream)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clickableNoRipple(onCatalog), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GridView, null, tint = DexColors.Cream)
                    Spacer(Modifier.width(8.dp))
                    PixelText(language.text("BROWSE 151", "浏览151种", "全151種を見る"), language, 8, color = DexColors.Cream)
                }
                Spacer(Modifier.weight(1f))
                PixelText(
                    state.confidence?.let { "${(it * 100).toInt()}%" } ?: language.text("GEN. I", "第一世代", "第1世代"),
                    language,
                    7,
                    color = Color.White.copy(0.5f),
                )
                if (showClose) {
                    Spacer(Modifier.width(8.dp))
                    HardwareButton(onClick = onClose) {
                        PixelText(language.text("CLOSE", "关闭", "閉じる"), language, 8, color = DexColors.Cream)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Screw()
            PixelText(
                language.text(
                    "SILPH CO. · PERSONAL FIELD SYSTEM",
                    "西尔佛公司 · 个人野外系统",
                    "シルフカンパニー · 個人用フィールドシステム",
                ),
                language,
                5,
                color = Color.Black.copy(0.45f),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                maxLines = 2,
            )
            Screw()
        }
    }
}

@Composable
private fun Metric(title: String, value: String, language: dev.idex.android.data.AppLanguage) {
    Column {
        PixelText(title, language, 6, color = DexColors.Ink.copy(0.65f))
        Spacer(Modifier.height(4.dp))
        PixelText(value, language, 8, color = DexColors.Ink)
    }
}

@Composable
private fun CanvasRing() {
    Box(
        Modifier
            .size(96.dp)
            .border(1.dp, DexColors.Ink.copy(0.15f), CircleShape),
    )
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
