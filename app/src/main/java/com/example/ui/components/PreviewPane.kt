package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FountainDocument
import com.example.domain.FountainElement
import com.example.domain.FountainLineType
import com.example.domain.TitlePageData
import com.example.ui.theme.CourierPrimeFontFamily
import com.example.ui.theme.LocalFountainSyntaxColors

@Composable
fun ScreenplayPreviewPane(
    document: FountainDocument,
    scrollToLine: Int?,
    onConsumeScroll: () -> Unit,
    onEditTitlePageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalFountainSyntaxColors.current
    val listState = rememberLazyListState()

    val printableElements = document.elements.filter {
        it.type != FountainLineType.TITLE_KEY_VALUE &&
            it.type != FountainLineType.TITLE_CONTINUATION
    }

    LaunchedEffect(scrollToLine) {
        if (scrollToLine != null) {
            val targetIdx = printableElements.indexOfFirst { it.lineIndex >= scrollToLine }
            if (targetIdx >= 0) {
                listState.animateScrollToItem(targetIdx + (if (document.titlePage.isNotEmpty) 1 else 0))
            }
            onConsumeScroll()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("screenplay_preview_pane"),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            shape = RoundedCornerShape(12.dp),
            color = syntaxColors.paperSurface,
            border = BorderStroke(1.dp, syntaxColors.paperBorder),
            tonalElevation = 2.dp
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (document.titlePage.isNotEmpty) {
                    item(key = "title_page_card") {
                        TitlePagePreviewBlock(
                            titlePage = document.titlePage,
                            onClick = onEditTitlePageClick
                        )
                    }
                }

                items(
                    items = printableElements,
                    key = { "line_${it.lineIndex}" }
                ) { element ->
                    ScreenplayPreviewElementRow(element = element)
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
private fun TitlePagePreviewBlock(
    titlePage: TitlePageData,
    onClick: () -> Unit
) {
    val syntaxColors = LocalFountainSyntaxColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp)
            .testTag("preview_title_page_block"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titlePage.title.ifBlank { "UNTITLED SCREENPLAY" }.uppercase(),
            fontFamily = CourierPrimeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (titlePage.credit.isNotBlank()) {
            Text(
                text = titlePage.credit,
                fontFamily = CourierPrimeFontFamily,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (titlePage.author.isNotBlank()) {
            Text(
                text = titlePage.author,
                fontFamily = CourierPrimeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (titlePage.source.isNotBlank()) {
            Text(
                text = titlePage.source,
                fontFamily = CourierPrimeFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (titlePage.contact.isNotBlank() || titlePage.draftDate.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = titlePage.contact,
                    fontFamily = CourierPrimeFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (titlePage.draftDate.isNotBlank()) {
                    Text(
                        text = titlePage.draftDate,
                        fontFamily = CourierPrimeFontFamily,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = syntaxColors.paperBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScreenplayPreviewElementRow(element: FountainElement) {
    val syntaxColors = LocalFountainSyntaxColors.current

    when (element.type) {
        FountainLineType.EMPTY -> {
            Spacer(modifier = Modifier.height(10.dp))
        }

        FountainLineType.PAGE_BREAK -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = syntaxColors.paperBorder)
                Text(
                    text = "PAGE BREAK",
                    style = MaterialTheme.typography.labelMedium,
                    color = syntaxColors.syntaxMarker,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        FountainLineType.SCENE_HEADING -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 6.dp)
                    .background(
                        color = syntaxColors.sceneHeadingBg,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatInlineStyles(element.cleanText),
                    fontFamily = CourierPrimeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = syntaxColors.sceneHeading,
                    modifier = Modifier.weight(1f)
                )
                if (!element.sceneNumber.isNullOrBlank()) {
                    Text(
                        text = "#${element.sceneNumber}",
                        fontFamily = CourierPrimeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = syntaxColors.syntaxMarker,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        FountainLineType.CHARACTER -> {
            Text(
                text = formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = syntaxColors.characterCue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 104.dp, top = 8.dp, bottom = 2.dp)
            )
        }

        FountainLineType.PARENTHETICAL -> {
            Text(
                text = formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = syntaxColors.parenthetical,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp, end = 48.dp)
            )
        }

        FountainLineType.DIALOGUE -> {
            Text(
                text = formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = syntaxColors.dialogue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 36.dp)
            )
        }

        FountainLineType.TRANSITION -> {
            Text(
                text = formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.End,
                color = syntaxColors.transition,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }

        FountainLineType.CENTERED -> {
            Text(
                text = formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = syntaxColors.centeredAndLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        FountainLineType.LYRICS -> {
            Text(
                text = "♪ " + formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = syntaxColors.centeredAndLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }

        FountainLineType.SECTION -> {
            Text(
                text = element.cleanText,
                style = MaterialTheme.typography.titleSmall,
                color = syntaxColors.sectionAndSynopsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
            )
        }

        FountainLineType.SYNOPSIS -> {
            Text(
                text = "Synopsis: ${element.cleanText}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = syntaxColors.sectionAndSynopsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }

        FountainLineType.NOTE -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Note: ${element.cleanText}",
                    fontFamily = CourierPrimeFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = syntaxColors.note,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        FountainLineType.ACTION -> {
            Text(
                text = formatInlineStyles(element.cleanText),
                fontFamily = CourierPrimeFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = syntaxColors.action,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }

        FountainLineType.TITLE_KEY_VALUE,
        FountainLineType.TITLE_CONTINUATION -> Unit
    }
}

private val boldItalicPreviewRegex = Regex("\\*\\*\\*(.+?)\\*\\*\\*")
private val boldPreviewRegex = Regex("\\*\\*(.+?)\\*\\*")
private val italicPreviewRegex = Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)")
private val underlinePreviewRegex = Regex("_([^_\\n]+)_")

private fun formatInlineStyles(rawText: String): AnnotatedString {
    if (!rawText.contains('*') && !rawText.contains('_')) {
        return AnnotatedString(rawText)
    }

    return buildAnnotatedString {
        var working = rawText
        // Strip markup delimiters while applying spans cleanly
        val boldRanges = mutableListOf<IntRange>()
        val italicRanges = mutableListOf<IntRange>()
        val underlineRanges = mutableListOf<IntRange>()

        // Simple single-pass parser for *, **, *** and _
        var i = 0
        while (i < working.length) {
            when {
                working.startsWith("***", i) -> {
                    val close = working.indexOf("***", i + 3)
                    if (close != -1) {
                        val content = working.substring(i + 3, close)
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                            start,
                            length
                        )
                        i = close + 3
                        continue
                    }
                }
                working.startsWith("**", i) -> {
                    val close = working.indexOf("**", i + 2)
                    if (close != -1) {
                        val content = working.substring(i + 2, close)
                        val start = length
                        append(content)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                        i = close + 2
                        continue
                    }
                }
                working[i] == '*' -> {
                    val close = working.indexOf('*', i + 1)
                    if (close != -1) {
                        val content = working.substring(i + 1, close)
                        val start = length
                        append(content)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                        i = close + 1
                        continue
                    }
                }
                working[i] == '_' -> {
                    val close = working.indexOf('_', i + 1)
                    if (close != -1) {
                        val content = working.substring(i + 1, close)
                        val start = length
                        append(content)
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, length)
                        i = close + 1
                        continue
                    }
                }
            }
            append(working[i])
            i++
        }
    }
}
