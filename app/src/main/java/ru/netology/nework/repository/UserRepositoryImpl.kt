package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.UserDao
import ru.netology.nework.entity.UserEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
    private val apiService: ApiService,
): UserRepository{

    override val data = dao.getAll()
        .map(List<UserEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override suspend fun getAll() {
        try {
            val response = apiService.getAllUsers()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        }
    }


}