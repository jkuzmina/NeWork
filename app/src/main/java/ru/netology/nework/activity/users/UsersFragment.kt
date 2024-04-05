package ru.netology.nework.activity.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.R
import ru.netology.nework.activity.posts.PostDetailsFragment.Companion.longArg
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.dto.User
import ru.netology.nework.viewmodel.UserViewModel

class UsersFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentUsersBinding.inflate(inflater, container, false)
        //userViewModel.loadUsers()
        val adapter = UserAdapter(object : OnInteractionListener {
            override fun onUserClick(user: User) {
                //userViewModel.selectUser(user)
                findNavController().navigate(
                    R.id.action_usersFragment_to_userWallFragment,
                    Bundle().apply{
                        longArg = user.id
                    })
            }

        }, requireContext())

        binding.list.adapter = adapter

        userViewModel.data.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        userViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { userViewModel.loadUsers() }
                    .show()
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            userViewModel.loadUsers()
            binding.swiperefresh.isRefreshing = false
        }
        return binding.root
    }

}