package com.boardgamegeek.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.export.Constants
import com.boardgamegeek.ui.viewmodel.DataPortViewModel
import com.boardgamegeek.ui.widget.DataStepRow
import com.boardgamegeek.util.FileUtils
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import org.jetbrains.anko.support.v4.toast
import timber.log.Timber

class DataFragment : Fragment(R.layout.fragment_data), DataStepRow.Listener {
    private lateinit var fileTypesView: ViewGroup
    private val viewModel by activityViewModels<DataPortViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileTypesView = view.findViewById(R.id.backup_types)
        createDataRow(Constants.TYPE_COLLECTION_VIEWS, R.string.backup_type_collection_view, R.string.backup_description_collection_view)
        createDataRow(Constants.TYPE_GAMES, R.string.backup_type_game, R.string.backup_description_game)
        createDataRow(Constants.TYPE_USERS, R.string.backup_type_user, R.string.backup_description_user)

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { content ->
                toast(content)
            }
        }
        viewModel.collectionViewProgress.observe(viewLifecycleOwner) { event ->
            updateProgress(Constants.TYPE_COLLECTION_VIEWS, event.first, event.second) // TODO separate import / export
        }
        viewModel.gameProgress.observe(viewLifecycleOwner) { event ->
            updateProgress(Constants.TYPE_GAMES, event.first, event.second)
        }
        viewModel.userProgress.observe(viewLifecycleOwner) { event ->
            updateProgress(Constants.TYPE_USERS, event.first, event.second)
        }
    }

    private fun updateProgress(type: Int, max: Int, progress: Int) {
        findRow(type)?.let { row ->
            row.updateProgressBar(max, progress)
            if (progress >= max) {
                row.hideProgressBar()
            }
        }
    }

    private fun createDataRow(type: Int, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
        val row = DataStepRow(requireContext())
        row.setListener(this)
        row.bind(type, typeResId, descriptionResId)
        row.tag = type
        fileTypesView.addView(row)
    }

    override fun onExportClicked(type: Int) {
        startActivityForResult(createIntent(type, Intent.ACTION_CREATE_DOCUMENT), REQUEST_EXPORT + type)
    }

    override fun onImportClicked(type: Int) {
        startActivityForResult(createIntent(type, Intent.ACTION_OPEN_DOCUMENT), REQUEST_IMPORT + type)
    }

    private fun createIntent(type: Int, action: String): Intent {
        val typeDescription = when (type) {
            Constants.TYPE_COLLECTION_VIEWS -> Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION
            Constants.TYPE_GAMES -> Constants.TYPE_GAMES_DESCRIPTION
            Constants.TYPE_USERS -> Constants.TYPE_USERS_DESCRIPTION
            else -> ""
        }
        return Intent(action).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("text/json")
            putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(typeDescription))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || !isAdded) return
        val uri: Uri = data?.data ?: return
        try {
            val modeFlags: Int = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            requireContext().contentResolver.takePersistableUriPermission(uri, modeFlags)
        } catch (e: SecurityException) {
            Timber.e(e, "Could not persist URI permissions for '%s'.", uri.toString())
        }
        when (requestCode) {
            REQUEST_EXPORT_COLLECTION_VIEW -> {
                findRow(Constants.TYPE_COLLECTION_VIEWS)?.initProgressBar()
                viewModel.exportCollectionViews(uri)
                logAction("Export")
            }
            REQUEST_EXPORT_GAME -> {
                findRow(Constants.TYPE_GAMES)?.initProgressBar()
                viewModel.exportGames(uri)
                logAction("Export")
            }
            REQUEST_EXPORT_USER -> {
                findRow(Constants.TYPE_USERS)?.initProgressBar()
                viewModel.exportUsers(uri)
                logAction("Export")
            }
            REQUEST_IMPORT_COLLECTION_VIEW -> {
                findRow(Constants.TYPE_COLLECTION_VIEWS)?.initProgressBar()
                viewModel.importCollectionViews(uri)
                logAction("Import")
            }
            REQUEST_IMPORT_GAME -> {
                findRow(Constants.TYPE_GAMES)?.initProgressBar()
                viewModel.importGames(uri)
                logAction("Import")
            }
            REQUEST_IMPORT_USER -> {
                findRow(Constants.TYPE_USERS)?.initProgressBar()
                viewModel.importUsers(uri)
                logAction("Import")
            }
        }
    }

    private fun logAction(action: String) {
        Firebase.analytics.logEvent("DataManagement") {
            param("Action", action)
        }
    }

    private fun findRow(type: Int): DataStepRow? {
        return fileTypesView.children.firstOrNull { (it.tag as? Int) == type } as? DataStepRow
    }

    companion object {
        private const val REQUEST_EXPORT = 1000
        private const val REQUEST_EXPORT_COLLECTION_VIEW = REQUEST_EXPORT + Constants.TYPE_COLLECTION_VIEWS
        private const val REQUEST_EXPORT_GAME = REQUEST_EXPORT + Constants.TYPE_GAMES
        private const val REQUEST_EXPORT_USER = REQUEST_EXPORT + Constants.TYPE_USERS
        private const val REQUEST_IMPORT = 2000
        private const val REQUEST_IMPORT_COLLECTION_VIEW = REQUEST_IMPORT + Constants.TYPE_COLLECTION_VIEWS
        private const val REQUEST_IMPORT_GAME = REQUEST_IMPORT + Constants.TYPE_GAMES
        private const val REQUEST_IMPORT_USER = REQUEST_IMPORT + Constants.TYPE_USERS
    }
}