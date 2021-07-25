package com.gabchmel.contextmusicplayer.playlistScreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.gabchmel.contextmusicplayer.R
import com.gabchmel.contextmusicplayer.Song
import com.gabchmel.contextmusicplayer.extensions.getAlbumArt
import com.gabchmel.contextmusicplayer.extensions.getArtist
import com.gabchmel.contextmusicplayer.extensions.getTitle
import com.gabchmel.contextmusicplayer.theme.JetnewsTheme
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class SongListFragment : Fragment() {

    private val viewModel: SongListViewModel by viewModels()
    private var _isPermGranted = MutableStateFlow(false)
    private var isPermGranted: StateFlow<Boolean> = _isPermGranted

    // permissions callback
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // permission is granted
                _isPermGranted.value = true
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Handle the back button event
            val a = Intent(Intent.ACTION_MAIN)
            a.addCategory(Intent.CATEGORY_HOME)
            a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(a)
        }

        // compose layout for this fragment
        return ComposeView(requireContext()).apply {
            setContent {
                val songs by viewModel.songs.collectAsState()

                JetnewsTheme {
                    val onPrimary = MaterialTheme.colors.onPrimary

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        "Song List",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                actions = {
                                    IconButton(
                                        onClick = { findNavController().navigate(R.id.settingsFragment) },
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                                            contentDescription = "Settings",
                                            modifier = Modifier.fillMaxHeight(0.4f),
                                            tint = onPrimary
                                        )
                                    }
                                },
                                elevation = 0.dp,
                            )
                        },
                        content = {
                            // Component to grant the permissions
                            val granted by isPermGranted.collectAsState()
                            if ((ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED) && !granted
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize()
                                        .padding(vertical = 16.dp)
                                ) {
                                    Text(
                                        text = "The permission to access external storage needed.",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()

                                    ) {
                                        Button({

//                                            openDirectory(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

                                            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                                        }) {
                                            Text("Grant permission")
                                        }
                                    }
                                }
                            } else if (granted || (ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) == PackageManager.PERMISSION_GRANTED)
                            ) {
                                Column {
                                    val isRefreshing by viewModel.isRefreshing.collectAsState()
//                                    viewModel.loadSongs()

                                    SwipeRefresh(
                                        state = rememberSwipeRefreshState(isRefreshing),
                                        onRefresh = { viewModel.refresh() },
                                    ) {
                                        if (songs != null)
                                            LazyColumn(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                items(songs!!) { song ->
                                                    SongRow(song)
                                                }
                                            }
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            val musicState by viewModel.musicState.collectAsState()
                            val musicMetadata by viewModel.musicMetadata.collectAsState()
                            val connected by viewModel.connected.collectAsState()

                            val fontColor = MaterialTheme.colors.onPrimary

                            if (connected) {

                                BottomAppBar(
//                                    Modifier.clickable(
//                                        onClick = {
//                                            val play = false
//                                            findNavController().navigate(
//                                                SongListFragmentDirections.actionSongListFragmentToHomeFragment(
//                                                    Uri.EMPTY,
//                                                    play
//                                                )
//                                            )
//                                        }
//                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(end = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            // Album art
                                            Image(
                                                painter = musicMetadata?.getAlbumArt()?.let {
                                                    rememberGlidePainter(it)
                                                }
                                                    ?: rememberVectorPainter(
                                                        ImageVector.vectorResource(
                                                            R.drawable.ic_album_cover_vector3_colored
                                                        )
                                                    ),
                                                contentDescription = "Album Art",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(percent = 10))
                                                    .height(46.dp)
                                            )
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = musicMetadata?.getTitle() ?: "Loading",
                                                    color = fontColor
                                                )
                                                Text(
                                                    text = musicMetadata?.getArtist() ?: "Loading",
                                                    color = fontColor,
                                                    modifier = Modifier.alpha(0.54f)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                playSong()
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                painter = rememberVectorPainter(
                                                    ImageVector.vectorResource(
                                                        if (musicState?.state == PlaybackStateCompat.STATE_PLAYING)
                                                            R.drawable.ic_pause_new
                                                        else
                                                            R.drawable.ic_play_button_arrowhead
                                                    )
                                                ),
                                                contentDescription = "Play",
                                                tint = Color(0xFFB1B1B1)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Function for creating one song compose object row
    @Composable
    fun SongRow(song: Song) {

        val fontColor = MaterialTheme.colors.onPrimary
        Row(
            Modifier
                .clickable(onClick = {
                    val play = true
                    findNavController().navigate(
                        SongListFragmentDirections.actionSongListFragmentToHomeFragment(
                            song.URI,
                            play
                        )
                    )
                })
        ) {
            // Album art
            Image(
                painter = song.albumArt?.let {
                    rememberGlidePainter(it)
                }
                    ?: rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_album_cover_vector3)),
                contentDescription = "Album Art",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(percent = 10))
                    .height(36.dp)
            )
            Column(
                Modifier
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "${song.title}",
                    fontWeight = FontWeight.W400,
                    color = fontColor,
                    fontSize = 16.sp
                )
                Text(
                    text = "${song.author}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                    color = fontColor,
                    modifier = Modifier.alpha(0.54f)
                )
            }
        }
    }

    private fun playSong() {
        val pbState = viewModel.musicState.value?.state ?: return
        if (pbState == PlaybackStateCompat.STATE_PLAYING) {
            viewModel.pause()

//            // Preemptively set icon
//            binding.btnPlay.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp)
        } else {
            if (viewModel.notPlayed) {
//                viewModel.play(uri)
            } else {
                viewModel.play()
            }

//            // Preemptively set icon
//            binding.btnPlay.setBackgroundResource(R.drawable.ic_pause_black_24dp)
        }
    }

//    fun openDirectory(pickerInitialUri: Uri) {
//        // Choose a directory using the system's file picker.
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//            // Optionally, specify a URI for the directory that should be opened in
//            // the system file picker when it loads.
//            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
//        }
//
//        startActivityForResult(intent, 1234)
//    }
//
//    override fun onActivityResult(
//        requestCode: Int, resultCode: Int, resultData: Intent?) {
//        if (requestCode == your-request-code
//            && resultCode == Activity.RESULT_OK) {
//            // The result data contains a URI for the document or directory that
//            // the user selected.
//            resultData?.data?.also { uri ->
//                // Perform operations on the document using its URI.
//            }
//        }
//    }

    // Function for preview
    @Preview
    @Composable
    fun ExampleSongRow() {
        SongRow(Song("Title", "author", null, Uri.EMPTY))
    }
}



