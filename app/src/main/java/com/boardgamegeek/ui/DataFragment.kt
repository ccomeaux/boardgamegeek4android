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
    private var currentType: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileTypesView = view.findViewById(R.id.backup_types)
        createDataRow(Constants.TYPE_COLLECTION_VIEWS, R.string.backup_type_collection_view, R.string.backup_description_collection_view)
        createDataRow(Constants.TYPE_GAMES, R.string.backup_type_game, R.string.backup_description_game)
        createDataRow(Constants.TYPE_USERS, R.string.backup_type_user, R.string.backup_description_user)
    }

    private fun createDataRow(type: String, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
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

    private fun getExportTask(type: String?, uri: Uri): JsonExportTask<*>? {
        return when (type) {
            Constants.TYPE_COLLECTION_VIEWS -> CollectionViewExportTask(requireContext(), uri)
            Constants.TYPE_GAMES -> GameExportTask(requireContext(), uri)
            Constants.TYPE_USERS -> UserExportTask(requireContext(), uri)
            else -> null
        }
    }

    private fun getImportTask(type: String?, uri: Uri): JsonImportTask<*>? {
        return when (type) {
            Constants.TYPE_COLLECTION_VIEWS -> CollectionViewImportTask(requireContext(), uri)
            Constants.TYPE_GAMES -> GameImportTask(requireContext(), uri)
            Constants.TYPE_USERS -> UserImportTask(requireContext(), uri)
            else -> null
        }
    }

    override fun onExportClicked(type: String?) {
        currentType = type
        startActivityForResult(createIntent(type, Intent.ACTION_CREATE_DOCUMENT), REQUEST_EXPORT)
    }

    override fun onImportClicked(type: String?) {
        currentType = type
        startActivityForResult(createIntent(type, Intent.ACTION_OPEN_DOCUMENT), REQUEST_IMPORT)
    }

    private fun createIntent(type: String?, action: String): Intent {
        return Intent(action).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("text/json")
            putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(type))
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
        currentType?.let {
            if (requestCode == REQUEST_EXPORT) {
                performExport(it, uri)
            } else if (requestCode == REQUEST_IMPORT) {
                performImport(it, uri)
            }
        }
    }

    private fun performExport(type: String, uri: Uri) {
        val task = getExportTask(type, uri)
        if (task == null) {
            Timber.i("No task found for %s", type)
            return
        }
        findRow(type)?.initProgressBar()
        task.executeAsyncTask()
        logAction("Export")
    }

    private fun performImport(type: String, uri: Uri) {
        val task = getImportTask(type, uri)
        if (task == null) {
            Timber.i("No task found for %s", type)
            return
        }
        findRow(type)?.initProgressBar()
        task.executeAsyncTask()
        logAction("Import")
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

    private fun findRow(type: String): DataStepRow? {
        return fileTypesView.children.firstOrNull { it.tag == type } as? DataStepRow
    }

    private fun notifyEnd(errorMessage: String?, @StringRes successResId: Int, @StringRes failureResId: Int) {
        val message = if (TextUtils.isEmpty(errorMessage)) getString(successResId) else getString(failureResId) + " - " + errorMessage
        this.view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQUEST_EXPORT = 1000
        private const val REQUEST_IMPORT = 2000
        private const val ANSWERS_EVENT_NAME = "DataManagement"
        private const val ANSWERS_ATTRIBUTE_KEY_ACTION = "Action"
    }
}