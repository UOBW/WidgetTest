package io.github.widgettest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.widgettest.WidgetsRepository.Companion.AddWidget
import io.github.widgettest.WidgetsRepository.Companion.ConfigureWidget
import io.github.widgettest.ui.theme.WidgetTestTheme

class MainActivity : ComponentActivity() {
    private val widgetsViewModel: WidgetsViewModel by viewModels { WidgetsViewModel.Factory }

    private val configureWidget = registerForActivityResult(ConfigureWidget) { result ->
        widgetsViewModel.onWidgetConfigurationResult(result)
    }

    private val addWidget = registerForActivityResult(AddWidget) { result ->
        widgetsViewModel.onAddWidgetResult(result, configureWidget = { configureWidget.launch(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WidgetTestTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp)
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Button(
                        onClick = { addWidget.launch(widgetsViewModel.allocateWidgetId()) }
                    ) {
                        Text("Add widget")
                    }
                    for(widget in widgetsViewModel.widgets.collectAsState().value) {
                        WidgetComposable(widget, widgetsViewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetsViewModel.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetsViewModel.stopListening()
    }
}
