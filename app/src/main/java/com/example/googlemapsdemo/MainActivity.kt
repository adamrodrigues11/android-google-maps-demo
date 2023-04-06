// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.googlemapsdemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.*

private const val TAG = "BasicMapActivity"

val vancouver = LatLng(49.2827, -123.1207)
val defaultCameraPosition = CameraPosition.fromLatLngZoom(vancouver, 11f)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isMapLoaded by remember { mutableStateOf(false) }
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMapView(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                )
                // display progress indicator while map is loading
                if (!isMapLoaded) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .matchParentSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(MaterialTheme.colors.background)
                                .wrapContentSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    onMapLoaded: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    // move circle to marker's current state when done dragging
    val markerState = rememberMarkerState(position = vancouver)
    var circleCenter by remember { mutableStateOf(vancouver) }
    var circleRadius by remember { mutableStateOf(1000.0f) }
    if (markerState.dragState == DragState.END) {
        circleCenter = markerState.position
    }
    // define map state variables and initialize default state
    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }
    var mapVisible by remember { mutableStateOf(true) }

    if (mapVisible) {
        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapLoaded = onMapLoaded,
            onPOIClick = {
                Log.d(TAG, "POI clicked: ${it.name}")
            }
        ) {
            // Handler for marker click. Shows info window and logs coordinates or marker.
            val markerClick: (Marker) -> Boolean = {
                Log.d(TAG, "The marker is at coordinates " +
                        "(${it.position.latitude}, ${it.position.longitude})")
                it.showInfoWindow()
                false
            }
            // Handler for marker info window click. Closes info window
            val closeInfoWindow: (Marker) -> Unit = {
                it.hideInfoWindow()
            }

            // marker info window
            MarkerInfoWindowContent(
                state = markerState,
                title = "The marker is at coordinates " +
                        "(${markerState.position.latitude}, ${markerState.position.longitude})",
                onClick = markerClick,
                onInfoWindowClick = closeInfoWindow,
                draggable = true
            ) {
                Text(it.title ?: "Marker was clicked!", color = Color.Red)
            }
            Circle(
                center = circleCenter,
                fillColor = MaterialTheme.colors.secondary,
                strokeColor = MaterialTheme.colors.secondaryVariant,
                radius = circleRadius.toDouble(),
            )
            content()
        }
    }
    Column {
        // hide/show map
        MapButton(
            text = "Hide/Show Map",
            onClick = { mapVisible = !mapVisible },
            modifier = Modifier.testTag("toggleMapVisibility"),
        )
        // map reset button
        MapButton(
            text = "Reset Map",
            onClick = {
                mapProperties = mapProperties.copy(mapType = MapType.NORMAL)
                cameraPositionState.position = defaultCameraPosition
                markerState.position = vancouver
                markerState.hideInfoWindow()
            }
        )
        // generate other map type control buttons
        // log the type of map overlay chosen
        MapTypeControls(onMapTypeClick = {
            Log.d(TAG, "Selected map type $it")
            mapProperties = mapProperties.copy(mapType = it)
        })

        // toggle zoom controls
        ZoomControls(
            uiSettings.zoomControlsEnabled,
            onZoomControlsCheckedChange = {
                uiSettings = uiSettings.copy(zoomControlsEnabled = it)
            }
        )

        // circle radius controls
        CircleRadiusControls(
            onValueChange = {
                circleRadius = it
            }
        )

    }
}

@Composable
private fun MapTypeControls(
    onMapTypeClick: (MapType) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(state = ScrollState(0)),
        horizontalArrangement = Arrangement.Center
    ) {
        MapType.values().forEach {
            MapTypeButton(type = it) { onMapTypeClick(it) }
        }
    }
}

@Composable
private fun MapTypeButton(type: MapType, onClick: () -> Unit) =
    MapButton(text = type.toString(), onClick = onClick)


@Composable
private fun MapButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier.padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.onPrimary,
            contentColor = MaterialTheme.colors.primary
        ),
        onClick = onClick
    ) {
        Text(text = text, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun ZoomControls(
    isZoomControlsEnabledChecked: Boolean,
    onZoomControlsCheckedChange: (Boolean) -> Unit,
) {
    Text(text = "Zoom Controls On?")
    Switch(
        isZoomControlsEnabledChecked,
        onCheckedChange = onZoomControlsCheckedChange
    )
}

@Composable
fun CircleRadiusControls(
    onValueChange: (Float) -> Unit
) {
    var sliderPosition by remember {
        mutableStateOf(1000.0f)
    }
    Column(
        Modifier.width(100.dp)
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onValueChange(it)
            },
            valueRange = 1000f..200000f // in m
        )
        Text(
            text = "Radius = ${(sliderPosition / 1000 ).toInt().toString()} km"
        )
    }

}

@Preview
@Composable
fun GoogleMapViewPreview() {
    GoogleMapView(Modifier.fillMaxSize())
}
