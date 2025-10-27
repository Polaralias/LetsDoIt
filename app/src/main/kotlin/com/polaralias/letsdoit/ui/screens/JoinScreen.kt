package com.polaralias.letsdoit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.ui.viewmodel.JoinEvent
import com.polaralias.letsdoit.ui.viewmodel.JoinViewModel
import kotlinx.coroutines.launch

@Composable
fun JoinScreen(initialLink: String?, viewModel: JoinViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(initialLink) {
        viewModel.handleInitialLink(initialLink)
    }
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is JoinEvent.Message -> snackbarHost.showSnackbar(event.text)
            }
        }
    }
    val scope = rememberCoroutineScope()
    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(id = R.string.join_title), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = state.linkInput,
                onValueChange = viewModel::onLinkChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.join_link_label)) },
                placeholder = { Text(text = stringResource(id = R.string.join_link_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true
            )
            if (state.errorMessage != null) {
                Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            if (state.processing) {
                CircularProgressIndicator()
            }
            Button(onClick = viewModel::submit, enabled = state.linkInput.isNotBlank() && !state.processing) {
                Text(text = stringResource(id = R.string.join_action))
            }
            TextButton(onClick = {
                scope.launch {
                    snackbarHost.showSnackbar(stringResource(id = R.string.join_scan))
                }
            }) {
                Text(text = stringResource(id = R.string.join_scan))
            }
            state.joined?.let {
                Text(text = stringResource(id = R.string.join_success))
            }
        }
    }
}
