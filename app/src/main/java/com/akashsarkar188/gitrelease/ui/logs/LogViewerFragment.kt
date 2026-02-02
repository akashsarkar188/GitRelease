package com.akashsarkar188.gitrelease.ui.logs

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.akashsarkar188.gitrelease.databinding.FragmentLogViewerBinding
import java.io.File

class LogViewerFragment : Fragment() {

    private var _binding: FragmentLogViewerBinding? = null
    private val binding get() = _binding!!
    
    private var packageName: String = ""
    private var logFiles: List<File> = emptyList()
    private var currentIndex = 0

    companion object {
        private const val ARG_PACKAGE_NAME = "package_name"
        
        fun newInstance(packageName: String): LogViewerFragment {
            return LogViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PACKAGE_NAME, packageName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = arguments?.getString(ARG_PACKAGE_NAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadLogFiles()
        setupListeners()
        displayCurrentFile()
    }

    private fun loadLogFiles() {
        // Log path: /sdcard/Android/media/{package}/logs/{date}/log_X.txt
        val baseDir = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/$packageName/logs"
        )
        
        binding.tvLogPath.text = baseDir.absolutePath
        
        if (!baseDir.exists()) {
            binding.tvLogContent.text = "No logs found at:\n${baseDir.absolutePath}"
            return
        }
        
        // Collect all log files from all date folders
        logFiles = baseDir.listFiles()
            ?.filter { it.isDirectory }
            ?.flatMap { dateDir ->
                dateDir.listFiles()
                    ?.filter { it.isFile && it.name.endsWith(".txt") }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
            }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        
        currentIndex = 0
    }

    private fun setupListeners() {
        binding.btnPrevFile.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                displayCurrentFile()
            }
        }
        
        binding.btnNextFile.setOnClickListener {
            if (currentIndex < logFiles.size - 1) {
                currentIndex++
                displayCurrentFile()
            }
        }
    }

    private fun displayCurrentFile() {
        if (logFiles.isEmpty()) {
            binding.tvLogContent.text = "No log files found"
            binding.tvFileInfo.text = "0 of 0"
            binding.btnPrevFile.isEnabled = false
            binding.btnNextFile.isEnabled = false
            return
        }
        
        val file = logFiles[currentIndex]
        binding.tvFileInfo.text = "${currentIndex + 1} of ${logFiles.size}"
        binding.tvLogPath.text = file.absolutePath
        
        binding.btnPrevFile.isEnabled = currentIndex > 0
        binding.btnNextFile.isEnabled = currentIndex < logFiles.size - 1
        
        try {
            val content = file.readText()
            binding.tvLogContent.text = content.ifEmpty { "(Empty file)" }
        } catch (e: Exception) {
            binding.tvLogContent.text = "Error reading file: ${e.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
