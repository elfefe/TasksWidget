package com.elfefe.common.ui.view

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elfefe.common.model.Task

@Composable
fun ColumnScope.TasksList(tasks: List<Task>, listState: LazyListState, showDescription: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f),
            state = listState
        ) {
            items(tasks, key = { it.created }) { task ->
                TaskCard(Modifier, task, showDescription)
            }
        }
    }
}