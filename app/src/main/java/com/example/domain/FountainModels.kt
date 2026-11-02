package com.example.domain

import androidx.compose.runtime.Immutable

enum class FountainLineType {
    TITLE_KEY_VALUE,
    TITLE_CONTINUATION,
    SCENE_HEADING,
    CHARACTER,
    DIALOGUE,
    PARENTHETICAL,
    TRANSITION,
    CENTERED,
    LYRICS,
    SECTION,
    SYNOPSIS,
    NOTE,
    PAGE_BREAK,
    ACTION,
    EMPTY
}

@Immutable
data class FountainElement(
    val lineIndex: Int,
    val startCharOffset: Int,
    val endCharOffset: Int,
    val rawText: String,
    val cleanText: String,
    val type: FountainLineType,
    val sceneNumber: String? = null,
    val sectionDepth: Int = 0
)

@Immutable
data class SceneOutlineItem(
    val sceneIndex: Int,
    val sceneNumberLabel: String,
    val heading: String,
    val lineIndex: Int,
    val charOffset: Int,
    val estimatedPage: Int,
    val synopsisPreview: String? = null
)

@Immutable
data class ScriptStats(
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val lineCount: Int = 0,
    val sceneCount: Int = 0,
    val estimatedPages: Int = 1,
    val characters: List<String> = emptyList()
)

@Immutable
data class TitlePageData(
    val title: String = "",
    val credit: String = "",
    val author: String = "",
    val source: String = "",
    val draftDate: String = "",
    val contact: String = ""
) {
    val isNotEmpty: Boolean
        get() = title.isNotBlank() || author.isNotBlank() || credit.isNotBlank() ||
            source.isNotBlank() || draftDate.isNotBlank() || contact.isNotBlank()
}

@Immutable
data class FountainDocument(
    val titlePage: TitlePageData = TitlePageData(),
    val elements: List<FountainElement> = emptyList(),
    val scenes: List<SceneOutlineItem> = emptyList(),
    val stats: ScriptStats = ScriptStats()
)
