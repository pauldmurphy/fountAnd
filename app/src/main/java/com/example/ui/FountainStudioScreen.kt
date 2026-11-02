package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.NewScriptDialog
import com.example.ui.components.QuickInsertBar
import com.example.ui.components.ScreenplayPreviewPane
import com.example.ui.components.StudioDrawerSheet
import com.example.ui.components.TitlePageEditorDialog
import com.example.ui.editor.FountainSyntaxHighlighter
import com.example.ui.theme.CourierPrimeFontFamily
import com.example.ui.theme.LocalFountainSyntaxColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FountainStudioScreen(
    viewModel: FountainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // SAF Open Document Launcher (supports Google Drive & local device storage)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFountainUri(context, uri)
        }
    }

    // SAF Create Document Launcher for saving .fountain to Google Drive or local folders
    val saveFountainLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.saveToExternalUri(context, uri)
        }
    }

    // SAF Create Document Launcher for exporting formatted Screenplay PDF
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPdfToUri(context, uri)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // BackHandler for drawer or Preview mode returning to Editor mode
    BackHandler(enabled = drawerState.isOpen || uiState.viewMode != WorkspaceViewMode.EDITOR) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.setViewMode(WorkspaceViewMode.EDITOR)
        }
    }

    if (uiState.showTitlePageDialog) {
        TitlePageEditorDialog(
            initialData = uiState.parsedDocument.titlePage,
            fallbackTitle = uiState.scriptTitle,
            onDismiss = { viewModel.setShowTitlePageDialog(false) },
            onConfirm = { viewModel.applyTitlePage(it) }
        )
    }

    if (uiState.showNewScriptDialog) {
        NewScriptDialog(
            onDismiss = { viewModel.setShowNewScriptDialog(false) },
            onCreate = { title, author -> viewModel.createNewScript(title, author) }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            StudioDrawerSheet(
                scriptTitle = uiState.scriptTitle,
                scenes = uiState.parsedDocument.scenes,
                stats = uiState.parsedDocument.stats,
                scripts = scripts,
                activeScriptId = uiState.activeScriptId,
                onSceneSelected = { scene ->
                    viewModel.jumpToScene(scene)
                    scope.launch { drawerState.close() }
                },
                onScriptSelected = { script ->
                    viewModel.selectScript(script)
                    scope.launch { drawerState.close() }
                },
                onNewScriptClick = {
                    scope.launch { drawerState.close() }
                    viewModel.setShowNewScriptDialog(true)
                },
                onDeleteScript = { script ->
                    viewModel.deleteScript(script)
                },
                onOpenFromDriveClick = {
                    scope.launch { drawerState.close() }
                    openDocumentLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                },
                onSaveToDriveClick = {
                    scope.launch { drawerState.close() }
                    saveFountainLauncher.launch(uiState.suggestedFountainFileName)
                },
                onExportPdfClick = {
                    scope.launch { drawerState.close() }
                    exportPdfLauncher.launch(uiState.suggestedPdfFileName)
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.cd_open_drawer)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = uiState.scriptTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("top_bar_script_title")
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (uiState.saveStatus == SaveStatus.LINKED_TO_DRIVE) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                val statusText = when (uiState.saveStatus) {
                                    SaveStatus.SAVED_LOCALLY -> stringResource(R.string.status_saved_locally)
                                    SaveStatus.LINKED_TO_DRIVE -> uiState.linkedFileName
                                        ?: stringResource(R.string.status_synced_drive)
                                    SaveStatus.SAVING -> stringResource(R.string.status_saving)
                                }
                                Text(
                                    text = "$statusText • p.${uiState.parsedDocument.stats.estimatedPages}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setShowTitlePageDialog(true) },
                            modifier = Modifier.testTag("title_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = stringResource(id = R.string.cd_title_page)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("toggle_dark_mode_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) {
                                    Icons.Default.LightMode
                                } else {
                                    Icons.Default.DarkMode
                                },
                                contentDescription = stringResource(id = R.string.cd_toggle_dark_mode),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("file_actions_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(id = R.string.cd_file_actions)
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.action_open_file)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        openDocumentLauncher.launch(
                                            arrayOf("text/*", "application/octet-stream", "*/*")
                                        )
                                    },
                                    modifier = Modifier.testTag("menu_open_file")
                                )

                                if (uiState.linkedUri != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.action_save_linked)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Save, contentDescription = null)
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.saveToAlreadyLinkedUri(context)
                                        },
                                        modifier = Modifier.testTag("menu_save_linked")
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.action_save_as_drive)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.SaveAs, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        saveFountainLauncher.launch(uiState.suggestedFountainFileName)
                                    },
                                    modifier = Modifier.testTag("menu_save_as_drive")
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.action_export_pdf)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        exportPdfLauncher.launch(uiState.suggestedPdfFileName)
                                    },
                                    modifier = Modifier.testTag("menu_export_pdf")
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.action_toggle_line_numbers)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleLineNumbers()
                                    },
                                    modifier = Modifier.testTag("menu_toggle_line_numbers")
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                AnimatedVisibility(visible = uiState.viewMode != WorkspaceViewMode.PREVIEW) {
                    QuickInsertBar(onInsertToken = viewModel::onQuickInsert)
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWideTablet = maxWidth >= 720.dp

                Column(modifier = Modifier.fillMaxSize()) {
                    // Mode Switcher & Quick Cloud Save Header Strip
                    WorkspaceModeStrip(
                        viewMode = uiState.viewMode,
                        isWideTablet = isWideTablet,
                        sceneCount = uiState.parsedDocument.stats.sceneCount,
                        wordCount = uiState.parsedDocument.stats.wordCount,
                        onModeChange = viewModel::setViewMode,
                        onQuickSaveDriveClick = {
                            if (!viewModel.saveToAlreadyLinkedUri(context)) {
                                saveFountainLauncher.launch(uiState.suggestedFountainFileName)
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Adaptive Workspace Content
                    if (isWideTablet && uiState.viewMode != WorkspaceViewMode.PREVIEW) {
                        // Tablet / Wide Screen: Live Split-Pane Side-by-Side
                        Row(modifier = Modifier.fillMaxSize()) {
                            FountainSyntaxEditorPane(
                                value = uiState.textFieldValue,
                                showLineNumbers = uiState.showLineNumbers,
                                scrollToLine = uiState.scrollToLineEvent,
                                onConsumeScroll = viewModel::consumeScrollEvent,
                                onValueChange = viewModel::onTextFieldValueChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ScreenplayPreviewPane(
                                document = uiState.parsedDocument,
                                scrollToLine = uiState.scrollToLineEvent,
                                onConsumeScroll = viewModel::consumeScrollEvent,
                                onEditTitlePageClick = { viewModel.setShowTitlePageDialog(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        when (uiState.viewMode) {
                            WorkspaceViewMode.EDITOR -> {
                                FountainSyntaxEditorPane(
                                    value = uiState.textFieldValue,
                                    showLineNumbers = uiState.showLineNumbers,
                                    scrollToLine = uiState.scrollToLineEvent,
                                    onConsumeScroll = viewModel::consumeScrollEvent,
                                    onValueChange = viewModel::onTextFieldValueChange,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            WorkspaceViewMode.SPLIT -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    FountainSyntaxEditorPane(
                                        value = uiState.textFieldValue,
                                        showLineNumbers = uiState.showLineNumbers,
                                        scrollToLine = uiState.scrollToLineEvent,
                                        onConsumeScroll = viewModel::consumeScrollEvent,
                                        onValueChange = viewModel::onTextFieldValueChange,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        thickness = 1.5.dp
                                    )
                                    ScreenplayPreviewPane(
                                        document = uiState.parsedDocument,
                                        scrollToLine = uiState.scrollToLineEvent,
                                        onConsumeScroll = viewModel::consumeScrollEvent,
                                        onEditTitlePageClick = { viewModel.setShowTitlePageDialog(true) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                }
                            }

                            WorkspaceViewMode.PREVIEW -> {
                                ScreenplayPreviewPane(
                                    document = uiState.parsedDocument,
                                    scrollToLine = uiState.scrollToLineEvent,
                                    onConsumeScroll = viewModel::consumeScrollEvent,
                                    onEditTitlePageClick = { viewModel.setShowTitlePageDialog(true) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceModeStrip(
    viewMode: WorkspaceViewMode,
    isWideTablet: Boolean,
    sceneCount: Int,
    wordCount: Int,
    onModeChange: (WorkspaceViewMode) -> Unit,
    onQuickSaveDriveClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.testTag("mode_segmented_control")
            ) {
                val modes = if (isWideTablet) {
                    listOf(
                        WorkspaceViewMode.SPLIT to stringResource(R.string.mode_split),
                        WorkspaceViewMode.PREVIEW to stringResource(R.string.mode_preview)
                    )
                } else {
                    listOf(
                        WorkspaceViewMode.EDITOR to stringResource(R.string.mode_editor),
                        WorkspaceViewMode.SPLIT to stringResource(R.string.mode_split),
                        WorkspaceViewMode.PREVIEW to stringResource(R.string.mode_preview)
                    )
                }

                modes.forEachIndexed { index, (mode, label) ->
                    val selected = if (isWideTablet && viewMode == WorkspaceViewMode.EDITOR) {
                        mode == WorkspaceViewMode.SPLIT
                    } else {
                        viewMode == mode
                    }
                    SegmentedButton(
                        selected = selected,
                        onClick = { onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        modifier = Modifier.testTag("mode_tab_${mode.name.lowercase()}")
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                onClick = onQuickSaveDriveClick,
                modifier = Modifier.testTag("quick_save_drive_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAs,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Drive / Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FountainSyntaxEditorPane(
    value: TextFieldValue,
    showLineNumbers: Boolean,
    scrollToLine: Int?,
    onConsumeScroll: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalFountainSyntaxColors.current
    val highlighter = remember(syntaxColors) { FountainSyntaxHighlighter(syntaxColors) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToLine) {
        if (scrollToLine != null) {
            val approxPixelsPerLine = 60
            scrollState.animateScrollTo((scrollToLine * approxPixelsPerLine).coerceAtLeast(0))
            onConsumeScroll()
        }
    }

    val lineCount = remember(value.text) {
        value.text.count { it == '\n' } + 1
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (showLineNumbers) {
                val lineNumbersText = remember(lineCount) {
                    (1..lineCount).joinToString("\n")
                }
                Text(
                    text = lineNumbersText,
                    style = TextStyle(
                        fontFamily = CourierPrimeFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = syntaxColors.lineNumber,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .width(36.dp)
                        .padding(end = 10.dp)
                        .testTag("editor_line_numbers")
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = CourierPrimeFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = highlighter,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.text.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.editor_placeholder),
                                style = TextStyle(
                                    fontFamily = CourierPrimeFontFamily,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("fountain_editor_text_field")
            )
        }
    }
}
