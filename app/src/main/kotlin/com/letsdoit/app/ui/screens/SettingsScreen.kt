package com.letsdoit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.ui.theme.AccentPack
import com.letsdoit.app.ui.theme.CardShapeFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val clickUpToken by viewModel.clickUpToken.collectAsState()
    val openAiKey by viewModel.openAiKey.collectAsState()
    val theme by viewModel.theme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(id = R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = clickUpToken,
            onValueChange = viewModel::onClickUpTokenChanged,
            label = { Text(text = stringResource(id = R.string.label_clickup_token)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Button(onClick = { viewModel.saveClickUpToken() }) {
            Text(text = stringResource(id = R.string.action_save))
        }
        OutlinedTextField(
            value = openAiKey,
            onValueChange = viewModel::onOpenAiKeyChanged,
            label = { Text(text = stringResource(id = R.string.label_openai_key)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Button(onClick = { viewModel.saveOpenAiKey() }) {
            Text(text = stringResource(id = R.string.action_save))
        }
        Text(text = stringResource(id = R.string.label_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_palette), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PaletteFamily.entries.forEach { palette ->
                FilterChip(
                    selected = palette == theme.paletteFamily,
                    onClick = { viewModel.updatePalette(palette) },
                    label = { Text(text = palette.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
        Text(text = stringResource(id = R.string.label_accent_pack), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentPack.entries.forEach { accent ->
                FilterChip(
                    selected = accent == theme.accentPack,
                    onClick = { viewModel.updateAccent(accent) },
                    label = { Text(text = accent.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
        Text(text = stringResource(id = R.string.label_shape), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardShapeFamily.entries.forEach { shape ->
                FilterChip(
                    selected = shape == theme.cardShapeFamily,
                    onClick = { viewModel.updateShape(shape) },
                    label = { Text(text = shape.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
    }
}

