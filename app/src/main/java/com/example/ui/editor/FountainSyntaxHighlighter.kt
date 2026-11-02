package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.example.domain.FountainLineType
import com.example.domain.FountainParser
import com.example.ui.theme.FountainSyntaxColors

class FountainSyntaxHighlighter(
    private val colors: FountainSyntaxColors
) : VisualTransformation {

    private val boldItalicRegex = Regex("\\*\\*\\*(.+?)\\*\\*\\*")
    private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    private val italicRegex = Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)")
    private val underlineRegex = Regex("_([^_\\n]+)_")
    private val inlineNoteRegex = Regex("\\[\\[(.+?)]]")

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val doc = FountainParser.parse(raw)
        val builder = AnnotatedString.Builder(raw)

        for (element in doc.elements) {
            val start = element.startCharOffset
            val end = element.endCharOffset
            if (start >= end || end > raw.length) continue

            when (element.type) {
                FountainLineType.TITLE_KEY_VALUE -> {
                    val colonIndex = raw.indexOf(':', startIndex = start)
                    if (colonIndex in start until end) {
                        builder.addStyle(
                            SpanStyle(color = colors.titleKey, fontWeight = FontWeight.Bold),
                            start,
                            colonIndex + 1
                        )
                        if (colonIndex + 1 < end) {
                            builder.addStyle(
                                SpanStyle(color = colors.titleValue, fontWeight = FontWeight.Bold),
                                colonIndex + 1,
                                end
                            )
                        }
                    } else {
                        builder.addStyle(SpanStyle(color = colors.titleKey), start, end)
                    }
                }

                FountainLineType.TITLE_CONTINUATION -> {
                    builder.addStyle(
                        SpanStyle(color = colors.titleValue, fontStyle = FontStyle.Italic),
                        start,
                        end
                    )
                }

                FountainLineType.SCENE_HEADING -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.sceneHeading,
                            background = colors.sceneHeadingBg,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.CHARACTER -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.characterCue,
                            fontWeight = FontWeight.Bold
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.PARENTHETICAL -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.parenthetical,
                            fontStyle = FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.DIALOGUE -> {
                    builder.addStyle(
                        SpanStyle(color = colors.dialogue),
                        start,
                        end
                    )
                }

                FountainLineType.TRANSITION -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.transition,
                            fontWeight = FontWeight.Bold
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.CENTERED,
                FountainLineType.LYRICS -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.centeredAndLyrics,
                            fontStyle = FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.SECTION,
                FountainLineType.SYNOPSIS -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.sectionAndSynopsis,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.NOTE -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.note,
                            fontStyle = FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.PAGE_BREAK -> {
                    builder.addStyle(
                        SpanStyle(
                            color = colors.syntaxMarker,
                            fontWeight = FontWeight.Bold
                        ),
                        start,
                        end
                    )
                }

                FountainLineType.ACTION -> {
                    builder.addStyle(
                        SpanStyle(color = colors.action),
                        start,
                        end
                    )
                }

                FountainLineType.EMPTY -> Unit
            }
        }

        // Apply inline Fountain emphasis styles (*italic*, **bold**, ***bold italic***, _underline_, [[note]])
        boldItalicRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }
        boldRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
        italicRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }
        underlineRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
        }
        inlineNoteRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.note, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

enum class QuickInsertToken(val label: String, val testTag: String) {
    INT("INT.", "quick_insert_int"),
    EXT("EXT.", "quick_insert_ext"),
    DAY_NIGHT("- NIGHT", "quick_insert_night"),
    CHARACTER("CHARACTER", "quick_insert_character"),
    PARENTHETICAL("(beat)", "quick_insert_parenthetical"),
    TRANSITION("CUT TO:", "quick_insert_transition"),
    CENTERED("> TEXT <", "quick_insert_centered"),
    NOTE("[[Note]]", "quick_insert_note")
}

fun applyQuickInsert(current: TextFieldValue, token: QuickInsertToken): TextFieldValue {
    val text = current.text
    val cursor = current.selection.start.coerceIn(0, text.length)
    val before = text.substring(0, cursor)
    val after = text.substring(cursor)
    val needsLeadingNewline = before.isNotEmpty() && !before.endsWith("\n")
    val needsDoubleNewline = before.isNotEmpty() && !before.endsWith("\n\n")

    return when (token) {
        QuickInsertToken.INT -> {
            val prefix = if (needsDoubleNewline) (if (needsLeadingNewline) "\n\n" else "\n") else ""
            val insert = "${prefix}INT. LOCATION - DAY\n\n"
            val newText = before + insert + after
            val selectStart = before.length + prefix.length + 5
            val selectEnd = selectStart + 8 // Select "LOCATION" for immediate typing
            TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
        }

        QuickInsertToken.EXT -> {
            val prefix = if (needsDoubleNewline) (if (needsLeadingNewline) "\n\n" else "\n") else ""
            val insert = "${prefix}EXT. LOCATION - NIGHT\n\n"
            val newText = before + insert + after
            val selectStart = before.length + prefix.length + 5
            val selectEnd = selectStart + 8
            TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
        }

        QuickInsertToken.DAY_NIGHT -> {
            val insert = " - NIGHT\n\n"
            val newText = before + insert + after
            TextFieldValue(text = newText, selection = TextRange(before.length + insert.length))
        }

        QuickInsertToken.CHARACTER -> {
            val prefix = if (needsDoubleNewline) (if (needsLeadingNewline) "\n\n" else "\n") else ""
            val insert = "${prefix}CHARACTER NAME\n"
            val newText = before + insert + after
            val selectStart = before.length + prefix.length
            val selectEnd = selectStart + 14
            TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
        }

        QuickInsertToken.PARENTHETICAL -> {
            val prefix = if (needsLeadingNewline) "\n" else ""
            val insert = "${prefix}(beat)\n"
            val newText = before + insert + after
            val selectStart = before.length + prefix.length + 1
            val selectEnd = selectStart + 4
            TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
        }

        QuickInsertToken.TRANSITION -> {
            val prefix = if (needsDoubleNewline) (if (needsLeadingNewline) "\n\n" else "\n") else ""
            val insert = "${prefix}CUT TO:\n\n"
            val newText = before + insert + after
            TextFieldValue(text = newText, selection = TextRange(before.length + insert.length))
        }

        QuickInsertToken.CENTERED -> {
            val prefix = if (needsDoubleNewline) (if (needsLeadingNewline) "\n\n" else "\n") else ""
            val insert = "${prefix}> CENTERED TEXT <\n\n"
            val newText = before + insert + after
            val selectStart = before.length + prefix.length + 2
            val selectEnd = selectStart + 13
            TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
        }

        QuickInsertToken.NOTE -> {
            val insert = "[[Production note]]"
            val newText = before + insert + after
            val selectStart = before.length + 2
            val selectEnd = selectStart + 15
            TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
        }
    }
}
