package ru.netology.nework.activity.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.netology.nework.R
import ru.netology.nework.adapter.FragmentPageAdapter
import ru.netology.nework.databinding.FragmentUserWallBinding
import ru.netology.nework.util.LongArg
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.UserViewModel


class UserWallFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: FragmentPageAdapter
    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var binding: FragmentUserWallBinding

    companion object {
        var Bundle.longArg: Long? by LongArg
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserWallBinding.inflate(layoutInflater, container, false)

        val collapsingToolbarLayout = binding.collapsingToolbar
        val toolbar = binding.toolbar

        val navController = findNavController()

        val appBarConfiguration= AppBarConfiguration(navController.graph)

        setupWithNavController(
            toolbar, navController, appBarConfiguration
        )

        setupWithNavController(collapsingToolbarLayout, toolbar, navController)
        val userId = arguments?.longArg ?: -1
        userViewModel.selectUser(userId)
        userViewModel.data.observe(viewLifecycleOwner) { users ->
            val user = users.find { it.id == userId }
            if(user != null){
                binding.apply {
                    Glide.with(avatar)
                        .load(user.avatar)
                        .placeholder(R.drawable.ic_loading_100dp)
                        .error(R.drawable.ic_error_100dp)
                        .centerCrop()
                        .timeout(10_000)
                        .into(binding.avatar)
                }
                collapsingToolbarLayout.isTitleEnabled = true
                collapsingToolbarLayout.title = "${user.name}/${user.login}"
        }

        }
        tabLayout = binding.tabs
        viewPager2 = binding.viewPager
        adapter = FragmentPageAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        tabLayout.addTab(tabLayout.newTab().setText("Wall"))
        tabLayout.addTab(tabLayout.newTab().setText("Jobs"))

        viewPager2.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager2){tab, index ->
            when(index){
                0 -> {
                    tab.text = "Wall"
                    binding.fab.visibility = View.GONE
                }
                1 -> {
                    tab.text = "Jobs"
                    binding.fab.visibility = View.VISIBLE
                }
                else -> Unit
            }

            //userViewModel.selectedUser.value == authViewModel.data.value?.id
        }.attach()
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when(position){
                    0 -> {
                        binding.fab.visibility = View.GONE
                    }
                    1 -> {
                        binding.fab.visibility = if(userId == authViewModel.data.value?.id) View.VISIBLE else View.GONE
                    }
                    else -> Unit
                }
            }
        })
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_userWallFragment_to_newJobFragment)
        }
        return binding.root
    }

}