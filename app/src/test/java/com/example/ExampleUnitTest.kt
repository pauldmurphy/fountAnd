package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.domain.FountainLineType
import com.example.domain.FountainParser
import com.example.domain.TitlePageData
import com.example.ui.editor.QuickInsertToken
import com.example.ui.editor.applyQuickInsert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun fountainParser_extractsTitlePageScenesCharactersAndDialogue() {
        val doc = FountainParser.parse(FountainParser.SAMPLE_FOUNTAIN_SCRIPT)

        assertEquals("THE LAST PROJECTIONIST", doc.titlePage.title)
        assertEquals("Clara Vance", doc.titlePage.author)
        assertEquals(2, doc.scenes.size)
        assertEquals("INT. ORPHEUM PROJECTION BOOTH - NIGHT", doc.scenes[0].heading)
        assertEquals("INT. ORPHEUM AUDITORIUM - CONTINUOUS", doc.scenes[1].heading)
        assertTrue(doc.stats.characters.contains("ARTHUR"))
        assertTrue(doc.stats.characters.contains("MARA"))
        assertTrue(doc.elements.any { it.type == FountainLineType.PARENTHETICAL })
        assertTrue(doc.elements.any { it.type == FountainLineType.TRANSITION })
        assertTrue(doc.elements.any { it.type == FountainLineType.CENTERED })
    }

    @Test
    fun fountainParser_appliesTitlePageMetadataCleanly() {
        val updated = FountainParser.applyTitlePageMetadata(
            rawContent = FountainParser.SAMPLE_FOUNTAIN_SCRIPT,
            titleData = TitlePageData(
                title = "MIDNIGHT REEL",
                credit = "Written by",
                author = "Paul Murphy",
                draftDate = "2026"
            )
        )
        val reparsed = FountainParser.parse(updated)
        assertEquals("MIDNIGHT REEL", reparsed.titlePage.title)
        assertEquals("Paul Murphy", reparsed.titlePage.author)
        assertEquals(2, reparsed.scenes.size)
    }

    @Test
    fun quickInsert_insertsSceneSlugAndSelectsPlaceholder() {
        val initial = TextFieldValue(text = "", selection = TextRange(0))
        val afterInt = applyQuickInsert(initial, QuickInsertToken.INT)
        assertTrue(afterInt.text.startsWith("INT. LOCATION - DAY"))
        assertEquals("LOCATION", afterInt.text.substring(afterInt.selection.start, afterInt.selection.end))
    }
}
