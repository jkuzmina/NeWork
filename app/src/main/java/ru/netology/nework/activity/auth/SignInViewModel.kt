package ru.netology.nework.activity.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nework.api.Api
import ru.netology.nework.auth.AuthState
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.model.UserAuthResult
import ru.netology.nework.util.SingleLiveEvent
import java.io.IOException

class SignInViewModel : ViewModel() {
    val login: MutableLiveData<String> = MutableLiveData<String>()
    val pass: MutableLiveData<String> = MutableLiveData<String>()


    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState>
        get() = _authState
    private val _userAuthResult = SingleLiveEvent<UserAuthResult>()
    val userAuthResult: LiveData<UserAuthResult>
        get() = _userAuthResult

    fun signIn() = viewModelScope.launch{
        try {
            val authResult = updateUser(login.value!!.toString().trim(), pass.value!!.toString().trim())
            _authState.value = authResult
            _userAuthResult.value = UserAuthResult()
        }
        catch(apiException: ApiError){
            _userAuthResult.value = UserAuthResult(error = true, "${apiException.status}:${apiException.code}")
        }
        catch (e: Exception) {
            _userAuthResult.value = UserAuthResult(error = true)
        }
    }

    private suspend fun updateUser(login: String, pass: String): AuthState {
        try {
            val response = Api.retrofitService.updateUser(login, pass)
            if (!response.isSuccessful) {
                var message = ""
                message = when(response.code()){
                    400 -> "Wrong password"
                    404 -> "User is not registered"
                    else -> response.message()
                }
                throw ApiError(response.code(), message)
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        }
    }
}