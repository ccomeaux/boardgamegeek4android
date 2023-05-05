package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentDataBinding
import com.boardgamegeek.export.Constants
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.ui.viewmodel.DataPortViewModel
import com.boardgamegeek.ui.widget.DataStepRow
import com.boardgamegeek.util.FileUtils
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DataFragment : Fragment() {
    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<DataPortViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.collectionViewsRow.onExport {
            registerForCollectionViewsExport.launch(Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION)
        }
        binding.collectionViewsRow.onImport {
            registerForCollectionViewsImport.launch(null)
        }

        binding.gamesRow.onExport {
            registerForGamesExport.launch(Constants.TYPE_GAMES_DESCRIPTION)
        }
        binding.gamesRow.onImport {
            registerForGamesImport.launch(null)
        }

        binding.usersRow.onExport {
            registerForUsersExport.launch(Constants.TYPE_USERS_DESCRIPTION)
        }
        binding.usersRow.onImport {
            registerForUsersImport.launch(null)
        }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { content ->
                toast(content)
            }
        }
        viewModel.collectionViewProgress.observe(viewLifecycleOwner) { binding.collectionViewsRow.updateProgressBar(it) }
        viewModel.gameProgress.observe(viewLifecycleOwner) { binding.gamesRow.updateProgressBar(it) }
        viewModel.userProgress.observe(viewLifecycleOwner) { binding.usersRow.updateProgressBar(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val registerForCollectionViewsExport =
        registerForActivityResult(ExportFileContract()) { uri ->
            doExport(uri, binding.collectionViewsRow) { viewModel.exportCollectionViews(it) }
        }

    private val registerForCollectionViewsImport =
        registerForActivityResult(ImportFileContract()) { uri ->
            doImport(uri, binding.collectionViewsRow) { viewModel.importCollectionViews(it) }
        }

    private val registerForGamesExport =
        registerForActivityResult(ExportFileContract()) { uri ->
            doExport(uri, binding.gamesRow) { viewModel.exportGames(it) }
        }

    private val registerForGamesImport =
        registerForActivityResult(ImportFileContract()) { uri ->
            doImport(uri, binding.gamesRow) { viewModel.importGames(it) }
        }

    private val registerForUsersExport =
        registerForActivityResult(ExportFileContract()) { uri ->
            doExport(uri, binding.usersRow) { viewModel.exportUsers(it) }
        }

    private val registerForUsersImport =
        registerForActivityResult(ImportFileContract()) { uri ->
            doImport(uri, binding.usersRow) { viewModel.importUsers(it) }
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
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
                .putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(input))
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }

    class ImportFileContract : ActivityResultContract<Unit, Uri?>() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }
}
