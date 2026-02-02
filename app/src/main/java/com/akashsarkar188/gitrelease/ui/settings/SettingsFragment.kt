package com.akashsarkar188.gitrelease.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.akashsarkar188.gitrelease.R
import com.akashsarkar188.gitrelease.data.local.AppDatabase
import com.akashsarkar188.gitrelease.data.repository.AppRepository
import com.akashsarkar188.gitrelease.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SettingsViewModel
    private lateinit var tokenAdapter: TokenAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val repository = AppRepository(db.trackedAppDao(), db.githubTokenDao())
        viewModel = SettingsViewModel(repository)

        setupHeader()
        setupTokenSection()
        setupAddRepoSection()
        setupObservers()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupTokenSection() {
        tokenAdapter = TokenAdapter(
            onDeleteClick = { token ->
                viewModel.deleteToken(token)
            }
        )
        
        binding.rvTokens.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tokenAdapter
        }

        binding.btnSaveToken.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            if (token.isBlank()) {
                showSnackbar("Please enter a token")
                return@setOnClickListener
            }
            
            viewModel.addToken(token)
            binding.etToken.setText("") // Clear input
        }
    }

    private fun setupAddRepoSection() {
        binding.btnAddApp.setOnClickListener {
            val url = binding.etRepoUrl.text.toString().trim()
            
            if (url.isBlank()) {
                showSnackbar("Please enter a repository")
                return@setOnClickListener
            }
            
            // Package name is auto-detected after first download
            viewModel.addRepository(url)
        }
    }

    private fun setupObservers() {
        viewModel.status.observe(viewLifecycleOwner) { msg ->
            if (msg.isNullOrBlank()) return@observe
            showSnackbar(msg)
            if (msg.contains("Added token")) {
                binding.etToken.setText("")
            }
        }
        
        viewModel.tokens.observe(viewLifecycleOwner) { tokens: List<com.akashsarkar188.gitrelease.data.local.entity.GithubToken> ->
            tokenAdapter.submitList(tokens)
            binding.tvSavedTokensHeader.isVisible = tokens.isNotEmpty()
        }
        
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SettingsEvent.NavigateBack -> {
                    // Show message on previous screen or before popping
                    showSnackbar("Repository added!")
                    view?.postDelayed({
                        parentFragmentManager.popBackStack()
                    }, 500)
                    viewModel.clearEvent()
                }
                is SettingsEvent.ShowMessage -> {
                    showSnackbar(event.message)
                    viewModel.clearEvent()
                }
                null -> { /* No event */ }
            }
        }
    }

    private fun showSnackbar(message: String) {
        val root = binding.settingsRoot
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
            .setTextColor(resources.getColor(R.color.primary_container, null))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
