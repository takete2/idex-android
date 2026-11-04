package dev.idex.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idex.android.data.PokemonCatalog
import dev.idex.android.ui.sprites.SpriteStore
import dev.idex.android.ui.theme.DexColors

@Composable
fun CatalogScreen(
    state: DexUiState,
    catalog: PokemonCatalog,
    sprites: SpriteStore,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = state.language
    var query by rememberSaveable { mutableStateOf("") }
    var seenOnly by rememberSaveable { mutableStateOf(false) }
    val matches = catalog.search(query, language).filter { !seenOnly || state.seenIds.contains(it.id) }
    Column(modifier.fillMaxSize().background(DexColors.Charcoal)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelText(language.text("Kanto archive", "关都图鉴", "カントー図鑑"), language, 10, color = DexColors.Cream, modifier = Modifier.weight(1f))
            HardwareButton(onClick = onClose) {
                PixelText(language.text("Done", "完成", "完了"), language, 8, color = DexColors.Cream)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = DexColors.Cream, fontFamily = FontFamily.Monospace),
            placeholder = {
                Text(
                    language.text("Name, number or type", "名称、编号或属性", "名前・番号・タイプ"),
                    color = DexColors.Cream.copy(0.5f),
                    fontFamily = FontFamily.Monospace,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DexColors.Lcd,
                unfocusedBorderColor = Color.White.copy(0.3f),
                cursorColor = DexColors.Lcd,
                focusedTextColor = DexColors.Cream,
                unfocusedTextColor = DexColors.Cream,
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                language.text("Discovered only", "仅显示已发现", "発見済みのみ"),
                color = DexColors.Cream,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = seenOnly,
                onCheckedChange = { seenOnly = it },
                colors = SwitchDefaults.colors(checkedTrackColor = DexColors.Lcd, checkedThumbColor = DexColors.Ink),
            )
        }
        if (matches.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PixelText(
                    language.text("No matches", "没有匹配", "該当なし"),
                    language,
                    8,
                    color = DexColors.Cream,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(124.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(matches, key = { it.id }) { pokemon ->
                        Column(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(DexColors.Lcd)
                            .clickable { onSelect(pokemon.id) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("#${pokemon.number}", color = DexColors.Ink, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            if (state.seenIds.contains(pokemon.id)) {
                                Icon(Icons.Filled.CheckCircle, null, tint = DexColors.Ink, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        SpriteImage(sprites, pokemon.id, null, Modifier.height(72.dp).fillMaxWidth())
                        PixelText(pokemon.name(language), language, 7, color = DexColors.Ink)
                    }
                }
            }
        }
    }
}
