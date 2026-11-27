package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val activeProfile: String = "Family",
    val appLanguage: String = "bn",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudFavoriteDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profileName: String = "Family",
    val channelUrl: String = "",
    val channelName: String = "",
    val groupTitle: String = "Other",
    val logoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudChannelDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val channelName: String = "",
    val streamUrl: String = "",
    val groupTitle: String = "Other",
    val logoUrl: String = "",
    val tvgId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudPlaylistDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val playlistUrl: String = "",
    val channelCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface CloudSyncUiState {
    data object Loading : CloudSyncUiState
    data class Synced(
        val favoritesCount: Int,
        val customChannelsCount: Int,
        val playlistsCount: Int,
        val lastSyncedAtMillis: Long = System.currentTimeMillis()
    ) : CloudSyncUiState
    data class Error(val message: String) : CloudSyncUiState
}
