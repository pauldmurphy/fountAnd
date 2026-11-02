package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ScriptEntity
import com.example.domain.SceneOutlineItem
import com.example.domain.ScriptStats
import com.example.ui.theme.CourierPrimeFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudioDrawerSheet(
    scriptTitle: String,
    scenes: List<SceneOutlineItem>,
    stats: ScriptStats,
    scripts: List<ScriptEntity>,
    activeScriptId: Long,
    onSceneSelected: (SceneOutlineItem) -> Unit,
    onScriptSelected: (ScriptEntity) -> Unit,
    onNewScriptClick: () -> Unit,
    onDeleteScript: (ScriptEntity) -> Unit,
    onOpenFromDriveClick: () -> Unit,
    onSaveToDriveClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxHeight()
            .testTag("studio_drawer_sheet")
    ) {
        // Studio Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_studio_emblem),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = scriptTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Script Stats Pill Row
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("script_stats_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatMetricItem(label = "Est. Pages", value = "${stats.estimatedPages}")
                StatMetricItem(label = "Scenes", value = "${stats.sceneCount}")
                StatMetricItem(label = "Words", value = "${stats.wordCount}")
                StatMetricItem(label = "Cast", value = "${stats.characters.size}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(id = R.string.drawer_tab_scenes)) },
                modifier = Modifier.testTag("drawer_tab_scenes")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(id = R.string.drawer_tab_library)) },
                modifier = Modifier.testTag("drawer_tab_library")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(stringResource(id = R.string.drawer_tab_guide)) },
                modifier = Modifier.testTag("drawer_tab_guide")
            )
        }

        when (selectedTab) {
            0 -> ScenesOutlineTab(
                scenes = scenes,
                characters = stats.characters,
                onSceneSelected = onSceneSelected
            )

            1 -> ScriptLibraryTab(
                scripts = scripts,
                activeScriptId = activeScriptId,
                onScriptSelected = onScriptSelected,
                onNewScriptClick = onNewScriptClick,
                onDeleteScript = onDeleteScript,
                onOpenFromDriveClick = onOpenFromDriveClick,
                onSaveToDriveClick = onSaveToDriveClick,
                onExportPdfClick = onExportPdfClick
            )

            2 -> FountainSyntaxGuideTab()
        }
    }
}

@Composable
private fun StatMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScenesOutlineTab(
    scenes: List<SceneOutlineItem>,
    characters: List<String>,
    onSceneSelected: (SceneOutlineItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("scenes_outline_list")
    ) {
        if (characters.isNotEmpty()) {
            item(key = "cast_section") {
                Text(
                    text = "SPEAKING CHARACTERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    characters.forEach { character ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = character,
                                    fontFamily = CourierPrimeFontFamily,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item(key = "scenes_header") {
            Text(
                text = "SCENE NAVIGATOR (${scenes.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (scenes.isEmpty()) {
            item(key = "empty_scenes") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.empty_scenes_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.empty_scenes_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = scenes,
                key = { "scene_${it.lineIndex}_${it.sceneIndex}" }
            ) { scene ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSceneSelected(scene) }
                        .testTag("scene_item_${scene.sceneIndex}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${scene.sceneNumberLabel}. ${scene.heading}",
                                fontFamily = CourierPrimeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "p.${scene.estimatedPage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (!scene.synopsisPreview.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scene.synopsisPreview,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptLibraryTab(
    scripts: List<ScriptEntity>,
    activeScriptId: Long,
    onScriptSelected: (ScriptEntity) -> Unit,
    onNewScriptClick: () -> Unit,
    onDeleteScript: (ScriptEntity) -> Unit,
    onOpenFromDriveClick: () -> Unit,
    onSaveToDriveClick: () -> Unit,
    onExportPdfClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("script_library_list")
    ) {
        item(key = "drive_and_local_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNewScriptClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_new_script_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Screenplay Draft")
                }

                OutlinedButton(
                    onClick = onOpenFromDriveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_open_drive_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.action_open_file))
                }

                OutlinedButton(
                    onClick = onSaveToDriveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_save_drive_button")
                ) {
                    Icon(Icons.Default.SaveAs, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.action_save_as_drive))
                }

                OutlinedButton(
                    onClick = onExportPdfClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.action_export_pdf))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LOCAL DRAFTS (${scripts.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(
            items = scripts,
            key = { "script_${it.id}" }
        ) { script ->
            val isSelected = script.id == activeScriptId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onScriptSelected(script) }
                    .testTag("script_card_${script.id}"),
                shape = RoundedCornerShape(10.dp),
                border = if (isSelected) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (script.linkedUri != null) {
                            Icons.Default.CloudDone
                        } else {
                            Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = script.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = buildString {
                            append(dateFormat.format(Date(script.updatedAt)))
                            if (!script.linkedFileName.isNullOrBlank()) {
                                append(" • ")
                                append(script.linkedFileName)
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (scripts.size > 1) {
                        IconButton(
                            onClick = { onDeleteScript(script) },
                            modifier = Modifier.testTag("delete_script_${script.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete screenplay",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FountainSyntaxGuideTab() {
    val guideItems = listOf(
        "Scene Heading" to "Start line with INT., EXT., I/E, or force with a leading period (.SNIPER SCOPE)",
        "Character Cue" to "ALL CAPS name preceded by an empty line, or force with @McCLANE",
        "Dialogue" to "Any text on the line immediately following a Character or Parenthetical",
        "Parenthetical" to "Wrap direction in parentheses (quietly) inside a Dialogue block",
        "Transition" to "UPPERCASE ending in TO: (e.g., CUT TO:) or start line with >",
        "Centered Text" to "Bracket line with > and < (e.g., > THE END <)",
        "Emphasis" to "*italics*, **bold**, ***bold italics***, and _underline_",
        "Title Page" to "Key: Value pairs at the very top of the script (Title:, Author:, Draft date:)",
        "Notes & Synopsis" to "Use [[Note text]] for inline notes and = Synopsis line for outline summaries"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("syntax_guide_list")
    ) {
        items(guideItems) { (title, syntax) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = syntax,
                        fontFamily = CourierPrimeFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
