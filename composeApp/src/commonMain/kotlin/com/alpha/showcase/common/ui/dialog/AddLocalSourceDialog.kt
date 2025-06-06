package com.alpha.showcase.common.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alpha.showcase.common.ui.view.HintText
import com.alpha.showcase.common.utils.checkName
import org.jetbrains.compose.resources.stringResource
import showcaseapp.composeapp.generated.resources.Res
import showcaseapp.composeapp.generated.resources.cancel
import showcaseapp.composeapp.generated.resources.confirm
import showcaseapp.composeapp.generated.resources.local_source_name
import showcaseapp.composeapp.generated.resources.source_name

@Composable
fun AddLocalSource(onCancelClick: () -> Unit, onConfirmClick: (String) -> Unit) {


    var name by rememberSaveable {
        mutableStateOf("")
    }
    var nameValid by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        dialogPaneDescription = stringResource(Res.string.local_source_name),
        onDismissRequest = {
            onCancelClick.invoke()
        }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    label = {
                        Text(
                            text = stringResource(Res.string.source_name),
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    },
                    value = name,
                    onValueChange = {
                        name = it
                        nameValid = it.checkName(it)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    placeholder = { HintText(text = stringResource(Res.string.local_source_name)) },
                    singleLine = true,
                    maxLines = 1,
                    isError = !nameValid
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(
                        onClick = {
                            onCancelClick.invoke()
                        }
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            nameValid = true
                            if (nameValid){
                                onConfirmClick.invoke(name)
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.confirm))
                    }
                }
            }
        }
    }

}

@Composable
fun AlertDialog(
    dialogPaneDescription: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Box(
            modifier = modifier
                .sizeIn(minWidth = DialogMinWidth, maxWidth = DialogMaxWidth)
                .then(Modifier.semantics { paneTitle = dialogPaneDescription }),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp