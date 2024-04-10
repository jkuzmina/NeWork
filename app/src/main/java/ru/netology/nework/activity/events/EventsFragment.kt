package ru.netology.nework.activity.events

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
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.activity.posts.PostDetailsFragment.Companion.longArg
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.adapter.EventOnInteractionListener
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.util.AndroidUtils.showSignInDialog
import ru.netology.nework.util.MediaLifecycleObserver
import ru.netology.nework.viewmodel.EventViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    private val eventViewModel: EventViewModel by viewModels(ownerProducer = ::requireActivity)
    private val mediaObserver = MediaLifecycleObserver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEventsBinding.inflate(inflater, container, false)
        lifecycle.addObserver(mediaObserver)
        val adapter = EventAdapter.EventAdapter(object : EventOnInteractionListener {
            override fun onEdit(event: Event) {
                eventViewModel.edit(event)
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            }

            override fun onLike(event: Event) {
                if(auth.authenticated()){
                    eventViewModel.likeById(event)
                } else showSignInDialog(this@EventsFragment)
            }

            override fun onParticipate(event: Event) {
                if(auth.authenticated()){
                    eventViewModel.participateById(event)
                } else showSignInDialog(this@EventsFragment)
            }

            override fun onRemove(event: Event) {
                eventViewModel.removeById(event)
            }

            override fun onShare(event: Event) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, event.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onItemClick(event: Event) {
                findNavController().navigate(
                    R.id.action_eventsFragment_to_eventDetailsFragment,
                    Bundle().apply {
                        longArg = event.id
                    })
            }

            override fun onPlayAudio(event: Event, seekBar: SeekBar, playAudio: ImageButton) {
                mediaObserver.playAudio(event.attachment!!, seekBar, playAudio)
            }


        }, requireContext(), auth.authenticated(), mediaObserver)

        binding.list.adapter = adapter

        eventViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { eventViewModel.loadEvents() }
                    .show()
            }
        }
        eventViewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            binding.emptyText.isVisible = state.empty
        }

        eventViewModel.newerCount.observe(viewLifecycleOwner) { state ->
            binding.newEvents.isVisible = state > 0
            println(state)
        }

        binding.newEvents.setOnClickListener {
            eventViewModel.readNewEvents()
            binding.newEvents.isVisible = false
            binding.list.smoothScrollToPosition(0)
        }

        binding.fab.setOnClickListener {
            if(auth.authenticated()){
                eventViewModel.edit(null)
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            } else showSignInDialog(this)

        }

        binding.swiperefresh.setOnRefreshListener {
            eventViewModel.loadEvents()
            binding.swiperefresh.isRefreshing = false
        }
        return binding.root
    }

    }
