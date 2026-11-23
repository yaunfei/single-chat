package com.yaunfei.singlechat.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE peerId = :peerId LIMIT 1")
    suspend fun findByPeerId(peerId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query(
        "UPDATE conversations SET lastMessage = :preview, lastMessageAt = :at, updatedAt = :at " +
            "WHERE id = :id",
    )
    suspend fun updatePreview(id: String, preview: String, at: Long)

    @Query(
        "UPDATE conversations SET lastMessage = :preview, lastMessageAt = :at, updatedAt = :at, " +
            "unreadCount = unreadCount + :unreadDelta WHERE id = :id",
    )
    suspend fun applyIncoming(id: String, preview: String, at: Long, unreadDelta: Int)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnread(id: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}
