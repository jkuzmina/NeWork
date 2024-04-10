package ru.netology.nework.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.db.AppDb
import ru.netology.nework.dto.Job
import ru.netology.nework.model.FeedModel
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.JobRepository
import ru.netology.nework.repository.JobRepositoryImpl
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.SingleLiveEvent
import java.util.Calendar

private val empty = Job(
    id= 0,
    name= "",
    position= "",
    start= AndroidUtils.calendarToUTCDate(Calendar.getInstance()),
    finish= null,
    link= null,
    userId = 0
    )

class JobViewModel @AssistedInject constructor(
    auth: AppAuth,
    @ApplicationContext context: Context,
    apiService: ApiService,
    @Assisted userId: Long,
    ) : ViewModel(){

    @AssistedFactory
    interface Factory {
        fun create(userId: Long): JobViewModel
    }
    companion object{
        fun provideJobViewModelFactory(factory: Factory, userId: Long): ViewModelProvider.Factory{
            return object: ViewModelProvider.Factory{
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return factory.create(userId) as T
                }
            }
        }
    }

    private val repository: JobRepository = JobRepositoryImpl(userId, AppDb.getInstance(context = context).jobDao(), apiService)

    val data: LiveData<FeedModel<Job>> = auth
    .authStateFlow
    .flatMapLatest {auth ->
        repository.data.map{jobs ->
            FeedModel(
                jobs.map { it.copy(ownedByMe = auth.id == it.userId) },
                jobs.none { userId == it.userId }
            )
        }
    }.asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    val newJob = MutableLiveData(empty)

   private val _jobCreated = SingleLiveEvent<Unit>()
    val jobCreated: LiveData<Unit>
        get() = _jobCreated

    fun loadJobs()  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun setName(name: String){
        newJob.value = newJob.value?.copy(name = name)
    }

    fun setPosition(position: String){
        newJob.value = newJob.value?.copy(position = position)
    }

    fun setStart(start: String){
        newJob.value = newJob.value?.copy(start = start)
    }

    fun setFinish(finish: String?){
        newJob.value = newJob.value?.copy(finish = finish)
    }

    fun setLink(link: String?){
        newJob.value = newJob.value?.copy(link = link)
    }

    fun save(){
        newJob.value?.let {
            val job = it.copy()
            _jobCreated.value = Unit
            newJob.value = empty
            viewModelScope.launch {
                try {
                    repository.save(job)
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
    }

    fun removeById(job: Job)  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.removeById(job)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }
}