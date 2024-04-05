package ru.netology.nework.repository

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.Api
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

class PostRepositoryUserWallImpl(private val userId: Long, private val myId: Long, private val dao: PostDao, application: Application) :
    PostRepositoryImpl(dao, application) {

    override val data = dao.getUserWall(userId)
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override suspend fun getAll() {
        try {
            val response =
                if(userId == myId){
                    Api.retrofitService.getMyWall()
                } else{
                    Api.retrofitService.getUserWall(userId)
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
                Api.retrofitService.likeUserPostById(userId, post.id)
            } else {
                Api.retrofitService.dislikeUserPostById(userId, post.id)
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


}