package ru.netology.nework.activity.posts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.R
import ru.netology.nework.activity.posts.PostDetailsFragment.Companion.longArg
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.util.AndroidUtils.showSignInDialog
import ru.netology.nework.util.MediaLifecycleObserver
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.PostViewModel

class PostsFragment : Fragment() {

    private val postViewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity) {
        PostViewModel.PostViewModelFactory(
            requireActivity().application
        )
    }
    private val mediaObserver = MediaLifecycleObserver()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostsBinding.inflate(inflater, container, false)
        lifecycle.addObserver(mediaObserver)
        val adapter = PostAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                //postViewModel.edit(post)
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment,
                    Bundle().apply{
                        longArg = post.id
                    })
            }

            override fun onLike(post: Post){
                if(authViewModel.authenticated){
                    postViewModel.likeById(post)
                } else showSignInDialog(this@PostsFragment)
            }

            override fun onRemove(post: Post) {
                postViewModel.removeById(post)
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
                findNavController().navigate(R.id.action_postsFragment_to_postDetailsFragment,
                    Bundle().apply{
                    longArg = post.id
                })
            }

            override fun onPlayAudio(post: Post, seekBar: SeekBar, playAudio: ImageButton) {
                mediaObserver.playAudio(post.attachment!!, seekBar, playAudio)
            }

        }, requireContext(), authViewModel.authenticated, mediaObserver)

        binding.list.adapter = adapter

        postViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { postViewModel.loadPosts() }
                    .show()
            }
        }
        postViewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            binding.emptyText.isVisible = state.empty
        }

        postViewModel.newerCount.observe(viewLifecycleOwner) { state ->
            binding.newPosts.isVisible = state > 0
            println(state)
        }

        binding.newPosts.setOnClickListener {
            postViewModel.readNewPosts()
            binding.newPosts.isVisible = false
            binding.list.smoothScrollToPosition(0)
        }

        binding.fab.setOnClickListener {
            if(authViewModel.authenticated){
                postViewModel.edit(null)
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment)
            } else showSignInDialog(this)

        }

        binding.swiperefresh.setOnRefreshListener {
            postViewModel.loadPosts()
            binding.swiperefresh.isRefreshing = false
        }
        return binding.root
    }




}