package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

class Game(@Expose val id: Int, @Expose val colors: List<Color>) : Model()