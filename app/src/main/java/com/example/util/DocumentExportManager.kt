package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.FountainDocument
import com.example.domain.FountainLineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object DocumentExportManager {

    fun resolveDisplayName(context: Context, uri: Uri): String {
        var displayName = "screenplay.fountain"
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val value = cursor.getString(nameIndex)
                    if (!value.isNullOrBlank()) {
                        displayName = value
                    }
                }
            }
        }
        return displayName
    }

    fun takePersistablePermissionIfPossible(context: Context, uri: Uri) {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
    }

    suspend fun readFountainFromUri(context: Context, uri: Uri): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                takePersistablePermissionIfPossible(context, uri)
                val fileName = resolveDisplayName(context, uri)
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                } ?: error("Unable to open file stream")
                fileName to content
            }
        }

    suspend fun writeFountainToUri(context: Context, uri: Uri, content: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                takePersistablePermissionIfPossible(context, uri)
                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                        writer.flush()
                    }
                } ?: context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                        writer.flush()
                    }
                } ?: error("Unable to open output stream for writing")
                resolveDisplayName(context, uri)
            }
        }

    /**
     * Renders a formatted US Letter (612 x 792 pt) screenplay PDF using Courier Prime
     * and standard motion picture screenplay indentation rules.
     */
    suspend fun exportFormattedPdfToUri(
        context: Context,
        uri: Uri,
        document: FountainDocument
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val pdfDocument = PdfDocument()
            val pageWidth = 612
            val pageHeight = 792
            val topMargin = 72f
            val bottomMargin = 720f
            val lineHeight = 14.5f

            // Screenplay X offsets in points (72 pt = 1 inch)
            val marginActionLeft = 108f // 1.5"
            val marginCharacterLeft = 252f // 3.5"
            val marginParentheticalLeft = 216f // 3.0"
            val marginDialogueLeft = 180f // 2.5"
            val marginRight = 540f // 7.5"

            val courierRegular = runCatching {
                ResourcesCompat.getFont(context, R.font.courier_prime)
            }.getOrNull() ?: Typeface.MONOSPACE
            val courierBold = Typeface.create(courierRegular, Typeface.BOLD)
            val courierItalic = Typeface.create(courierRegular, Typeface.ITALIC)

            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.BLACK
                textSize = 12f
                typeface = courierRegular
            }

            // 1. Optional Title Page
            if (document.titlePage.isNotEmpty) {
                val titlePageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdfDocument.startPage(titlePageInfo)
                val canvas = page.canvas

                basePaint.textAlign = Paint.Align.CENTER
                basePaint.typeface = courierBold
                basePaint.textSize = 16f
                var centerY = 260f
                val titleText = document.titlePage.title.ifBlank { "UNTITLED SCREENPLAY" }.uppercase()
                wrapText(titleText, 42).forEach { line ->
                    canvas.drawText(line, pageWidth / 2f, centerY, basePaint)
                    centerY += 22f
                }

                basePaint.typeface = courierRegular
                basePaint.textSize = 12f
                centerY += 18f
                if (document.titlePage.credit.isNotBlank()) {
                    canvas.drawText(document.titlePage.credit, pageWidth / 2f, centerY, basePaint)
                    centerY += 20f
                }
                if (document.titlePage.author.isNotBlank()) {
                    canvas.drawText(document.titlePage.author, pageWidth / 2f, centerY, basePaint)
                    centerY += 24f
                }
                if (document.titlePage.source.isNotBlank()) {
                    basePaint.typeface = courierItalic
                    canvas.drawText(document.titlePage.source, pageWidth / 2f, centerY, basePaint)
                    basePaint.typeface = courierRegular
                }

                // Bottom-left Contact & Bottom-right Draft Date
                var bottomLeftY = 660f
                basePaint.textAlign = Paint.Align.LEFT
                document.titlePage.contact.lines().filter { it.isNotBlank() }.forEach { line ->
                    canvas.drawText(line.trim(), marginActionLeft, bottomLeftY, basePaint)
                    bottomLeftY += lineHeight
                }
                if (document.titlePage.draftDate.isNotBlank()) {
                    basePaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(document.titlePage.draftDate, marginRight, 690f, basePaint)
                }

                pdfDocument.finishPage(page)
            }

            // 2. Body Pages
            var scriptPageNumber = 1
            var currentPageInfo = PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pdfDocument.pages.size + 1
            ).create()
            var currentPage = pdfDocument.startPage(currentPageInfo)
            var canvas = currentPage.canvas
            var currentY = topMargin

            fun startNewPage() {
                pdfDocument.finishPage(currentPage)
                scriptPageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pdfDocument.pages.size + 1
                ).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                currentY = topMargin

                // Page header number on pages 2+
                basePaint.typeface = courierRegular
                basePaint.textSize = 11f
                basePaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("$scriptPageNumber.", marginRight, 46f, basePaint)
                basePaint.textSize = 12f
            }

            fun ensureSpace(linesNeeded: Int = 1) {
                if (currentY + (linesNeeded * lineHeight) > bottomMargin) {
                    startNewPage()
                }
            }

            for (element in document.elements) {
                when (element.type) {
                    FountainLineType.TITLE_KEY_VALUE,
                    FountainLineType.TITLE_CONTINUATION,
                    FountainLineType.SECTION,
                    FountainLineType.SYNOPSIS,
                    FountainLineType.NOTE -> {
                        // Non-printing structural/metadata elements in final PDF
                    }

                    FountainLineType.PAGE_BREAK -> {
                        startNewPage()
                    }

                    FountainLineType.EMPTY -> {
                        currentY += lineHeight * 0.75f
                    }

                    FountainLineType.SCENE_HEADING -> {
                        currentY += lineHeight * 0.5f
                        ensureSpace(2)
                        basePaint.typeface = courierBold
                        basePaint.textAlign = Paint.Align.LEFT
                        val cleaned = stripInlineMarkup(element.cleanText)
                        wrapText(cleaned, 58).forEach { line ->
                            canvas.drawText(line, marginActionLeft, currentY, basePaint)
                            currentY += lineHeight
                        }
                        currentY += lineHeight * 0.3f
                    }

                    FountainLineType.CHARACTER -> {
                        ensureSpace(3)
                        basePaint.typeface = courierBold
                        basePaint.textAlign = Paint.Align.LEFT
                        val cleaned = stripInlineMarkup(element.cleanText)
                        canvas.drawText(cleaned, marginCharacterLeft, currentY, basePaint)
                        currentY += lineHeight
                    }

                    FountainLineType.PARENTHETICAL -> {
                        ensureSpace(1)
                        basePaint.typeface = courierItalic
                        basePaint.textAlign = Paint.Align.LEFT
                        wrapText(stripInlineMarkup(element.cleanText), 30).forEach { line ->
                            canvas.drawText(line, marginParentheticalLeft, currentY, basePaint)
                            currentY += lineHeight
                        }
                    }

                    FountainLineType.DIALOGUE -> {
                        basePaint.typeface = courierRegular
                        basePaint.textAlign = Paint.Align.LEFT
                        val wrapped = wrapText(stripInlineMarkup(element.cleanText), 36)
                        ensureSpace(wrapped.size)
                        wrapped.forEach { line ->
                            canvas.drawText(line, marginDialogueLeft, currentY, basePaint)
                            currentY += lineHeight
                        }
                    }

                    FountainLineType.TRANSITION -> {
                        ensureSpace(2)
                        basePaint.typeface = courierBold
                        basePaint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(stripInlineMarkup(element.cleanText), marginRight, currentY, basePaint)
                        currentY += lineHeight * 1.4f
                    }

                    FountainLineType.CENTERED,
                    FountainLineType.LYRICS -> {
                        ensureSpace(1)
                        basePaint.typeface = if (element.type == FountainLineType.LYRICS) courierItalic else courierRegular
                        basePaint.textAlign = Paint.Align.CENTER
                        wrapText(stripInlineMarkup(element.cleanText), 52).forEach { line ->
                            canvas.drawText(line, pageWidth / 2f, currentY, basePaint)
                            currentY += lineHeight
                        }
                    }

                    FountainLineType.ACTION -> {
                        basePaint.typeface = courierRegular
                        basePaint.textAlign = Paint.Align.LEFT
                        val wrapped = wrapText(stripInlineMarkup(element.cleanText), 60)
                        ensureSpace(wrapped.size)
                        wrapped.forEach { line ->
                            canvas.drawText(line, marginActionLeft, currentY, basePaint)
                            currentY += lineHeight
                        }
                    }
                }
            }

            pdfDocument.finishPage(currentPage)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
            } ?: error("Unable to open PDF output stream")
            pdfDocument.close()
            resolveDisplayName(context, uri)
        }
    }

    private fun stripInlineMarkup(text: String): String {
        return text
            .replace(Regex("\\[\\[.+?]]"), "")
            .replace("***", "")
            .replace("**", "")
            .replace("*", "")
            .replace("_", "")
            .trim()
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.length <= maxCharsPerLine) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + 1 + word.length <= maxCharsPerLine) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
