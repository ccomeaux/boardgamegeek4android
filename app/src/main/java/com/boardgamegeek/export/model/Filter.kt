package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

class Filter(@Expose val type: Int, @Expose val data: String)