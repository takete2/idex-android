package dev.idex.android

import dev.idex.android.data.AppLanguage
import dev.idex.android.data.PokemonCatalog
import dev.idex.android.recognition.ModelLabel
import dev.idex.android.recognition.RecognitionException
import dev.idex.android.recognition.Scoring
import dev.idex.android.ui.PaneLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogAndScoringTest {
    private val catalog: PokemonCatalog by lazy {
        val file = listOf(
            File("src/main/assets/catalog/pokemon-kanto.json"),
            File("app/src/main/assets/catalog/pokemon-kanto.json"),
        ).first { it.exists() }
        PokemonCatalog.fromJson(file.readText())
    }

    @Test
    fun catalogHasTheKanto151() {
        assertEquals(151, catalog.entries.size)
        assertEquals((1..151).toSet(), catalog.entries.map { it.id }.toSet())
        val poliwhirl = catalog.pokemon(61)!!
        assertEquals("Poliwhirl", poliwhirl.englishName)
        assertTrue(poliwhirl.types.contains("water"))
        assertTrue(poliwhirl.chineseName.isNotBlank())
        assertTrue(poliwhirl.japaneseName.isNotBlank())
        assertEquals(6, poliwhirl.stats.size)
        assertTrue(poliwhirl.height > 0)
        assertTrue(poliwhirl.weight > 0)
    }

    @Test
    fun searchMatchesNameNumberAndTypeInEachLanguage() {
        val poliwhirl = catalog.pokemon(61)!!
        assertTrue(catalog.search(poliwhirl.englishName, AppLanguage.En).any { it.id == 61 })
        assertTrue(catalog.search(poliwhirl.chineseName, AppLanguage.ZhHans).any { it.id == 61 })
        assertTrue(catalog.search(poliwhirl.japaneseName, AppLanguage.Ja).any { it.id == 61 })
        assertTrue(catalog.search("061", AppLanguage.En).any { it.id == 61 })
        assertTrue(catalog.search("#61", AppLanguage.En).any { it.id == 61 })
        assertTrue(catalog.search("water", AppLanguage.En).any { it.id == 61 })
        assertTrue(catalog.search("水", AppLanguage.ZhHans).any { it.id == 61 })
        assertTrue(catalog.search("みず", AppLanguage.Ja).any { it.id == 61 })
        assertEquals(151, catalog.search("   ", AppLanguage.En).size)
        assertTrue(catalog.search("not-a-pokemon-xyz", AppLanguage.En).isEmpty())
    }

    @Test
    fun acceptanceUsesConfidenceAndLogitMargin() {
        val labels = List(151) { ModelLabel(it + 1, "species-$it") }
        val strong = FloatArray(151) { -4f }
        strong[60] = 5f
        strong[24] = 1f
        val accepted = Scoring.interpret(strong, labels)
        assertEquals(61, accepted.accepted?.dexId)
        assertEquals(61, accepted.candidates.first().dexId)
        assertTrue(accepted.logitMargin >= Scoring.MIN_LOGIT_MARGIN)

        val flat = FloatArray(151) { 0.2f }
        assertNull(Scoring.interpret(flat, labels).accepted)

        val close = FloatArray(151) { -2f }
        close[60] = 3f
        close[24] = 2.8f
        assertNull(Scoring.interpret(close, labels).accepted)

        val broken = FloatArray(151) { 0f }
        broken[0] = Float.NaN
        try {
            Scoring.interpret(broken, labels)
            throw AssertionError("expected invalid scores")
        } catch (_: RecognitionException) {
        }
    }

    @Test
    fun wideAndFoldedWindowsUseTwoPanes() {
        assertTrue(!PaneLayout.useDualPane(400f, 800f, verticalFold = false, expandedWidth = false))
        assertTrue(!PaneLayout.useDualPane(800f, 360f, verticalFold = false, expandedWidth = false))
        assertTrue(PaneLayout.useDualPane(700f, 840f, verticalFold = true, expandedWidth = false))
        assertTrue(PaneLayout.useDualPane(900f, 700f, verticalFold = false, expandedWidth = true))
        assertTrue(PaneLayout.useDualPane(720f, 900f, verticalFold = false, expandedWidth = false))
        assertTrue(PaneLayout.useTabletop(800f, horizontalSeparatingFold = true))
        assertTrue(!PaneLayout.useTabletop(800f, horizontalSeparatingFold = false))
    }
}
