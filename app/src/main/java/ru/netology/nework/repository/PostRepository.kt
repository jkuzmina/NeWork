package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.model.UserAvatar

interface PostRepository {

    val data: Flow<List<Post>>
    suspend fun getAll()
    suspend fun save(post: Post)
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType)
    suspend fun saveLocal(post: Post)
    suspend fun getPostById(post: Post): Post
    suspend fun likeById(post: Post): Post
    suspend fun likeByIdLocal(post: Post)
    suspend fun removeById(post: Post)
    suspend fun getUserAvatar(userId: Long):String
    suspend fun getLikersAvatars(post: Post): Set<UserAvatar>
    suspend fun getMentionedAvatars(post: Post): Set<UserAvatar>
    suspend fun getUser(userId: Long): User
    suspend fun getLikers(post: Post): List<User>
    suspend fun getMentioned(post: Post): List<User>
    suspend fun upload(upload: MediaUpload): Media
    suspend fun readNewPosts()
    fun getNewerCount(id: Long): Flow<Int>
}