package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.TitlePageData

@Composable
fun TitlePageEditorDialog(
    initialData: TitlePageData,
    fallbackTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (TitlePageData) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialData.title.ifBlank { fallbackTitle }) }
    var credit by rememberSaveable { mutableStateOf(initialData.credit.ifBlank { "Written by" }) }
    var author by rememberSaveable { mutableStateOf(initialData.author) }
    var source by rememberSaveable { mutableStateOf(initialData.source) }
    var draftDate by rememberSaveable { mutableStateOf(initialData.draftDate) }
    var contact by rememberSaveable { mutableStateOf(initialData.contact) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.title_page_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.title_page_dialog_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.label_title)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_page_input_title")
                )

                OutlinedTextField(
                    value = credit,
                    onValueChange = { credit = it },
                    label = { Text(stringResource(id = R.string.label_credit)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_page_input_credit")
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(id = R.string.label_author)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_page_input_author")
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text(stringResource(id = R.string.label_source)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_page_input_source")
                )

                OutlinedTextField(
                    value = draftDate,
                    onValueChange = { draftDate = it },
                    label = { Text(stringResource(id = R.string.label_draft_date)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_page_input_draft_date")
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text(stringResource(id = R.string.label_contact)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_page_input_contact")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        TitlePageData(
                            title = title,
                            credit = credit,
                            author = author,
                            source = source,
                            draftDate = draftDate,
                            contact = contact
                        )
                    )
                },
                modifier = Modifier.testTag("title_page_save_button")
            ) {
                Text(stringResource(id = R.string.btn_apply))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("title_page_cancel_button")
            ) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun NewScriptDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, author: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Screenplay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Screenplay Title") },
                    placeholder = { Text("MIDNIGHT EXPRESSIONS") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_script_title_input")
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author Name") },
                    placeholder = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_script_author_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, author) },
                modifier = Modifier.testTag("new_script_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        }
    )
}
