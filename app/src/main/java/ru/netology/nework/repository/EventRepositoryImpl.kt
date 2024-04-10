package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.User
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.entity.toEntityNew
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val dao: EventDao,
    private val apiService: ApiService,
    private val auth: AppAuth
): EventRepository {
    override val data = dao.getAll()
        .map(List<EventEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = apiService.getAllEvents()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        }
    }

    override suspend fun save(event: Event) {
        try {
            val response = apiService.saveEvent(event.toEventApi())
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(EventEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            e.printStackTrace()
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(
        event: Event,
        upload: MediaUpload,
        attachmentType: AttachmentType
    ) {
        try {
            val media = upload(upload)
            val eventWithAttachment = event.copy(attachment = Attachment(media.url, attachmentType))
            save(eventWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveLocal(event: Event) {
        dao.insert(EventEntity.fromDto(event))
    }

    override suspend fun likeById(event: Event): Event {
        try {
            likeByIdLocal(event)
            val response = if (!event.likedByMe) {
                apiService.likeEventById(event.id)
            } else {
                apiService.dislikeEventById(event.id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            likeByIdLocal(event)
            throw NetworkError
        } catch (e: Exception) {
            likeByIdLocal(event)
            throw UnknownError
        }
    }

    override suspend fun likeByIdLocal(event: Event) {
        return if(event.likedByMe){
            val list = event.likeOwnerIds.filter{
                it != auth.authStateFlow.value.id
            }
            dao.likeById(event.id, list)
        } else{
            val list = event.likeOwnerIds.plus(auth.authStateFlow.value.id)

            dao.likeById(event.id, list)
        }
    }

    override suspend fun participateById(event: Event): Event {
        try {
            participateByIdLocal(event)
            val response = if (!event.participatedByMe) {
                apiService.participateEventById(event.id)
            } else {
                apiService.notParticipateEventById(event.id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            participateByIdLocal(event)
            throw NetworkError
        } catch (e: Exception) {
            participateByIdLocal(event)
            throw UnknownError
        }
    }

    override suspend fun participateByIdLocal(event: Event) {
        return if(event.participatedByMe){
            val list = event.participantsIds.filter{
                it != auth.authStateFlow.value.id
            }
            dao.participateById(event.id, list)
        } else{
            val list = event.participantsIds.plus(auth.authStateFlow.value.id)

            dao.participateById(event.id, list)
        }
    }

    override suspend fun removeById(event: Event) {
        val eventRemoved = event.copy()
        try {
            dao.removeById(event.id)
            val response = apiService.removeEventById(event.id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            saveLocal(eventRemoved)
            throw NetworkError
        } catch (e: Exception) {
            saveLocal(eventRemoved)
            throw UnknownError
        }
    }

    override suspend fun getLikers(event: Event): List<User> {
        var likers = emptyList<User>()
        if(event.likeOwnerIds.isNotEmpty()){
            event.likeOwnerIds.forEach{
                likers = likers.plus(getUser(it))
            }
        }
        return likers
    }

    override suspend fun getSpeakers(event: Event): List<User> {
        var speakers = emptyList<User>()
        if(event.speakerIds.isNotEmpty()){
            event.speakerIds.forEach{
                speakers = speakers.plus(getUser(it))
            }
        }
        return speakers
    }

    override suspend fun getParticipants(event: Event): List<User> {
        var participants = emptyList<User>()
        if(event.participantsIds.isNotEmpty()){
            event.participantsIds.forEach{
                participants = participants.plus(getUser(it))
            }
        }
        return participants
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
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

    override suspend fun getUser(userId: Long): User {
        try {
            val response = apiService.getUserById(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        }
    }

    override suspend fun readNewEvents() {
        dao.readNewEvents()
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = apiService.getNewerEvents(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntityNew())
            emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)
}