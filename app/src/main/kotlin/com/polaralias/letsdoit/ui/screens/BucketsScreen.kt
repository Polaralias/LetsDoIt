package com.polaralias.letsdoit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.ui.viewmodel.BucketsViewModel

@Composable
fun BucketsScreen(viewModel: BucketsViewModel = hiltViewModel()) {
    val lists by viewModel.lists.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (lists.isEmpty()) {
            Text(text = stringResource(id = R.string.message_empty_buckets), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(lists, key = { it.id }) { list ->
                    BucketCard(list)
                }
            }
        }
    }
}

@Composable
internal fun BucketCard(entity: ListEntity) {
    val spaceLabel = stringResource(id = R.string.bucket_space_label, entity.spaceId)
    val summary = stringResource(id = R.string.accessibility_bucket_summary, entity.name, spaceLabel)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = summary
            }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = entity.name, style = MaterialTheme.typography.titleMedium)
            Text(text = spaceLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}
