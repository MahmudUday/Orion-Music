package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OrionDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: OrionDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, OrionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name and offline manifest permissions are configured`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("ORION MUSIC", appName)

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse("ORION MUSIC must not declare INTERNET permission", Manifest.permission.INTERNET in perms)
        assertTrue(Manifest.permission.READ_MEDIA_AUDIO in perms)
        assertTrue(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK in perms)
    }

    @Test
    fun `large 5000 track library inserts and playlist reordering persist accurately`() = runTest {
        val batch = (1..5000).map { i ->
            SongEntity(
                mediaStoreId = i.toLong(),
                contentUri = "content://media/external/audio/media/$i",
                title = "Nebula Track $i",
                artist = "Artist ${i % 120}",
                album = "Album ${i % 350}",
                albumId = (i % 350).toLong(),
                genre = if (i % 2 == 0) "Electronic" else "Ambient",
                durationMs = 210_000L + (i % 60) * 1000L,
                sizeBytes = 5_000_000L,
                dateAddedSec = 1_700_000_000L + i,
                trackNumber = (i % 15) + 1,
                year = 2025,
                filePath = "/storage/emulated/0/Music/track_$i.flac",
                folderName = "Music",
                folderPath = "/storage/emulated/0/Music",
                mimeType = "audio/flac"
            )
        }

        val startMs = System.currentTimeMillis()
        db.songDao().insertAll(batch)
        val visible = db.songDao().observeVisibleSongs().first()
        val elapsedMs = System.currentTimeMillis() - startMs

        assertEquals(5000, visible.size)
        assertTrue("5000-track insert + query should complete swiftly (<10s), took ${elapsedMs}ms", elapsedMs < 10_000)

        val plId = db.playlistDao().insertPlaylist(PlaylistEntity(name = "Cosmic Mix"))
        val firstThreeIds = visible.take(3).map { it.id }
        val reversedIds = firstThreeIds.reversed()
        db.playlistDao().replacePlaylistOrder(plId, reversedIds)

        val playlistSongs = db.playlistDao().observeSongsForPlaylist(plId).first()
        assertEquals(reversedIds, playlistSongs.map { it.id })
    }
}
