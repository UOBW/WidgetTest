package io.github.widgettest

import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat

@Composable
fun WidgetComposable(
    widget: Widget,
    widgetsViewModel: WidgetsViewModel
) {
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
                .scrollable(
                    state = rememberScrollableState { it },
                    orientation = Vertical
                )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                widgetsViewModel.getViewForWidget(widget, context)
                    .apply {
                        ViewCompat.setNestedScrollingEnabled(this, true)
                    }
            },
            update = { widgetView ->
                widgetView.updateAppWidgetOptions(Bundle().apply {
                    putInt(OPTION_APPWIDGET_MIN_WIDTH, maxWidth.int)
                    putInt(OPTION_APPWIDGET_MIN_HEIGHT, maxHeight.int)
                    putInt(OPTION_APPWIDGET_MAX_WIDTH, maxWidth.int)
                    putInt(OPTION_APPWIDGET_MAX_HEIGHT, maxHeight.int)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        putParcelableArrayList(
                            OPTION_APPWIDGET_SIZES,
                            arrayListOf(SizeF(maxWidth.value, maxHeight.value))
                        )
                    }
                })
                Log.i(TAG, "Updated widget size")
            }
        )
    }
}

private val Dp.int get() = value.toInt()
