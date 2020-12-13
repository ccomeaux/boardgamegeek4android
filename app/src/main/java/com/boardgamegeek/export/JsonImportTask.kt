package com.boardgamegeek.export

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.ParcelFileDescriptor
import com.boardgamegeek.R
import com.boardgamegeek.events.ImportFinishedEvent
import com.boardgamegeek.events.ImportProgressEvent
import com.boardgamegeek.export.model.Model
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.collections.forEachWithIndex
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

abstract class JsonImportTask<T : Model>(context: Context, private val type: Int, private val typeDescription: String, private val uri: Uri) : AsyncTask<Void, Int, String?>() {
    @SuppressLint("StaticFieldLeak")
    protected val context: Context = context.applicationContext
    private val items: MutableList<T> = mutableListOf()
    private val gson = Gson()

    protected open fun initializeImport() {}
    protected abstract fun parseItem(gson: Gson, reader: JsonReader): T
    protected abstract fun importRecord(item: T, version: Int)

    override fun doInBackground(vararg params: Void): String? {
        val pfd: ParcelFileDescriptor = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: FileNotFoundException) {
            val error = context.getString(R.string.msg_import_failed_file_not_exist, uri)
            Timber.w(e, error)
            return error
        } ?: return context.getString(R.string.msg_export_failed_null_pfd, uri)

        if (isCancelled) return context.getString(R.string.cancelled)

        initializeImport()

        var version = 0
        val reader = JsonReader(InputStreamReader(FileInputStream(pfd.fileDescriptor), "UTF-8"))
        try {
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                parseItems(reader)
            } else {
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        Constants.NAME_TYPE -> reader.nextString().also {
                            if (it != typeDescription) {
                                return context.getString(R.string.msg_import_failed_wrong_type, typeDescription, it)
                            }
                        }
                        Constants.NAME_VERSION -> version = reader.nextInt()
                        Constants.NAME_ITEMS -> parseItems(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Timber.w(e, "Importing %s JSON file.", type)
            return context.getString(R.string.msg_import_failed_parse_json)
        } finally {
            try {
                reader.close()
            } catch (e: IOException) {
                Timber.w(e, "Failed trying to close the JsonReader")
            }
        }
        items.forEachWithIndex { i, item ->
            publishProgress(items.size, i)
            importRecord(item, version)
        }

        try {
            pfd.close()
        } catch (e: IOException) {
            Timber.w(e)
        }

        return null
    }

    @Throws(IOException::class)
    private fun parseItems(reader: JsonReader) {
        items.clear()
        reader.beginArray()
        while (reader.hasNext()) {
            items.add(parseItem(gson, reader))
        }
        reader.endArray()
    }

    override fun onProgressUpdate(vararg values: Int?) {
        EventBus.getDefault().post(ImportProgressEvent(
                values[PROGRESS_TOTAL]!!,
                values[PROGRESS_CURRENT]!!,
                type))
    }

    override fun onPostExecute(errorMessage: String?) {
        Timber.i(errorMessage)
        EventBus.getDefault().post(ImportFinishedEvent(type, errorMessage))
    }

    companion object {
        private const val PROGRESS_TOTAL = 0
        private const val PROGRESS_CURRENT = 1
    }
}