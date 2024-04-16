package ru.netology.nework.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.db.AppDb
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.PostRepositoryUserWallImpl



class WallViewModel @AssistedInject constructor(
    auth: AppAuth,
    @ApplicationContext context: Context,
    @Assisted private val userId: Long,
    apiService: ApiService,
) : PostViewModel(auth, context, apiService) {

    @AssistedFactory
    interface Factory {
        fun create(userId: Long): WallViewModel
    }
    companion object{
        fun provideWallViewModelFactory(factory: Factory, userId: Long): ViewModelProvider.Factory{
            return object: ViewModelProvider.Factory{
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return factory.create(userId) as T
                }
            }
        }
    }

    override val repository: PostRepository = PostRepositoryUserWallImpl(
        AppDb.getInstance(context),
        AppDb.getInstance(context).postDao(),
        apiService,
        auth,
        userId,
        AppDb.getInstance(context).wallRemoteKeyDao(),
    )

}