package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.PlaylistEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<CloudUserDoc?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(CloudUserDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeFavorites(userId: String): Flow<List<CloudFavoriteDoc>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudFavoriteDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeCustomChannels(userId: String): Flow<List<CloudChannelDoc>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/channels"
        emitAll(
            db.collection("users").document(userId).collection("channels")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudChannelDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePlaylists(userId: String): Flow<List<CloudPlaylistDoc>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/playlists"
        emitAll(
            db.collection("users").document(userId).collection("playlists")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudPlaylistDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(
        displayName: String,
        activeProfile: String,
        appLanguage: AppLanguage
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanProfile = activeProfile.trim().ifEmpty { "Family" }.take(50)
            val cleanName = displayName.trim().take(80)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "activeProfile" to cleanProfile,
                    "appLanguage" to appLanguage.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanName.isNotEmpty()) {
                    updates["displayName"] = cleanName
                }
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "activeProfile" to cleanProfile,
                    "appLanguage" to appLanguage.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanName.isNotEmpty()) {
                    payload["displayName"] = cleanName
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertFavorite(profileName: String, channel: ChannelEntity): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/favorites"
        return try {
            val uid = requireUserId()
            val cleanProfile = profileName.trim().ifEmpty { "Family" }.take(50)
            val cleanUrl = channel.url.trim().take(1000)
            val cleanName = channel.name.trim().ifEmpty { "Channel" }.take(120)
            require(cleanUrl.isNotEmpty()) { "Channel URL must not be blank" }

            val docId = stableDocId("fav", "$cleanProfile|$cleanUrl")
            val docRef = db.collection("users").document(uid).collection("favorites").document(docId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "profileName" to cleanProfile,
                        "channelUrl" to cleanUrl,
                        "channelName" to cleanName,
                        "groupTitle" to channel.groupTitle.trim().take(80),
                        "logoUrl" to channel.logoUrl.trim().take(1000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "profileName" to cleanProfile,
                        "channelUrl" to cleanUrl,
                        "channelName" to cleanName,
                        "groupTitle" to channel.groupTitle.trim().take(80),
                        "logoUrl" to channel.logoUrl.trim().take(1000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(profileName: String, channelUrl: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/favorites"
        return try {
            val uid = requireUserId()
            val cleanProfile = profileName.trim().ifEmpty { "Family" }.take(50)
            val cleanUrl = channelUrl.trim().take(1000)
            val docId = stableDocId("fav", "$cleanProfile|$cleanUrl")
            val docRef = db.collection("users").document(uid).collection("favorites").document(docId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertCustomChannel(channel: ChannelEntity): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/channels"
        return try {
            val uid = requireUserId()
            val cleanName = channel.name.trim().ifEmpty { "Channel" }.take(120)
            val cleanUrl = channel.url.trim().take(1000)
            val cleanGroup = channel.groupTitle.trim().ifEmpty { "Other" }.take(80)
            require(cleanUrl.isNotEmpty()) { "Stream URL must not be blank" }

            val docId = stableDocId("ch", cleanUrl)
            val docRef = db.collection("users").document(uid).collection("channels").document(docId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "channelName" to cleanName,
                        "streamUrl" to cleanUrl,
                        "groupTitle" to cleanGroup,
                        "logoUrl" to channel.logoUrl.trim().take(1000),
                        "tvgId" to channel.tvgId.trim().take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "channelName" to cleanName,
                        "streamUrl" to cleanUrl,
                        "groupTitle" to cleanGroup,
                        "logoUrl" to channel.logoUrl.trim().take(1000),
                        "tvgId" to channel.tvgId.trim().take(120),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeCustomChannel(channelUrl: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/channels"
        return try {
            val uid = requireUserId()
            val docId = stableDocId("ch", channelUrl.trim().take(1000))
            val docRef = db.collection("users").document(uid).collection("channels").document(docId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertPlaylist(playlist: PlaylistEntity): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/playlists"
        return try {
            val uid = requireUserId()
            val cleanUrl = playlist.url.trim().take(1000)
            val cleanTitle = playlist.title.trim().ifEmpty { "M3U Playlist" }.take(120)
            val safeCount = playlist.channelCount.coerceIn(0, 100_000).toLong()
            require(cleanUrl.isNotEmpty()) { "Playlist URL must not be blank" }

            val docId = stableDocId("pl", cleanUrl)
            val docRef = db.collection("users").document(uid).collection("playlists").document(docId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "playlistUrl" to cleanUrl,
                        "channelCount" to safeCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "playlistUrl" to cleanUrl,
                        "channelCount" to safeCount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removePlaylist(playlistUrl: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/playlists"
        return try {
            val uid = requireUserId()
            val docId = stableDocId("pl", playlistUrl.trim().take(1000))
            val docRef = db.collection("users").document(uid).collection("playlists").document(docId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    companion object {
        fun stableDocId(prefix: String, rawKey: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            return "${prefix}_${digest.take(32)}"
        }
    }
}
