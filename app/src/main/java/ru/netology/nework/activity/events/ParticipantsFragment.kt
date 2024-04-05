package ru.netology.nework.activity.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentParticipantsBinding
import ru.netology.nework.viewmodel.EventViewModel

class ParticipantsFragment : Fragment() {

    //private val eventViewModel: EventViewModel by navGraphViewModels(R.id.navigationEvents)
    private val eventViewModel: EventViewModel by viewModels(ownerProducer = ::requireActivity)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentParticipantsBinding.inflate(inflater, container, false)
        val adapter = UserAdapter(object : OnInteractionListener {
            /*override fun onUserClick(user: User) {
                findNavController().navigate(
                    R.id.action_usersFragment_to_userWallFragment)
            }*/

        }, requireContext())

        binding.list.adapter = adapter

        eventViewModel.participants.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }
        return binding.root
    }


}