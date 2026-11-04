package dev.idex.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idex.android.data.AppLanguage
import dev.idex.android.ui.theme.DexColors

@Composable
fun SettingsScreen(
    state: DexUiState,
    onLanguage: (AppLanguage) -> Unit,
    onSound: (Boolean) -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = state.language
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .background(DexColors.Charcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelText(
                language.text("Device settings", "设备设置", "デバイス設定"),
                language,
                10,
                color = DexColors.Cream,
                modifier = Modifier.weight(1f),
            )
            HardwareButton(onClick = onClose) {
                PixelText(language.text("Done", "完成", "完了"), language, 8, color = DexColors.Cream)
            }
        }
        SectionTitle(language.text("Language / 语言", "语言 / Language", "言語 / Language"))
        AppLanguage.entries.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onLanguage(option) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option.title, color = DexColors.Cream, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (option == language) Icon(Icons.Filled.Check, null, tint = DexColors.Lcd)
            }
        }
        SectionTitle(language.text("Sound", "声音", "サウンド"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                language.text("Device sounds & spoken entries", "设备音效与图鉴语音", "効果音と図鑑の読み上げ"),
                color = DexColors.Cream,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
            )
            Switch(
                checked = state.soundEnabled,
                onCheckedChange = onSound,
                colors = SwitchDefaults.colors(checkedTrackColor = DexColors.Lcd, checkedThumbColor = DexColors.Ink),
            )
        }
        HardwareButton(onClick = onStop, modifier = Modifier.padding(top = 8.dp)) {
            PixelText(language.text("Stop reading", "停止朗读", "読み上げを停止"), language, 7, color = DexColors.Cream)
        }
        SectionTitle(language.text("Field guide", "使用指南", "使い方"))
        Guide(language.text("Point at a Pokémon, then identify.", "将取景器对准宝可梦，轻点识别。", "ポケモンにカメラを向けて、スキャンします。"))
        Guide(language.text(
            "Unfold a foldable, or use a wide screen, to keep the archive beside the scanner.",
            "展开折叠屏，或在宽屏上并排查看图鉴与扫描器。",
            "折りたたみ端末を開くか、広い画面では図鑑とスキャナーが並びます。",
        ))
        Guide(language.text(
            "Photos and recognition stay on this device.",
            "照片与识别过程都留在这台设备上。",
            "写真と識別処理は、この端末の中だけで完結します。",
        ))
        Text(
            language.text(
                "The bundled model recognizes the original 151 Kanto Pokémon. Low-confidence scans are not saved as discoveries. The Poliwhirl sample uses the same on-device model as the camera.",
                "内置模型可识别最初的151种关都宝可梦。置信度不足的结果不会记入发现。蚊香君样本与相机使用同一个设备端模型。",
                "内蔵モデルは、カントー地方の最初の151種を識別します。確信度が低い結果は発見に記録されません。ニョロゾのサンプルもカメラと同じ端末内モデルを使います。",
            ),
            color = DexColors.Cream.copy(0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        SectionTitle(language.text("Credits", "制作信息", "クレジット"))
        Text("iDex · FIELD UNIT 001", color = DexColors.Cream, fontFamily = FontFamily.Monospace)
        Text(
            language.text(
                "An independent fan project. Pokémon and its characters belong to Nintendo, Creatures, and GAME FREAK. Not affiliated with or endorsed by them.",
                "独立粉丝项目。宝可梦及相关角色归任天堂、Creatures及GAME FREAK所有。本项目与其无关联，亦未获得其认可。",
                "個人によるファンプロジェクトです。ポケモンおよび関連キャラクターの権利は、任天堂・クリーチャーズ・ゲームフリークに帰属します。本プロジェクトは、各社との提携や公認を受けていません。",
            ),
            color = DexColors.Cream.copy(0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LinkText(language.text("Sprites · Pokémon Database", "像素图 · Pokémon Database", "スプライト · Pokémon Database"), "https://pokemondb.net/sprites")
        LinkText(language.text("Pokédex data · PokéAPI", "图鉴数据 · PokéAPI", "図鑑データ · PokéAPI"), "https://github.com/PokeAPI/pokeapi")
        LinkText("Classifier · BiernyVR ONNX", "https://huggingface.co/BiernyVR/pokemon-classifier-mobilenetv3")
        Text("Press Start 2P · Fusion Pixel · SIL OFL 1.1", color = DexColors.Cream.copy(0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = DexColors.Lcd, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
    HorizontalDivider(color = Color.White.copy(0.12f))
}

@Composable
private fun Guide(text: String) {
    Text(text, color = DexColors.Cream, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun LinkText(label: String, url: String) {
    val context = LocalContext.current
    Text(
        label,
        color = DexColors.Lcd,
        fontSize = 14.sp,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
    )
}
