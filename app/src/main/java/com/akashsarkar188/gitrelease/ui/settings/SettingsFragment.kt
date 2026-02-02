package com.akashsarkar188.gitrelease.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.akashsarkar188.gitrelease.auth.TokenManager
import com.akashsarkar188.gitrelease.data.local.AppDatabase
import com.akashsarkar188.gitrelease.data.repository.AppRepository
import com.akashsarkar188.gitrelease.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SettingsViewModel

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
        val repository = AppRepository(db.trackedAppDao())
        viewModel = SettingsViewModel(repository, requireContext())

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
        // Load existing token if saved
        val savedToken = TokenManager.getToken(requireContext())
        if (!savedToken.isNullOrBlank()) {
            binding.etToken.setText(savedToken)
        }

        binding.btnSaveToken.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(context, "Please enter a token", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            TokenManager.saveToken(requireContext(), token)
            Toast.makeText(context, "Token saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAddRepoSection() {
        binding.btnAddApp.setOnClickListener {
            val url = binding.etRepoUrl.text.toString().trim()
            
            if (url.isBlank()) {
                Toast.makeText(context, "Please enter a repository", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Package name is auto-detected after first download
            viewModel.addRepository(url)
        }
    }

    private fun setupObservers() {
        viewModel.status.observe(viewLifecycleOwner) { msg ->
            binding.tvStatus.text = msg
        }
        
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SettingsEvent.NavigateBack -> {
                    Toast.makeText(context, "Repository added!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                    viewModel.clearEvent()
                }
                is SettingsEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearEvent()
                }
                null -> { /* No event */ }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
