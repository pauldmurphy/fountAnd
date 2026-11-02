package com.example.domain

import kotlin.math.max

object FountainParser {

    private val SCENE_PREFIX_REGEX = Regex(
        pattern = "^(INT\\./EXT\\.|INT/EXT|I/E|INT\\.|EXT\\.|EST\\.)([\\s\\.].*)",
        option = RegexOption.IGNORE_CASE
    )
    private val SCENE_NUM_SUFFIX_REGEX = Regex("#([A-Za-z0-9\\-.]+)#\\s*$")
    private val TITLE_KEY_REGEX = Regex(
        pattern = "^(Title|Credit|Author|Authors|Source|Draft date|Contact|Copyright|Notes):\\s*(.*)$",
        option = RegexOption.IGNORE_CASE
    )
    private val TRANSITION_SUFFIX_REGEX = Regex("^[A-Z0-9\\s]+TO:$")

    fun parse(text: String): FountainDocument {
        if (text.isEmpty()) {
            return FountainDocument()
        }

        val lines = text.split("\n")
        val elements = ArrayList<FountainElement>(lines.size)

        var inTitlePage = true
        var sawAnyTitleKey = false
        var currentTitleKey: String? = null
        val titleMap = mutableMapOf<String, MutableList<String>>()

        var currentOffset = 0
        var previousLineWasBlank = true
        var inDialogueBlock = false

        var formattedLinesAccumulator = 0
        var autoSceneIndex = 1
        val scenes = mutableListOf<SceneOutlineItem>()
        val characterSet = linkedSetOf<String>()

        for (i in lines.indices) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()
            val lineStart = currentOffset
            val lineEnd = currentOffset + rawLine.length
            currentOffset = lineEnd + 1 // +1 for '\n'

            // 1. Check Title Page block at very top of document
            if (inTitlePage) {
                val keyMatch = TITLE_KEY_REGEX.matchEntire(trimmed)
                if (keyMatch != null) {
                    sawAnyTitleKey = true
                    val keyName = keyMatch.groupValues[1].lowercase()
                    val firstValue = keyMatch.groupValues[2].trim()
                    currentTitleKey = keyName
                    val list = titleMap.getOrPut(keyName) { mutableListOf() }
                    if (firstValue.isNotEmpty()) {
                        list.add(firstValue)
                    }
                    elements.add(
                        FountainElement(
                            lineIndex = i,
                            startCharOffset = lineStart,
                            endCharOffset = lineEnd,
                            rawText = rawLine,
                            cleanText = rawLine,
                            type = FountainLineType.TITLE_KEY_VALUE
                        )
                    )
                    previousLineWasBlank = false
                    continue
                } else if (sawAnyTitleKey && currentTitleKey != null &&
                    trimmed.isNotEmpty() && (rawLine.startsWith("   ") || rawLine.startsWith("\t") || !trimmed.contains(":"))
                ) {
                    // Indented continuation of a Title Page key
                    titleMap.getOrPut(currentTitleKey) { mutableListOf() }.add(trimmed)
                    elements.add(
                        FountainElement(
                            lineIndex = i,
                            startCharOffset = lineStart,
                            endCharOffset = lineEnd,
                            rawText = rawLine,
                            cleanText = trimmed,
                            type = FountainLineType.TITLE_CONTINUATION
                        )
                    )
                    previousLineWasBlank = false
                    continue
                } else if (trimmed.isEmpty() && sawAnyTitleKey) {
                    // Blank line ends the Title Page header block
                    inTitlePage = false
                    currentTitleKey = null
                    elements.add(
                        FountainElement(
                            lineIndex = i,
                            startCharOffset = lineStart,
                            endCharOffset = lineEnd,
                            rawText = rawLine,
                            cleanText = "",
                            type = FountainLineType.EMPTY
                        )
                    )
                    previousLineWasBlank = true
                    continue
                } else {
                    inTitlePage = false
                }
            }

            // 2. Empty line
            if (trimmed.isEmpty()) {
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = "",
                        type = FountainLineType.EMPTY
                    )
                )
                previousLineWasBlank = true
                inDialogueBlock = false
                formattedLinesAccumulator += 1
                continue
            }

            // 3. Page break (===)
            if (trimmed.length >= 3 && trimmed.all { it == '=' }) {
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = "",
                        type = FountainLineType.PAGE_BREAK
                    )
                )
                formattedLinesAccumulator = ((formattedLinesAccumulator / 55) + 1) * 55
                previousLineWasBlank = false
                inDialogueBlock = false
                continue
            }

            // 4. Section Heading (#, ##, ###)
            if (trimmed.startsWith("#")) {
                val depth = trimmed.takeWhile { it == '#' }.length
                val clean = trimmed.drop(depth).trim()
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean,
                        type = FountainLineType.SECTION,
                        sectionDepth = depth
                    )
                )
                previousLineWasBlank = false
                inDialogueBlock = false
                continue
            }

            // 5. Synopsis (= ...)
            if (trimmed.startsWith("=") && !trimmed.startsWith("==")) {
                val clean = trimmed.removePrefix("=").trim()
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean,
                        type = FountainLineType.SYNOPSIS
                    )
                )
                if (scenes.isNotEmpty() && scenes.last().synopsisPreview == null) {
                    val lastIdx = scenes.lastIndex
                    scenes[lastIdx] = scenes[lastIdx].copy(synopsisPreview = clean)
                }
                previousLineWasBlank = false
                inDialogueBlock = false
                continue
            }

            // 6. Note ([[ ... ]])
            if (trimmed.startsWith("[[") && trimmed.endsWith("]]")) {
                val clean = trimmed.removePrefix("[[").removeSuffix("]]").trim()
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean,
                        type = FountainLineType.NOTE
                    )
                )
                previousLineWasBlank = false
                continue
            }

            // 7. Centered (> ... <)
            if (trimmed.startsWith(">") && trimmed.endsWith("<") && trimmed.length > 2) {
                val clean = trimmed.substring(1, trimmed.length - 1).trim()
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean,
                        type = FountainLineType.CENTERED
                    )
                )
                formattedLinesAccumulator += 2
                previousLineWasBlank = false
                inDialogueBlock = false
                continue
            }

            // 8. Lyrics (~ ...)
            if (trimmed.startsWith("~")) {
                val clean = trimmed.removePrefix("~").trim()
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean,
                        type = FountainLineType.LYRICS
                    )
                )
                formattedLinesAccumulator += 1
                previousLineWasBlank = false
                continue
            }

            // 9. Scene Heading (INT., EXT., EST., INT./EXT., or forced '.' prefix)
            val isForcedScene = trimmed.startsWith(".") && trimmed.length > 1 && !trimmed.startsWith("..")
            val isStandardScene = SCENE_PREFIX_REGEX.matches(trimmed)
            if ((isForcedScene || isStandardScene) && !trimmed.startsWith("!")) {
                var clean = if (isForcedScene) trimmed.substring(1).trim() else trimmed
                val sceneNumMatch = SCENE_NUM_SUFFIX_REGEX.find(clean)
                val explicitNum = sceneNumMatch?.groupValues?.get(1)
                if (sceneNumMatch != null) {
                    clean = clean.removeRange(sceneNumMatch.range).trim()
                }
                val numberLabel = explicitNum ?: autoSceneIndex.toString()
                autoSceneIndex++
                val cleanUpper = clean.uppercase()
                val estPage = max(1, (formattedLinesAccumulator / 55) + 1)

                scenes.add(
                    SceneOutlineItem(
                        sceneIndex = scenes.size + 1,
                        sceneNumberLabel = numberLabel,
                        heading = cleanUpper,
                        lineIndex = i,
                        charOffset = lineStart,
                        estimatedPage = estPage
                    )
                )

                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = cleanUpper,
                        type = FountainLineType.SCENE_HEADING,
                        sceneNumber = numberLabel
                    )
                )
                formattedLinesAccumulator += 2
                previousLineWasBlank = false
                inDialogueBlock = false
                continue
            }

            // 10. Transition (forced '>' prefix or ends with 'TO:' / 'FADE OUT.' / 'CUT TO BLACK.')
            val isForcedTransition = trimmed.startsWith(">") && !trimmed.endsWith("<")
            val isStandardTransition = previousLineWasBlank &&
                (TRANSITION_SUFFIX_REGEX.matches(trimmed) ||
                    trimmed == "FADE OUT." ||
                    trimmed == "FADE TO BLACK." ||
                    trimmed == "CUT TO BLACK.")
            if (isForcedTransition || isStandardTransition) {
                val clean = if (isForcedTransition) trimmed.removePrefix(">").trim().uppercase() else trimmed.uppercase()
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean,
                        type = FountainLineType.TRANSITION
                    )
                )
                formattedLinesAccumulator += 2
                previousLineWasBlank = false
                inDialogueBlock = false
                continue
            }

            // 11. Character cue (preceded by blank line, followed by non-empty line, uppercase or forced '@')
            val nextLineNotEmpty = (i + 1 < lines.size) && lines[i + 1].trim().isNotEmpty()
            val isForcedCharacter = trimmed.startsWith("@") && trimmed.length > 1
            val isAllCapsCharacter = previousLineWasBlank &&
                nextLineNotEmpty &&
                !trimmed.startsWith("!") &&
                trimmed.any { it.isLetter() } &&
                trimmed.filter { it.isLetter() }.all { it.isUpperCase() } &&
                trimmed.length <= 50

            if (isForcedCharacter || isAllCapsCharacter) {
                val clean = if (isForcedCharacter) trimmed.removePrefix("@").trim() else trimmed
                val speakerBase = clean.substringBefore("(").trim().uppercase()
                if (speakerBase.isNotEmpty() && speakerBase.length <= 32) {
                    characterSet.add(speakerBase)
                }
                elements.add(
                    FountainElement(
                        lineIndex = i,
                        startCharOffset = lineStart,
                        endCharOffset = lineEnd,
                        rawText = rawLine,
                        cleanText = clean.uppercase(),
                        type = FountainLineType.CHARACTER
                    )
                )
                formattedLinesAccumulator += 1
                previousLineWasBlank = false
                inDialogueBlock = true
                continue
            }

            // 12. Parenthetical or Dialogue (when inside a dialogue block)
            if (inDialogueBlock) {
                if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                    elements.add(
                        FountainElement(
                            lineIndex = i,
                            startCharOffset = lineStart,
                            endCharOffset = lineEnd,
                            rawText = rawLine,
                            cleanText = trimmed,
                            type = FountainLineType.PARENTHETICAL
                        )
                    )
                    formattedLinesAccumulator += 1
                } else {
                    elements.add(
                        FountainElement(
                            lineIndex = i,
                            startCharOffset = lineStart,
                            endCharOffset = lineEnd,
                            rawText = rawLine,
                            cleanText = trimmed,
                            type = FountainLineType.DIALOGUE
                        )
                    )
                    formattedLinesAccumulator += max(1, (trimmed.length + 34) / 35)
                }
                previousLineWasBlank = false
                continue
            }

            // 13. Default: Action (narrative description or forced '!')
            val cleanAction = if (trimmed.startsWith("!")) rawLine.trimStart().removePrefix("!") else rawLine.trim()
            elements.add(
                FountainElement(
                    lineIndex = i,
                    startCharOffset = lineStart,
                    endCharOffset = lineEnd,
                    rawText = rawLine,
                    cleanText = cleanAction,
                    type = FountainLineType.ACTION
                )
            )
            formattedLinesAccumulator += max(1, (cleanAction.length + 59) / 60)
            previousLineWasBlank = false
        }

        val titlePageData = TitlePageData(
            title = titleMap["title"]?.joinToString(" ") ?: "",
            credit = titleMap["credit"]?.joinToString(" ") ?: "",
            author = (titleMap["author"] ?: titleMap["authors"])?.joinToString(", ") ?: "",
            source = titleMap["source"]?.joinToString(" ") ?: "",
            draftDate = titleMap["draft date"]?.joinToString(" ") ?: "",
            contact = titleMap["contact"]?.joinToString("\n") ?: ""
        )

        val wordCount = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        val estimatedPages = max(1, (formattedLinesAccumulator + 54) / 55)

        return FountainDocument(
            titlePage = titlePageData,
            elements = elements,
            scenes = scenes,
            stats = ScriptStats(
                wordCount = wordCount,
                charCount = text.length,
                lineCount = lines.size,
                sceneCount = scenes.size,
                estimatedPages = estimatedPages,
                characters = characterSet.toList()
            )
        )
    }

    /**
     * Updates or inserts the Fountain Title Page block at the top of the raw script text.
     */
    fun applyTitlePageMetadata(rawContent: String, titleData: TitlePageData): String {
        val lines = rawContent.split("\n")
        var bodyStartIndex = 0
        var sawTitleHeader = false

        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (TITLE_KEY_REGEX.matches(trimmed)) {
                sawTitleHeader = true
                bodyStartIndex = i + 1
            } else if (sawTitleHeader && trimmed.isNotEmpty() &&
                (lines[i].startsWith("   ") || lines[i].startsWith("\t") || !trimmed.contains(":"))
            ) {
                bodyStartIndex = i + 1
            } else if (sawTitleHeader && trimmed.isEmpty()) {
                bodyStartIndex = i + 1
                break
            } else {
                break
            }
        }

        val remainingBody = if (sawTitleHeader && bodyStartIndex <= lines.size) {
            lines.subList(bodyStartIndex, lines.size).joinToString("\n").trimStart('\n')
        } else {
            rawContent.trimStart('\n')
        }

        if (!titleData.isNotEmpty) {
            return remainingBody
        }

        val headerBuilder = StringBuilder()
        if (titleData.title.isNotBlank()) headerBuilder.append("Title: ").append(titleData.title.trim()).append("\n")
        if (titleData.credit.isNotBlank()) headerBuilder.append("Credit: ").append(titleData.credit.trim()).append("\n")
        if (titleData.author.isNotBlank()) headerBuilder.append("Author: ").append(titleData.author.trim()).append("\n")
        if (titleData.source.isNotBlank()) headerBuilder.append("Source: ").append(titleData.source.trim()).append("\n")
        if (titleData.draftDate.isNotBlank()) headerBuilder.append("Draft date: ").append(titleData.draftDate.trim()).append("\n")
        if (titleData.contact.isNotBlank()) {
            val contactLines = titleData.contact.trim().split("\n")
            if (contactLines.size == 1) {
                headerBuilder.append("Contact: ").append(contactLines[0]).append("\n")
            } else {
                headerBuilder.append("Contact:\n")
                contactLines.forEach { line ->
                    headerBuilder.append("   ").append(line.trim()).append("\n")
                }
            }
        }

        return headerBuilder.toString() + "\n" + remainingBody
    }

    val SAMPLE_FOUNTAIN_SCRIPT = """
Title: THE LAST PROJECTIONIST
Credit: Written by
Author: Clara Vance
Source: Original Short Film
Draft date: November 2026
Contact:
   Noir Studio Productions
   Los Angeles, CA

# Act I — The Booth

= Arthur prepares the final 35mm reel before the Orpheum Theater closes forever.

FADE IN:

INT. ORPHEUM PROJECTION BOOTH - NIGHT #1#

A cramped, cathedral-like room high above the auditorium. Warm amber light spills from a single brass desk lamp onto a steel rewinder bench.

SILVER NITRATE FILM winds between two spoked reels with a rhythmic *whir-click*.

ARTHUR (68), wearing cotton archivist gloves and a faded vest, inspects a splice through a jeweler's loupe.

ARTHUR
(softly, to the projector)
Forty-two years of light, old friend. Let's not drop a frame tonight.

[[Sound cue: Distant rain drumming against the roof vents]]

A sharp KNOCK at the iron booth door.

MARA (26), drenched from the storm, steps inside carrying a rusted hexagonal film canister.

MARA
They're locking the lobby doors in twenty minutes, Arthur. The demolition crew is already outside.

ARTHUR
Then they can wait in the rain for two reels.
(eyeing the canister)
Where did you find that?

MARA
Behind the velvet curtain in Balcony B. It still has the *1954 premiere seal* on the lid.

CUT TO:

INT. ORPHEUM AUDITORIUM - CONTINUOUS #2#

Seven hundred empty crimson seats face a towering silver screen.

Suddenly, a blinding BEAM OF LIGHT pierces the dusty darkness from the booth window above.

> THE END OF REEL ONE <

FADE OUT.
    """.trimIndent()
}
