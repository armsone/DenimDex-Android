package com.armsone.denimdex.core.sync

import com.armsone.denimdex.core.model.CollectionItem

sealed class SyncClientException(message: String) : Exception(message) {
    object NotConfigured : SyncClientException("현재는 이 기기의 개인 아카이브만 사용합니다. 개인 NAS 연결은 준비 중입니다.")
    object NetworkUnavailable : SyncClientException("네트워크에 연결할 수 없습니다.")
}

interface DenimDexSyncClient {
    suspend fun syncCollection(items: List<CollectionItem>): Result<Unit>
    suspend fun checkConnection(): Boolean
}

class DisabledDenimDexSyncClient : DenimDexSyncClient {
    override suspend fun syncCollection(items: List<CollectionItem>): Result<Unit> {
        return Result.failure(SyncClientException.NotConfigured)
    }

    override suspend fun checkConnection(): Boolean {
        return false
    }
}
