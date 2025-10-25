package com.letsdoit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.letsdoit.app.R

data class SearchBarState(
    val query: String,
    val history: List<String>,
    val active: Boolean,
    val onQueryChange: (String) -> Unit,
    val onSearch: () -> Unit,
    val onActiveChange: (Boolean) -> Unit,
    val onSelectHistory: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    searchState: SearchBarState? = null,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(contentAlignment = Alignment.TopCenter) {
        CenterAlignedTopAppBar(
            title = { Text(text = title) },
            navigationIcon = { navigation?.invoke() },
            actions = {
                if (searchState != null) {
                    IconButton(onClick = { searchState.onActiveChange(true) }) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = stringResource(id = R.string.search_placeholder))
                    }
                }
                actions()
            }
        )
        if (searchState != null) {
            DockedSearchBar(
                query = searchState.query,
                onQueryChange = searchState.onQueryChange,
                onSearch = { searchState.onSearch() },
                active = searchState.active,
                onActiveChange = searchState.onActiveChange,
                placeholder = { Text(text = stringResource(id = R.string.search_placeholder)) },
                leadingIcon = {
                    if (searchState.active) {
                        IconButton(onClick = { searchState.onActiveChange(false) }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(id = R.string.action_close))
                        }
                    } else {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { searchState.onQueryChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(id = R.string.search_clear))
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                if (searchState.history.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.search_history),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    searchState.history.forEach { item ->
                        ListItem(
                            headlineContent = {
                                Text(text = item, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchState.onSelectHistory(item)
                                    searchState.onActiveChange(false)
                                }
                        )
                    }
                }
            }
        }
    }
}
