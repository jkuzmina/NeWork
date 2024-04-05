package ru.netology.nework.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.db.AppDb
import ru.netology.nework.dto.User
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.repository.UserRepositoryImpl

class UserViewModel (application: Application) : AndroidViewModel(application){
    private val repository: UserRepository =
        UserRepositoryImpl(AppDb.getInstance(context = application).userDao())

    val data: LiveData<List<User>> = repository.data
        .asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    val selectedUser = MutableLiveData(-1L)

    init {
        loadUsers()
    }

    fun loadUsers()  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun selectUser(userId: Long){
        selectedUser.value = userId
    }

}