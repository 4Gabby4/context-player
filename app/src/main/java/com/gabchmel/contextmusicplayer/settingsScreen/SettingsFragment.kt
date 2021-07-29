package com.gabchmel.contextmusicplayer.settingsScreen

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.fragment.findNavController
import com.gabchmel.contextmusicplayer.BuildConfig
import com.gabchmel.contextmusicplayer.MediaBrowserConnector
import com.gabchmel.contextmusicplayer.R
import com.gabchmel.contextmusicplayer.theme.JetnewsTheme
import com.gabchmel.contextmusicplayer.theme.appFontFamily
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Compose view of the Settings screen
        return ComposeView(requireContext()).apply {
            setContent {
                JetnewsTheme {
                    val materialYel400 = MaterialTheme.colors.onPrimary
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        "Settings",
                                        fontFamily = appFontFamily
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { findNavController().navigateUp() }) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                                            contentDescription = "Back",
                                            modifier = Modifier.fillMaxHeight(0.4f),
                                            tint = materialYel400
                                        )
                                    }
                                },
                                elevation = 0.dp,
                                backgroundColor = Color.Transparent
                            )
                        },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(28.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = {
                                            findNavController().navigate(
                                                SettingsFragmentDirections
                                                    .actionSettingsFragmentToSensorScreen()
                                            )
                                        }),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row {
                                        Icon(
                                            imageVector = Icons.Filled.Sensors,
                                            contentDescription = "Sensors",
                                        )
                                        Text(
                                            text = "On device sensors",
                                            color = materialYel400,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.NavigateNext,
                                        contentDescription = "NavigateToNext",
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = {
                                            findNavController().navigate(
                                                SettingsFragmentDirections
                                                    .actionSettingsFragmentToSensorValuesFragment()
                                            )
                                        }),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row {
                                        Icon(
                                            imageVector = Icons.Filled.DriveFileRenameOutline,
                                            contentDescription = "Back",
                                        )
                                        Text(
                                            text = "Collected sensor data",
                                            color = materialYel400,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.NavigateNext,
                                        contentDescription = "Settings",
                                    )
                                }
                                Row() {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Clicking on this causes recreation of the model and triggers new song prediction",
                                            color = materialYel400,
                                            textAlign = TextAlign.Justify
                                        )
                                        Button(
                                            onClick = {
                                                MediaBrowserConnector(
                                                    ProcessLifecycleOwner.get(),
                                                    requireContext()
                                                )
                                            },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "Recreate model",
                                                color = materialYel400,
                                            )
                                        }
                                    }
                                }
                                Row() {

                                    val openDialog = remember { mutableStateOf(false) }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Clicking on this deletes all collected sensor data used for song prediction",
                                            color = materialYel400,
                                            textAlign = TextAlign.Justify
                                        )
                                        Button(
                                            onClick = {
                                                openDialog.value = true
                                            },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "Delete saved data",
                                                color = materialYel400,
                                            )
                                        }
                                        if (openDialog.value) {

                                            AlertDialog(
                                                onDismissRequest = {
                                                    // Dismiss the dialog when the user clicks outside the dialog or on the back
                                                    // button. If you want to disable that functionality, simply use an empty
                                                    // onCloseRequest.
                                                    openDialog.value = false
                                                },
                                                title = {
                                                    Text(text = "Delete all saved data")
                                                },
                                                text = {
                                                    Text("Are you sure you want to delete all saved data?")
                                                },
                                                confirmButton = {
                                                    Button(

                                                        onClick = {
                                                            openDialog.value = false
                                                            val inputFile =
                                                                File(
                                                                    requireContext().filesDir,
                                                                    "data.csv"
                                                                )
                                                            if (inputFile.exists()) {
                                                                requireContext().deleteFile("data.csv")
                                                            }
                                                        }) {
                                                        Text("YES")
                                                    }
                                                },
                                                dismissButton = {
                                                    Button(

                                                        onClick = {
                                                            openDialog.value = false
                                                        }) {
                                                        Text("NO")
                                                    }
                                                }
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
}