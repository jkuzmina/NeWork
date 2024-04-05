package ru.netology.nework.activity.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.R
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentUserPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.MediaLifecycleObserver
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.UserViewModel
import ru.netology.nework.viewmodel.WallViewModel

class UserPostsFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)
    private val wallViewModel: WallViewModel by viewModels {
        WallViewModel.WallViewModelFactory(
            requireActivity().application,
            userViewModel.selectedUser.value!!,
            authViewModel.data.value?.id ?: -1L
        )
    }
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var binding: FragmentUserPostsBinding
    private val mediaObserver = MediaLifecycleObserver()
    private var postPlaying: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserPostsBinding.inflate(layoutInflater, container, false)
        userViewModel.selectedUser.observe(viewLifecycleOwner) {
            wallViewModel.loadPosts()
        }
        val adapter = PostAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                val request = NavDeepLinkRequest.Builder
                    .fromUri("android-app://newPostFragment?longArg=${post.id}".toUri())
                    .build()
                findNavController().navigate(request)
            }

            override fun onLike(post: Post) {
                if(authViewModel.authenticated){
                    wallViewModel.likeById(post)
                } else AndroidUtils.showSignInDialog(this@UserPostsFragment)
            }

            override fun onRemove(post: Post) {
                wallViewModel.removeById(post)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onItemClick(post: Post) {
                val request = NavDeepLinkRequest.Builder
                    .fromUri("android-app://postDetailsFragment?longArg=${post.id}".toUri())
                    .build()
                findNavController().navigate(request)
            }

            override fun onPlayAudio(post: Post, seekBar: SeekBar, playAudio: ImageButton) {
                mediaObserver.playAudio(post.attachment!!, seekBar, playAudio)
                postPlaying = post
            }


        }, requireContext(), authViewModel.authenticated, mediaObserver)

        binding.list.adapter = adapter

        wallViewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            binding.emptyText.isVisible = state.empty
        }

        wallViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) {
                        wallViewModel.loadPosts()
                    }
                    .show()
            }
        }
        binding.swiperefresh.setOnRefreshListener {
            wallViewModel.loadPosts()
            binding.swiperefresh.isRefreshing = false
        }
        return binding.root
    }


}