package com.boardgamegeek.export

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import com.boardgamegeek.R
import com.boardgamegeek.events.ExportFinishedEvent
import com.boardgamegeek.events.ExportProgressEvent
import com.boardgamegeek.export.model.Model
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import okhttp3.internal.closeQuietly
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.*

abstract class JsonExportTask<T : Model>(context: Context, private val type: String, private val uri: Uri) : AsyncTask<Void, Int, String?>() {
    @SuppressLint("StaticFieldLeak")
    private val context: Context = context.applicationContext
    protected open val version: Int
        get() = 0

    protected abstract fun getCursor(context: Context): Cursor?

    protected abstract fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter)

    override fun doInBackground(vararg params: Void): String? {
        if (isCancelled) return context.getString(R.string.cancelled)
        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "w")
        } catch (e: SecurityException) {
            val error = context.getString(R.string.msg_export_failed_permissions, uri)
            Timber.w(e, error)
            return error
        } catch (e: FileNotFoundException) {
            val error = context.getString(R.string.msg_export_failed_file_not_found, uri)
            Timber.w(e, error)
            return error
        } ?: return context.getString(R.string.msg_export_failed_null_pfd, uri)

        val out: OutputStream = FileOutputStream(pfd.fileDescriptor)

        val cursor = getCursor(context)
                ?: return context.getString(R.string.msg_export_failed_null_cursor)
        try {
            writeJsonStream(out, cursor)
        } catch (e: Exception) {
            val error = context.getString(R.string.msg_export_failed_write_json)
            Timber.e(e, error)
            return error
        } finally {
            cursor.close()
        }

        try {
            pfd.close()
        } catch (e: IOException) {
            Timber.w(e)
        }
        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        EventBus.getDefault().post(ExportProgressEvent(
                values[PROGRESS_TOTAL]!!,
                values[PROGRESS_CURRENT]!!,
                type))
    }

    override fun onPostExecute(errorMessage: String?) {
        Timber.i(errorMessage)
        EventBus.getDefault().post(ExportFinishedEvent(type, errorMessage))
    }

    @Throws(IOException::class)
    private fun writeJsonStream(out: OutputStream, cursor: Cursor) {
        val gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
        JsonWriter(OutputStreamWriter(out, "UTF-8")).use {
            it.setIndent("  ")
            it.beginObject()
            it.name(Constants.NAME_TYPE).value(type)
            it.name(Constants.NAME_VERSION).value(version.toLong())
            it.name(Constants.NAME_ITEMS)
            it.beginArray()

            var numExported = 0
            while (cursor.moveToNext()) {
                if (isCancelled) break
                publishProgress(cursor.count, numExported++)
                try {
                    writeJsonRecord(context, cursor, gson, it)
                } catch (e: RuntimeException) {
                    Timber.e(e)
                }
            }

            it.endArray()
            it.endObject()
        }
    }

    companion object {
        private const val PROGRESS_TOTAL = 0
        private const val PROGRESS_CURRENT = 1
    }
}