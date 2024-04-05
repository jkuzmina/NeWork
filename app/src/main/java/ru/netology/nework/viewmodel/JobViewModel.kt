package ru.netology.nework.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
class JobViewModel(application: Application, userId: Long) : AndroidViewModel(application){
    class JobViewModelFactory(
        private val app: Application,
        private val userId: Long)
        : ViewModelProvider.AndroidViewModelFactory(app) {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            if (modelClass.isAssignableFrom(
                    JobViewModel::class.java)) {

                return JobViewModel(app, userId) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val repository: JobRepository = JobRepositoryImpl(userId, AppDb.getInstance(context = application).jobDao())

    val data: LiveData<FeedModel<Job>> = AppAuth.getInstance()
    .authStateFlow
    .flatMapLatest {auth ->
        repository.data.map{jobs ->
            FeedModel(
                jobs.map { it.copy(ownedByMe = auth.id == it.userId) },
                jobs.none { userId == it.userId }
            )
        }
    }.asLiveData(Dispatchers.Default)
    //val data: LiveData<List<Job>> = repository.data
        //.asLiveData(Dispatchers.Default)

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