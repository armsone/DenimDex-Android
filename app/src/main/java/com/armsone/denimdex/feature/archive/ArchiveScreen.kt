package com.armsone.denimdex.feature.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimTypography

/**
 * Manages archive list <-> detail navigation. On expanded-width layouts (tablet/TV)
 * the list and detail are shown side by side (master-detail); on compact width the
 * detail screen replaces the list.
 */
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    isExpandedWidth: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedItem by viewModel.selectedItem.collectAsState()

    if (isExpandedWidth) {
        Row(modifier = modifier.fillMaxSize()) {
            MyDenimListScreen(
                viewModel = viewModel,
                onItemSelected = { viewModel.selectItem(it) },
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(DenimColors.canvas)
            ) {
                if (selectedItem != null) {
                    CollectionItemDetailScreen(
                        item = selectedItem!!,
                        onBack = { viewModel.clearSelectedItem() },
                        onUpdate = { viewModel.updateItem(it) },
                        onDelete = { viewModel.deleteItem(it) }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "왼쪽 목록에서 데님을 선택하면\n상세 정보가 여기에 표시됩니다.",
                            style = DenimTypography.body.copy(color = DenimColors.inkSoft),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        if (selectedItem == null) {
            MyDenimListScreen(
                viewModel = viewModel,
                onItemSelected = { viewModel.selectItem(it) },
                modifier = modifier
            )
        } else {
            BackHandler { viewModel.clearSelectedItem() }
            CollectionItemDetailScreen(
                item = selectedItem!!,
                onBack = { viewModel.clearSelectedItem() },
                onUpdate = { viewModel.updateItem(it) },
                onDelete = { viewModel.deleteItem(it) },
                modifier = modifier
            )
        }
    }
}
