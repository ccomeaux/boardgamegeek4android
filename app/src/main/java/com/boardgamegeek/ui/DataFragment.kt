package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
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
import kotlinx.android.synthetic.main.fragment_data.*
import org.jetbrains.anko.support.v4.toast
import timber.log.Timber

class DataFragment : Fragment(R.layout.fragment_data) {
    private val viewModel by activityViewModels<DataPortViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionViewsRow.onExport {
            registerForCollectionViewsExport.launch(Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION)
        }
        collectionViewsRow.onImport {
            registerForCollectionViewsImport.launch(null)
        }

        gamesRow.onExport {
            registerForGamesExport.launch(Constants.TYPE_GAMES_DESCRIPTION)
        }
        gamesRow.onImport {
            registerForGamesImport.launch(null)
        }

        usersRow.onExport {
            registerForUsersExport.launch(Constants.TYPE_USERS_DESCRIPTION)
        }
        usersRow.onImport {
            registerForUsersImport.launch(null)
        }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { content ->
                toast(content)
            }
        }
        viewModel.collectionViewProgress.observe(viewLifecycleOwner) { collectionViewsRow.updateProgressBar(it) }
        viewModel.gameProgress.observe(viewLifecycleOwner) { gamesRow.updateProgressBar(it) }
        viewModel.userProgress.observe(viewLifecycleOwner) { usersRow.updateProgressBar(it) }
    }

    private val registerForCollectionViewsExport =
        registerForActivityResult(ExportFileContract()) { uri ->
            doExport(uri, collectionViewsRow) { viewModel.exportCollectionViews(it) }
        }

    private val registerForCollectionViewsImport =
        registerForActivityResult(ImportFileContract()) { uri ->
            doImport(uri, collectionViewsRow) { viewModel.importCollectionViews(it) }
        }

    private val registerForGamesExport =
        registerForActivityResult(ExportFileContract()) { uri ->
            doExport(uri, gamesRow) { viewModel.exportGames(it) }
        }

    private val registerForGamesImport =
        registerForActivityResult(ImportFileContract()) { uri ->
            doImport(uri, gamesRow) { viewModel.importGames(it) }
        }

    private val registerForUsersExport =
        registerForActivityResult(ExportFileContract()) { uri ->
            doExport(uri, usersRow) { viewModel.exportUsers(it) }
        }

    private val registerForUsersImport =
        registerForActivityResult(ImportFileContract()) { uri ->
            doImport(uri, usersRow) { viewModel.importUsers(it) }
        }

    private fun doExport(uri: Uri?, dataStepRow: DataStepRow, export: (Uri) -> Unit) {
        uri?.let {
            tryUriPermission(it)
            dataStepRow.initProgressBar()
            export(it)
            logAction("Export")
        }
    }

    private fun doImport(uri: Uri?, dataStepRow: DataStepRow, import: (Uri) -> Unit) {
        uri?.let {
            tryUriPermission(it)
            dataStepRow.initProgressBar()
            import(it)
            logAction("Import")
        }
    }

    private fun tryUriPermission(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Timber.e(e, "Could not persist URI permissions for '%s'.", uri.toString())
        }
    }

    private fun logAction(action: String) {
        Firebase.analytics.logEvent("DataManagement") {
            param("Action", action)
        }
    }

    class ExportFileContract : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, typeDescription: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
                .putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(typeDescription))
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }

    class ImportFileContract : ActivityResultContract<Unit, Uri?>() {
        override fun createIntent(context: Context, input: Unit?): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }
}
