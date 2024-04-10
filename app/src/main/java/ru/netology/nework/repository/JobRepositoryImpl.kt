package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.JobDao
import ru.netology.nework.dto.Job
import ru.netology.nework.entity.JobEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException

/*interface JobRepositoryAssistedFactory {
    fun create(userId: Long): JobRepositoryImpl
}*/
class JobRepositoryImpl(
    private val userId: Long,
    private val dao: JobDao,
    private val apiService: ApiService,
    ): JobRepository {

    override val data = dao.getAll(userId)
        .map(List<JobEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override suspend fun getAll(){
        try {
            val response = apiService.getUserJobs(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity(userId))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(job: Job) {
        val jobRemoved = job.copy()
        try {
            dao.removeById(job.id)
            val response = apiService.removeJobById(job.id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            saveLocal(jobRemoved)
            throw NetworkError
        } catch (e: Exception) {
            saveLocal(jobRemoved)
            throw UnknownError
        }
    }

    override suspend fun save(job: Job) {
        try {
            val response = apiService.saveJob(job)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(JobEntity.fromDto(body, userId))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            e.printStackTrace()
            throw UnknownError
        }
    }

    override suspend fun saveLocal(job: Job) {
        dao.insert(JobEntity.fromDto(job, userId))
    }
}

