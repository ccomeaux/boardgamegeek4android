package com.boardgamegeek.ui.loader

import com.boardgamegeek.io.model.GeekListResponse
import retrofit2.Call

class GeekListSafeResponse(call: Call<GeekListResponse?>?) : SafeResponse<GeekListResponse?>(call)
