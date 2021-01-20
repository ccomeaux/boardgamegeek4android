package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

data class User(@Expose val name: String, @Expose val colors: List<PlayerColor>) : Model()