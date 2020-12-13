package com.boardgamegeek.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.events.ExportFinishedEvent
import com.boardgamegeek.events.ExportProgressEvent
import com.boardgamegeek.events.ImportFinishedEvent
import com.boardgamegeek.events.ImportProgressEvent
import com.boardgamegeek.export.*
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.ui.widget.DataStepRow
import com.boardgamegeek.util.FileUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class DataFragment : Fragment(R.layout.fragment_data), DataStepRow.Listener {
    private lateinit var fileTypesView: ViewGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileTypesView = view.findViewById(R.id.backup_types)
        createDataRow(Constants.TYPE_COLLECTION_VIEWS, R.string.backup_type_collection_view, R.string.backup_description_collection_view)
        createDataRow(Constants.TYPE_GAMES, R.string.backup_type_game, R.string.backup_description_game)
        createDataRow(Constants.TYPE_USERS, R.string.backup_type_user, R.string.backup_description_user)
    }

    private fun createDataRow(type: Int, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
        val row = DataStepRow(requireContext())
        row.setListener(this)
        row.bind(type, typeResId, descriptionResId)
        row.tag = type
        fileTypesView.addView(row)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
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
        // TODO
        if (requestCode >= REQUEST_IMPORT) {
            performImport(requestCode, uri)
        } else if (requestCode >= REQUEST_EXPORT) {
            performExport(requestCode, uri)
        }
    }

    private fun performExport(type: Int, uri: Uri) {
        val task = getExportTask(type, uri)
        if (task == null) {
            Timber.i("No task found for %s", type)
            return
        }
        findRow(type - REQUEST_EXPORT)?.initProgressBar()
        task.executeAsyncTask()
        logAction("Export")
    }

    private fun performImport(type: Int, uri: Uri) {
        val task = getImportTask(type, uri)
        if (task == null) {
            Timber.i("No task found for %s", type)
            return
        }
        findRow(type - REQUEST_IMPORT)?.initProgressBar()
        task.executeAsyncTask()
        logAction("Import")
    }

    private fun getExportTask(type: Int, uri: Uri): JsonExportTask<*>? {
        return when (type) {
            REQUEST_EXPORT_COLLECTION_VIEW -> CollectionViewExportTask(requireContext(), uri)
            REQUEST_EXPORT_GAME -> GameExportTask(requireContext(), uri)
            REQUEST_EXPORT_USER -> UserExportTask(requireContext(), uri)
            else -> null
        }
    }

    private fun getImportTask(type: Int, uri: Uri): JsonImportTask<*>? {
        return when (type) {
            REQUEST_IMPORT_COLLECTION_VIEW -> CollectionViewImportTask(requireContext(), uri)
            REQUEST_IMPORT_GAME -> GameImportTask(requireContext(), uri)
            REQUEST_IMPORT_USER -> UserImportTask(requireContext(), uri)
            else -> null
        }
    }

    private fun logAction(action: String) {
        Firebase.analytics.logEvent(ANSWERS_EVENT_NAME) {
            param(ANSWERS_ATTRIBUTE_KEY_ACTION, action)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ExportFinishedEvent) {
        findRow(event.type)?.hideProgressBar()
        notifyEnd(event.errorMessage, R.string.msg_export_success, R.string.msg_export_failed)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ImportFinishedEvent) {
        findRow(event.type)?.hideProgressBar()
        notifyEnd(event.errorMessage, R.string.msg_import_success, R.string.msg_import_failed)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ExportProgressEvent) {
        findRow(event.type)?.updateProgressBar(event.totalCount, event.currentCount)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ImportProgressEvent) {
        findRow(event.type)?.updateProgressBar(event.totalCount, event.currentCount)
    }

    private fun findRow(type: Int): DataStepRow? {
        return fileTypesView.children.firstOrNull { (it.tag as? Int) == type } as? DataStepRow
    }

    private fun notifyEnd(errorMessage: String?, @StringRes successResId: Int, @StringRes failureResId: Int) {
        val message = if (TextUtils.isEmpty(errorMessage)) getString(successResId) else getString(failureResId) + " - " + errorMessage
        this.view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
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
        private const val ANSWERS_EVENT_NAME = "DataManagement"
        private const val ANSWERS_ATTRIBUTE_KEY_ACTION = "Action"
    }
}