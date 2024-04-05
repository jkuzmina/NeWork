package ru.netology.nework.repository

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import ru.netology.nework.R
import ru.netology.nework.api.Api
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.UserAvatar
import java.io.IOException


open class PostRepositoryImpl(
    private val dao: PostDao,
    private val application: Application
): PostRepository {


    override val data = dao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default) //разгружаем главный поток


    override suspend fun getAll() {
        try {
            val response = Api.retrofitService.getAllPosts()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = Api.retrofitService.savePost(post.toPostApi())
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            e.printStackTrace()
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(
        post: Post,
        upload: MediaUpload,
        attachmentType: AttachmentType
    ) {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(attachment = Attachment(media.url, attachmentType))
            save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = Api.retrofitService.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun readNewPosts() {
        dao.readNewPosts()
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
                delay(120_000L)
                val response = Api.retrofitService.getNewer(id)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }

                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insert(body.toEntity())
                emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)


    override suspend fun getPostById(post: Post): Post {
        try {
            val response = Api.retrofitService.getPostById(post.id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun saveLocal(post: Post) {
        dao.insert(PostEntity.fromDto(post))
    }

    override suspend fun likeById(post: Post) : Post {
        try {
            likeByIdLocal(post)
            val response = if (!post.likedByMe) {
                Api.retrofitService.likePostById(post.id)
            } else {
                Api.retrofitService.dislikePostById(post.id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            likeByIdLocal(post)
            throw NetworkError
        } catch (e: Exception) {
            likeByIdLocal(post)
            throw UnknownError
        }
    }

    override suspend fun likeByIdLocal(post: Post) {
        return if(post.likedByMe){
            val list = post.likeOwnerIds.filter{
                it != AppAuth.getInstance().authStateFlow.value.id
            }
            dao.likeById(post.id, list)
        } else{
            val list = post.likeOwnerIds.plus(AppAuth.getInstance().authStateFlow.value.id)

            dao.likeById(post.id, list)
        }

    }

    override suspend fun removeById(post: Post) {
        val postRemoved = post.copy()
        try {
            dao.removeById(post.id)
            val response = Api.retrofitService.removePostById(post.id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            saveLocal(postRemoved)
            throw NetworkError
        } catch (e: Exception) {
            saveLocal(postRemoved)
            throw UnknownError
        }
    }

    override suspend fun getLikersAvatars(post: Post): Set<UserAvatar>{
        var setAvatars = emptySet<UserAvatar>()
        if(post.likeOwnerIds.isNotEmpty()){
            post.likeOwnerIds.forEach{
                setAvatars = setAvatars.plus(UserAvatar(it, getUserAvatar(it)))
            }
        }
        return setAvatars
    }

    override suspend fun getMentionedAvatars(post: Post): Set<UserAvatar> {
        var setAvatars = emptySet<UserAvatar>()
        if(post.mentionIds.isNotEmpty()){
            post.mentionIds.forEach{
                setAvatars = setAvatars.plus(UserAvatar(it, getUserAvatar(it)))
            }
        }
        return setAvatars
    }

    override suspend fun getLikers(post: Post): List<User> {
        var likers = emptyList<User>()
        if(post.likeOwnerIds.isNotEmpty()){
            post.likeOwnerIds.forEach{
                likers = likers.plus(getUser(it))
            }
        }
        return likers
    }

    override suspend fun getMentioned(post: Post): List<User> {
        var mentioned = emptyList<User>()
        if(post.mentionIds.isNotEmpty()){
            post.mentionIds.forEach{
                mentioned = mentioned.plus(getUser(it))
            }
        }
        return mentioned
    }

    override suspend fun getUserAvatar(userId: Long): String {
        try {
            val response = Api.retrofitService.getUserById(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            if(body.avatar == null){
                return ""
            }
            return body.avatar
        } catch (e: IOException) {
            throw NetworkError
        }
    }
    override suspend fun getUser(userId: Long): User {
        try {
            val response = Api.retrofitService.getUserById(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        }
    }

    fun <T : Any?> errorDescription(response: Response<T>):String = application.getString(R.string.error_text, response.code().toString(), response.message())

}