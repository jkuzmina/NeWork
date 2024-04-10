package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException

/*interface PostRepositoryUserWallAssistedFactory {
    fun create(userId: Long): PostRepositoryUserWallImpl
}*/

class PostRepositoryUserWallImpl (
    private val userId: Long,
    private val dao: PostDao,
    apiService: ApiService,
    auth: AppAuth
    ) :
    PostRepositoryImpl(dao, apiService, auth) {


    override val data = dao.getUserWall(userId)
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override suspend fun getAll() {
        try {
            val response =
                if(userId == auth.authStateFlow.value.id){
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


}