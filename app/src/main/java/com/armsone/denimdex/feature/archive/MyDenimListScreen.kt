package com.armsone.denimdex.feature.archive

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsone.denimdex.core.design.*
import com.armsone.denimdex.core.model.CollectionItem
import com.armsone.denimdex.core.model.VerificationState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDenimListScreen(
    viewModel: ArchiveViewModel,
    onItemSelected: (CollectionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSyncInvite by viewModel.showSyncInvite.collectAsState()
    val showSyncDisclosure by viewModel.showSyncDisclosure.collectAsState()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkSyncInvitationOnAppear()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(DenimTestTags.ARCHIVE_SCREEN)
            .background(DenimColors.canvas)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Large Title
        Text(
            text = "내 아카이브",
            style = DenimTypography.largeTitle,
            modifier = Modifier
                .testTag(DenimTestTags.ARCHIVE_TITLE)
                .padding(bottom = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DenimTestTags.ARCHIVE_SEARCH_FIELD)
                .padding(bottom = 16.dp),
            placeholder = {
                Text(
                    text = "브랜드, 모델 또는 메모",
                    style = DenimTypography.body.copy(color = DenimColors.inkSoft.copy(alpha = 0.6f))
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = DenimColors.inkSoft
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "검색 지우기")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DenimColors.cardSurface,
                unfocusedContainerColor = DenimColors.cardSurface,
                focusedBorderColor = DenimColors.indigoBright,
                unfocusedBorderColor = DenimColors.hairline
            )
        )

        // Content: List or Empty State
        if (items.isEmpty()) {
            EmptyArchiveView()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(DenimTestTags.ARCHIVE_LIST),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    CollectionItemRow(
                        item = item,
                        onClick = { onItemSelected(item) },
                        onDelete = { viewModel.deleteItem(item.id) }
                    )
                }
            }
        }
    }

    // Sync Invite Sheet
    if (showSyncInvite) {
        SyncInviteSheet(
            onDismiss = { viewModel.onDismissSyncInvite() },
            onStartSync = { viewModel.onAttemptSync() },
            onViewDisclosure = { viewModel.onOpenSyncDisclosure() }
        )
    }

    // Sync Disclosure Sheet
    if (showSyncDisclosure) {
        SyncDisclosureSheet(
            onDismiss = { viewModel.onDismissSyncDisclosure() }
        )
    }

    // Sync Error Alert
    if (syncErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSyncError() },
            title = { Text(text = "동기화 안내", style = DenimTypography.title3) },
            text = { Text(text = syncErrorMessage ?: "", style = DenimTypography.body) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSyncError() }) {
                    Text(text = "확인", style = DenimTypography.headline.copy(color = DenimColors.indigoBright))
                }
            }
        )
    }
}

@Composable
private fun CollectionItemRow(
    item: CollectionItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val firstPhotoUri = item.photoUris.firstOrNull()
    val thumbnailBitmap = remember(firstPhotoUri) {
        if (firstPhotoUri != null) {
            try {
                val path = Uri.parse(firstPhotoUri).path
                if (path != null && File(path).exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(path, opts)
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.archiveItemRow(item.id))
            .denimCard(padding = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail (76x76dp)
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DenimColors.offWhite),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = DenimColors.inkSoft.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.displayTitle,
                style = DenimTypography.headline.copy(fontSize = 16.sp),
                maxLines = 1,
                modifier = Modifier.testTag(DenimTestTags.ARCHIVE_ITEM_TITLE)
            )

            Text(
                text = item.formattedValueRange,
                style = DenimTypography.captionBold.copy(color = DenimColors.indigo),
                modifier = Modifier.testTag(DenimTestTags.ARCHIVE_ITEM_VALUE_RANGE)
            )

            // Verification Badge
            Box(
                modifier = Modifier
                    .testTag(DenimTestTags.ARCHIVE_ITEM_VERIFICATION_BADGE)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (item.verificationState) {
                            VerificationState.USER_CONFIRMED -> DenimColors.successGreen.copy(alpha = 0.12f)
                            VerificationState.SOURCE_VERIFIED -> DenimColors.brass.copy(alpha = 0.15f)
                            VerificationState.AI_ESTIMATE -> DenimColors.fadedDenim
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.verificationState.displayName,
                    style = DenimTypography.captionBold.copy(
                        fontSize = 11.sp,
                        color = when (item.verificationState) {
                            VerificationState.USER_CONFIRMED -> DenimColors.successGreen
                            VerificationState.SOURCE_VERIFIED -> DenimColors.brass
                            VerificationState.AI_ESTIMATE -> DenimColors.inkSoft
                        }
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DenimColors.inkSoft.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyArchiveView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.ARCHIVE_EMPTY_VIEW)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 112x112dp indigoGradient rounded square
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(brush = DenimColors.indigoGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        DenimEyebrow("Private Collection")
        Text(
            text = "첫 데님을 보관해보세요",
            style = DenimTypography.title3.copy(color = DenimColors.charcoal)
        )
        Text(
            text = "사진으로 가치를 확인하고\n당신만의 아카이브를 완성해보세요.",
            style = DenimTypography.body.copy(color = DenimColors.inkSoft),
            textAlign = TextAlign.Center
        )
    }
}
