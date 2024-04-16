package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dao.WallRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toEntity
import ru.netology.nework.entity.toEntityNew
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException

class PostRepositoryUserWallImpl (
    appDb: AppDb,
    private val dao: PostDao,
    private val apiService: ApiService,
    private val auth: AppAuth,
    private val userId: Long,
    wallRemoteKeyDao: WallRemoteKeyDao,
    ) :
    PostRepositoryBaseImpl(dao, apiService) {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 25),
        remoteMediator = WallRemoteMediator(apiService, appDb, dao, wallRemoteKeyDao, auth, userId),
        pagingSourceFactory = {
            dao.pagingSourceUserWall(userId)
        }
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }


    override suspend fun getAll() {
        try {
            val response =
                if(isMyWall()){
                    apiService.getMyWall()
                } else{
                    apiService.getUserWall(userId)
                }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(post: Post): Post {
        try {
            likeByIdLocal(post)
            val response = if (!post.likedByMe) {
                apiService.likeUserPostById(userId, post.id)
            } else {
                apiService.dislikeUserPostById(userId, post.id)
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
                it != auth.authStateFlow.value.id
            }
            dao.likeById(post.id, list)
        } else{
            val list = post.likeOwnerIds.plus(auth.authStateFlow.value.id)

            dao.likeById(post.id, list)
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response =
                if(isMyWall()){
                    apiService.getMyWallNewer(id)
                } else{
                    apiService.getUserWallNewer(userId, id)
                }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            //записываем новые посты с признаком read = false
            dao.insert(body.toEntityNew())
            emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun latestReadPostId(): Long {
        return dao.latestUserReadPostId(userId) ?: 0L
    }


    fun isMyWall(): Boolean{
        return userId == auth.authStateFlow.value.id
    }
}