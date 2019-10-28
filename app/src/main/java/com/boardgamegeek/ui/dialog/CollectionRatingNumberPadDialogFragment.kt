package com.boardgamegeek.ui.dialog

import com.boardgamegeek.R
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.UpdateCollectionItemRatingTask

class CollectionRatingNumberPadDialogFragment : NumberPadDialogFragment() {
    override fun done(output: Double, requestCode: Int) {
        val gameId = arguments?.getInt(KEY_GAME_ID) ?: BggContract.INVALID_ID
        val collectionId = arguments?.getInt(KEY_COLLECTION_ID) ?: BggContract.INVALID_ID
        val internalId = arguments?.getLong(KEY_INTERNAL_ID) ?: BggContract.INVALID_ID.toLong()
        UpdateCollectionItemRatingTask(context, gameId, collectionId, internalId, output).executeAsyncTask()
    }

    companion object {
        const val KEY_GAME_ID = "GAME_ID"
        const val KEY_COLLECTION_ID = "COLLECTION_ID"
        const val KEY_INTERNAL_ID = "INTERNAL_ID"

        @JvmStatic
        fun newInstance(gameId: Int,
                        collectionId: Int,
                        internalId: Long,
                        initialValue: String
        ): CollectionRatingNumberPadDialogFragment {
            val args = createBundle(0, R.string.rating, initialValue, null, null, 1.0, 10.0, 6)
            args.putInt(KEY_GAME_ID, gameId)
            args.putInt(KEY_COLLECTION_ID, collectionId)
            args.putLong(KEY_INTERNAL_ID, internalId)
            return CollectionRatingNumberPadDialogFragment().apply {
                arguments = args
            }
        }
    }
}
