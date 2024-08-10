package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists")
    suspend fun loadArtists(): List<ArtistEntity>

    @Query("SELECT artists.*, COUNT(game_id) AS itemCount FROM artists LEFT OUTER JOIN games_artists ON artists.artist_id = games_artists.artist_id GROUP BY games_artists.artist_id")
    fun loadArtistsFlow(): Flow<List<ArtistWithItemCount>>

    @Query("SELECT * FROM artists WHERE artist_id=:artistId")
    suspend fun loadArtist(artistId: Int): ArtistEntity?

    @Query("SELECT * FROM artists WHERE artist_id=:artistId")
    fun loadArtistFlow(artistId: Int): Flow<ArtistEntity?>

    @Query("SELECT artists.* FROM artists LEFT OUTER JOIN games_artists ON artists.artist_id = games_artists.artist_id  WHERE game_id = :gameId")
    suspend fun loadArtistsForGame(gameId: Int): List<ArtistEntity>

    @Query("UPDATE artists SET artist_image_url=:imageUrl, artist_thumbnail_url=:thumbnailUrl, artist_images_updated_timestamp=:timestamp WHERE artist_id=:artistId")
    suspend fun updateImageUrls(artistId: Int, imageUrl: String, thumbnailUrl: String, timestamp: Date)

    @Query("UPDATE artists SET artist_hero_image_url=:url WHERE artist_id=:artistId")
    suspend fun updateHeroImageUrl(artistId: Int, url: String)

    @Query("UPDATE artists SET whitmore_score=:score, artist_stats_updated_timestamp=:timestamp WHERE artist_id=:artistId")
    suspend fun updateWhitmoreScore(artistId: Int, score: Int, timestamp: Date)

    @Insert(ArtistEntity::class)
    suspend fun insert(artist: ArtistForUpsert)

    @Update(ArtistEntity::class)
    suspend fun update(artist: ArtistForUpsert)

    @Insert(ArtistEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(artist: ArtistBriefForUpsert)

    @Query("DELETE FROM artists")
    suspend fun deleteAll(): Int
}
