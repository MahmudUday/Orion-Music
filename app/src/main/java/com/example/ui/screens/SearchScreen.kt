package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Genre
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.OrionArtwork
import com.example.ui.components.SectionHeader
import com.example.ui.components.SongRowItem
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.PlaybackUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    libraryState: LibraryUiState,
    playlists: List<Playlist>,
    recentSearches: List<String>,
    playbackState: PlaybackUiState,
    onRecordSearch: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMore: (Song) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    contentBottomPadding: PaddingValues
) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()

    val matchingSongs = remember(trimmed, libraryState.allSongs) {
        if (trimmed.isEmpty()) emptyList()
        else libraryState.allSongs.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.artist.contains(trimmed, ignoreCase = true) ||
                it.album.contains(trimmed, ignoreCase = true) ||
                it.folderName.contains(trimmed, ignoreCase = true)
        }
    }
    val matchingAlbums = remember(trimmed, libraryState.albums) {
        if (trimmed.isEmpty()) emptyList()
        else libraryState.albums.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.artist.contains(trimmed, ignoreCase = true)
        }
    }
    val matchingArtists = remember(trimmed, libraryState.artists) {
        if (trimmed.isEmpty()) emptyList()
        else libraryState.artists.filter { it.name.contains(trimmed, ignoreCase = true) }
    }
    val matchingGenres = remember(trimmed, libraryState.genres) {
        if (trimmed.isEmpty()) emptyList()
        else libraryState.genres.filter { it.name.contains(trimmed, ignoreCase = true) }
    }
    val matchingPlaylists = remember(trimmed, playlists) {
        if (trimmed.isEmpty()) emptyList()
        else playlists.filter { it.name.contains(trimmed, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Songs, artists, albums, genres, playlists...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 16.dp)
        ) {
            if (trimmed.isEmpty()) {
                if (recentSearches.isNotEmpty()) {
                    item(key = "recent_searches") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onClearSearchHistory) { Text("Clear") }
                        }
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { term ->
                                AssistChip(
                                    onClick = { query = term },
                                    label = { Text(term) }
                                )
                            }
                        }
                    }
                } else {
                    item(key = "search_prompt") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Instant Offline Search", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Search across all ${libraryState.stats.totalSongs} local tracks, albums, artists, genres, and playlists.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val totalMatches = matchingSongs.size + matchingAlbums.size + matchingArtists.size + matchingGenres.size + matchingPlaylists.size
                if (totalMatches == 0) {
                    item(key = "no_results") {
                        Text(
                            text = "No offline results for \"$trimmed\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                if (matchingPlaylists.isNotEmpty()) {
                    item { SectionHeader("Playlists") }
                    items(matchingPlaylists, key = { "sp_${it.id}" }) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    onRecordSearch(trimmed)
                                    onOpenPlaylist(pl)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrionArtwork(pl.artworkUri, pl.name, Modifier.size(44.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pl.name, style = MaterialTheme.typography.titleSmall)
                                Text("${pl.songCount} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (matchingArtists.isNotEmpty()) {
                    item { SectionHeader("Artists") }
                    items(matchingArtists, key = { "sar_${it.name}" }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    onRecordSearch(trimmed)
                                    onOpenArtist(artist)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(artist.name, style = MaterialTheme.typography.titleSmall)
                                Text("${artist.songCount} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (matchingAlbums.isNotEmpty()) {
                    item { SectionHeader("Albums") }
                    items(matchingAlbums, key = { "sal_${it.id}_${it.name}" }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    onRecordSearch(trimmed)
                                    onOpenAlbum(album)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrionArtwork(album.artworkUri, album.name, Modifier.size(44.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (matchingGenres.isNotEmpty()) {
                    item { SectionHeader("Genres") }
                    items(matchingGenres, key = { "sg_${it.name}" }) { genre ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    onRecordSearch(trimmed)
                                    onOpenGenre(genre)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${genre.name} (${genre.songCount})", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                if (matchingSongs.isNotEmpty()) {
                    item { SectionHeader("Songs (${matchingSongs.size})") }
                    itemsIndexed(matchingSongs, key = { _, s -> "ss_${s.id}" }) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrent = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                onRecordSearch(trimmed)
                                onPlaySongs(matchingSongs, idx, false)
                            },
                            onFavoriteClick = { onToggleFavorite(song.id) },
                            onMoreClick = { onOpenSongMore(song) },
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
