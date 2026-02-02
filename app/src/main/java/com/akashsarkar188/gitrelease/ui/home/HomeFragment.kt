package com.akashsarkar188.gitrelease.ui.home

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.akashsarkar188.gitrelease.R
import com.akashsarkar188.gitrelease.data.local.AppDatabase
import com.akashsarkar188.gitrelease.data.manager.ApkDownloadService
import com.akashsarkar188.gitrelease.data.manager.UpdateManager
import com.akashsarkar188.gitrelease.data.repository.AppRepository
import com.akashsarkar188.gitrelease.databinding.FragmentHomeBinding
import com.akashsarkar188.gitrelease.ui.settings.SettingsFragment
import com.google.android.material.snackbar.Snackbar

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: AppAdapter
    
    private var isFabMenuOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        val db = AppDatabase.getDatabase(requireContext())
        val repository = AppRepository(db.trackedAppDao(), db.githubTokenDao())
        val updateManager = UpdateManager(requireContext())
        val downloadService = ApkDownloadService(requireContext())
        viewModel = HomeViewModel(repository, updateManager, downloadService)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFabMenu()
        setupObservers()
        
        // Trigger initial refresh only once
        if (viewModel.appStates.value.isNullOrEmpty()) {
            viewModel.refreshApps()
        }
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(
            onDeleteClick = { app ->
                Log.d(TAG, "Delete clicked for: ${app.appName}")
                viewModel.deleteApp(app)
            },
            onDownloadClick = { app, track ->
                Log.d(TAG, "Download clicked for: ${app.appName} - ${track.trackType}")
                viewModel.downloadTrack(app, track)
            }
        )
        binding.rvApps.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_blue)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_dark)
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Pull to refresh triggered")
            viewModel.refreshApps()
        }
    }

    private fun setupFabMenu() {
        // Main FAB click - toggle menu
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }
        
        // Overlay click - close menu
        binding.fabOverlay.setOnClickListener {
            closeFabMenu()
        }
        
        // Add Repo FAB
        binding.fabAddRepo.setOnClickListener {
            closeFabMenu()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }
        
        // Clear Downloads FAB
        binding.fabClearDownloads.setOnClickListener {
            closeFabMenu()
            viewModel.clearDownloads()
        }
    }
    
    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }
    
    private fun openFabMenu() {
        isFabMenuOpen = true
        
        // Rotate main FAB
        ObjectAnimator.ofFloat(binding.fabMain, "rotation", 0f, 90f).apply {
            duration = 200
            start()
        }
        
        // Show overlay
        binding.fabOverlay.visibility = View.VISIBLE
        binding.fabOverlay.alpha = 0f
        binding.fabOverlay.animate().alpha(1f).setDuration(200).start()
        
        // Animate sub FABs
        animateSubFab(binding.fabAddContainer, 0)
        animateSubFab(binding.fabClearContainer, 50)
    }
    
    private fun closeFabMenu() {
        isFabMenuOpen = false
        
        // Rotate main FAB back
        ObjectAnimator.ofFloat(binding.fabMain, "rotation", 90f, 0f).apply {
            duration = 200
            start()
        }
        
        // Hide overlay
        binding.fabOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            _binding?.fabOverlay?.visibility = View.GONE
        }.start()
        
        // Hide sub FABs
        hideSubFab(binding.fabAddContainer)
        hideSubFab(binding.fabClearContainer)
    }
    
    private fun animateSubFab(container: View, delay: Long) {
        container.visibility = View.VISIBLE
        container.alpha = 0f
        container.translationY = 50f
        container.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .start()
    }
    
    private fun hideSubFab(container: View) {
        container.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(150)
            .withEndAction {
                _binding?.let {
                    container.visibility = View.GONE
                }
            }
            .start()
    }

    private fun setupObservers() {
        // Observe tracked apps and trigger refresh
        viewModel.trackedApps.observe(viewLifecycleOwner) { apps ->
            Log.d(TAG, "trackedApps changed: ${apps.size} apps")
            binding.emptyState.isVisible = apps.isEmpty()
            val distinctCount = apps.distinctBy { it.repoOwner.lowercase() + "/" + it.repoName.lowercase() }.size
            binding.tvSubtitle.text = if (apps.isEmpty()) {
                "Track your app releases"
            } else {
                "$distinctCount repositor${if (distinctCount == 1) "y" else "ies"}"
            }
            
            // Trigger refresh if a new app was added but isn't yet in appStates
            val currentStatesSize = viewModel.appStates.value?.size ?: 0
            if (apps.isNotEmpty() && apps.size != currentStatesSize) {
                Log.d(TAG, "New app detected (apps: ${apps.size}, states: $currentStatesSize), triggering refresh")
                viewModel.refreshApps()
            }
        }
        
        // Observe enriched app states with version info
        viewModel.appStates.observe(viewLifecycleOwner) { states ->
            Log.d(TAG, "appStates updated: ${states.size} states")
            adapter.submitList(states)
        }
        
        // Observe loading state for SwipeRefreshLayout
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.isNullOrBlank()) return@observe
            showSnackbar(msg)
        }
    }

    private fun showSnackbar(message: String) {
        val root = binding.homeRoot
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
            .setTextColor(resources.getColor(R.color.primary_container, null))
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        // Refresh when returning to this fragment
        if (viewModel.trackedApps.value?.isNotEmpty() == true) {
            viewModel.refreshApps()
        }
    }

    override fun onDestroyView() {
        // Cancel any running animations to prevent NPE during callbacks
        _binding?.fabOverlay?.animate()?.cancel()
        _binding?.fabAddContainer?.animate()?.cancel()
        _binding?.fabClearContainer?.animate()?.cancel()
        
        super.onDestroyView()
        _binding = null
    }
}
