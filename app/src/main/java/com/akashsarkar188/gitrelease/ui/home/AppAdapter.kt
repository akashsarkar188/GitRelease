package com.akashsarkar188.gitrelease.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.akashsarkar188.gitrelease.R
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AppAdapter(
    private val onDeleteClick: (TrackedApp) -> Unit,
    private val onDownloadClick: (TrackedApp, TrackInfo) -> Unit
) : ListAdapter<AppUiState, AppAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivOwnerAvatar: ImageView = itemView.findViewById(R.id.ivOwnerAvatar)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvRepoPath: TextView = itemView.findViewById(R.id.tvRepoPath)
        private val layoutInstalledVersion: View = itemView.findViewById(R.id.layoutInstalledVersion)
        private val layoutPackageRows: LinearLayout = itemView.findViewById(R.id.layoutPackageRows)
        private val tvUpToDate: TextView = itemView.findViewById(R.id.tvUpToDate)
        private val tvPackageUnknown: TextView = itemView.findViewById(R.id.tvPackageUnknown)
        private val layoutTracks: LinearLayout = itemView.findViewById(R.id.layoutTracks)
        private val tvError: TextView = itemView.findViewById(R.id.tvError)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(state: AppUiState) {
            val app = state.app

            // Header
            tvAppName.text = app.appName
            tvRepoPath.text = app.fullRepoPath
            
            ivOwnerAvatar.load(app.ownerAvatarUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
                transformations(CircleCropTransformation())
            }

            // Disable delete for self-app
            val isDefaultRepo = app.repoOwner.equals("akashsarkar188", ignoreCase = true) && 
                               app.repoName.equals("GitRelease", ignoreCase = true)
            btnDelete.isVisible = !isDefaultRepo

            // Installed packages
            layoutPackageRows.removeAllViews()
            if (state.installedPackages.isNotEmpty()) {
                layoutInstalledVersion.visibility = View.VISIBLE
                tvPackageUnknown.visibility = View.GONE
                
                for (pkg in state.installedPackages) {
                    val row = LayoutInflater.from(itemView.context).inflate(R.layout.view_package_row, layoutPackageRows, false)
                    val tvVersion = row.findViewById<TextView>(R.id.tvPkgVersion)
                    val tvName = row.findViewById<TextView>(R.id.tvPkgName)
                    
                    val versionText = if (pkg.versionCode != null) {
                        "v${pkg.versionName} (code: ${pkg.versionCode})"
                    } else {
                        "v${pkg.versionName}"
                    }
                    tvVersion.text = versionText
                    tvName.text = "pkg: ${pkg.packageName}"
                    layoutPackageRows.addView(row)
                }
                
                // Show "Up to date" marker if all tracks are satisfied
                tvUpToDate.isVisible = state.isUpToDate
            } else if (app.packageName.isBlank()) {
                // Package unknown
                layoutInstalledVersion.visibility = View.GONE
                tvPackageUnknown.visibility = View.VISIBLE
            } else {
                layoutInstalledVersion.visibility = View.VISIBLE
                tvPackageUnknown.visibility = View.GONE
                
                val noPkgView = TextView(itemView.context).apply {
                    text = "Not installed"
                    setTextColor(itemView.context.getColor(R.color.text_tertiary))
                    textSize = 13f
                }
                layoutPackageRows.addView(noPkgView)
                tvUpToDate.visibility = View.GONE
            }

            // Error state
            if (state.errorMessage != null) {
                tvError.visibility = View.VISIBLE
                tvError.text = state.errorMessage
            } else {
                tvError.visibility = View.GONE
            }

            // Delete button
            btnDelete.setOnClickListener {
                onDeleteClick(app)
            }

            // Clear and add tracks
            layoutTracks.removeAllViews()
            
            if (state.tracks.isEmpty() && state.errorMessage == null) {
                val noTracksView = TextView(itemView.context).apply {
                    text = "No releases found"
                    setPadding(32, 32, 32, 32)
                    setTextColor(context.getColor(R.color.text_tertiary))
                }
                layoutTracks.addView(noTracksView)
            } else {
                for (track in state.tracks) {
                    val trackView = createTrackView(app, track)
                    layoutTracks.addView(trackView)
                }
            }
        }

        private fun createTrackView(app: TrackedApp, track: TrackInfo): View {
            val inflater = LayoutInflater.from(itemView.context)
            val trackView = inflater.inflate(R.layout.item_track, layoutTracks, false)
            val context = itemView.context

            // Track badge
            val tvTrackBadge = trackView.findViewById<TextView>(R.id.tvTrackBadge)
            tvTrackBadge.text = track.trackType
            tvTrackBadge.setBackgroundResource(
                if (track.isPrerelease) R.drawable.badge_prerelease
                else R.drawable.badge_release
            )

            // Status badge
            val tvStatusBadge = trackView.findViewById<TextView>(R.id.tvStatusBadge)
            when (track.status) {
                TrackStatus.INSTALLED -> {
                    tvStatusBadge.visibility = View.VISIBLE
                    tvStatusBadge.text = "INSTALLED"
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_installed)
                }
                TrackStatus.UPDATE -> {
                    tvStatusBadge.visibility = View.VISIBLE
                    tvStatusBadge.text = "UPDATE"
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_update)
                }
                TrackStatus.OLD -> {
                    tvStatusBadge.visibility = View.VISIBLE
                    tvStatusBadge.text = "OLD"
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_old)
                }
                TrackStatus.UNKNOWN -> {
                    tvStatusBadge.visibility = View.GONE
                }
            }

            // Version
            val tvVersion = trackView.findViewById<TextView>(R.id.tvVersion)
            tvVersion.text = "v${track.version}"

            // Version code
            val tvVersionCode = trackView.findViewById<TextView>(R.id.tvVersionCode)
            if (track.versionCode != null) {
                tvVersionCode.visibility = View.VISIBLE
                tvVersionCode.text = "(code: ${track.versionCode})"
            } else {
                tvVersionCode.visibility = View.GONE
            }

            // Title
            val tvTitle = trackView.findViewById<TextView>(R.id.tvTitle)
            if (!track.title.isNullOrBlank() && track.title != track.version && track.title != "v${track.version}") {
                tvTitle.visibility = View.VISIBLE
                tvTitle.text = track.title
            } else {
                tvTitle.visibility = View.GONE
            }

            // Package Name in track
            val tvTrackPackageName = trackView.findViewById<TextView>(R.id.tvTrackPackageName)
            val trackPkg = app.trackPackageNames[track.trackType] ?: run {
                if (app.trackPackageNames.isEmpty()) app.packageName else ""
            }
            if (trackPkg.isNotBlank()) {
                tvTrackPackageName.visibility = View.VISIBLE
                tvTrackPackageName.text = "pkg: $trackPkg"
            } else {
                tvTrackPackageName.visibility = View.GONE
            }

            // Changelog with Read More/Less
            val tvChangelog = trackView.findViewById<TextView>(R.id.tvChangelog)
            val tvReadMore = trackView.findViewById<TextView>(R.id.tvReadMore)
            
            if (!track.changelog.isNullOrBlank()) {
                tvChangelog.visibility = View.VISIBLE
                tvChangelog.text = track.changelog
                tvChangelog.maxLines = 3
                
                // Reset Read More state
                tvReadMore.visibility = View.GONE
                
                // Robust Read More detection
                tvChangelog.post {
                    val layout = tvChangelog.layout
                    if (layout != null) {
                        val lines = layout.lineCount
                        if (lines > 0 && layout.getEllipsisCount(lines - 1) > 0) {
                            tvReadMore.visibility = View.VISIBLE
                            tvReadMore.text = "Read more"
                            
                            var isExpanded = false
                            tvReadMore.setOnClickListener {
                                isExpanded = !isExpanded
                                if (isExpanded) {
                                    tvChangelog.maxLines = Int.MAX_VALUE
                                    tvReadMore.text = "Read less"
                                } else {
                                    tvChangelog.maxLines = 3
                                    tvReadMore.text = "Read more"
                                }
                            }
                        }
                    }
                }
            } else {
                tvChangelog.visibility = View.GONE
                tvReadMore.visibility = View.GONE
            }

            // Published date/time
            val tvPublishedAt = trackView.findViewById<TextView>(R.id.tvPublishedAt)
            tvPublishedAt.text = "📅 ${formatDateTime(track.publishedAt)}"

            // No APK warning / Download button
            val layoutNoApk = trackView.findViewById<View>(R.id.layoutNoApk)
            val btnDownload = trackView.findViewById<MaterialButton>(R.id.btnDownload)
            
            if (track.apkAsset == null) {
                layoutNoApk.visibility = View.VISIBLE
                btnDownload.visibility = View.GONE
            } else {
                layoutNoApk.visibility = View.GONE
                btnDownload.visibility = View.VISIBLE
                
                // Change button text based on status
                when (track.status) {
                    TrackStatus.INSTALLED -> {
                        btnDownload.text = "Reinstall"
                        btnDownload.setBackgroundColor(context.getColor(R.color.text_tertiary))
                    }
                    TrackStatus.UPDATE -> {
                        btnDownload.text = "Update"
                        btnDownload.setBackgroundColor(context.getColor(R.color.status_success))
                    }
                    TrackStatus.OLD -> {
                        btnDownload.text = "Downgrade"
                        btnDownload.setBackgroundColor(context.getColor(R.color.text_tertiary))
                    }
                    TrackStatus.UNKNOWN -> {
                        btnDownload.text = "Download"
                        btnDownload.setBackgroundColor(context.getColor(R.color.primary_blue))
                    }
                }
                
                btnDownload.setOnClickListener {
                    onDownloadClick(app, track)
                }
            }

            return trackView
        }

        private fun formatDateTime(isoDate: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US)
                val date = inputFormat.parse(isoDate)
                date?.let { outputFormat.format(it) } ?: isoDate
            } catch (e: Exception) {
                isoDate.take(16).replace("T", " ") // Fallback
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppUiState>() {
        override fun areItemsTheSame(oldItem: AppUiState, newItem: AppUiState): Boolean {
            return oldItem.app.id == newItem.app.id
        }

        override fun areContentsTheSame(oldItem: AppUiState, newItem: AppUiState): Boolean {
            return oldItem == newItem
        }
    }
}
