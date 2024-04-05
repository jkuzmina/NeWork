package ru.netology.nework.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.netology.nework.db.AppDb
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.PostRepositoryUserWallImpl



class WallViewModel(
    application: Application,
    private val userId: Long,
    private val myId: Long,
    private val repository: PostRepository
) : PostViewModel(application, repository) {

    class WallViewModelFactory(
        private val application: Application,
        private val userId: Long,
        private val myId: Long,
        private val repository: PostRepository = PostRepositoryUserWallImpl(userId, myId, AppDb.getInstance(context = application).postDao(), application))
        : ViewModelProvider.AndroidViewModelFactory(application) {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            if (modelClass.isAssignableFrom(
                    WallViewModel::class.java)) {
                val vm = WallViewModel(
                    application = application,
                    userId = userId,
                    myId = myId,
                    repository = repository
                )
                return vm as T
            }

            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}