package io.github.widgettest

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL
import android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.P
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val TAG = "widgets_repository"

class WidgetsRepository(
    private val widgetManager: AppWidgetManager,
    private val host: AppWidgetHost
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        host.deleteHost()
    }

    private val _widgets = MutableStateFlow(emptyList<Widget>())
    val widgets: StateFlow<List<Widget>> = _widgets.asStateFlow()

    private fun reload() {
        val widgets = mutableListOf<Widget>()
        host.appWidgetIds.forEach { id ->
            widgets.add(
                Widget(id = id)
            )
        }
        _widgets.value = widgets
    }

    fun startListening() {
        coroutineScope.launch {
            host.startListening()
        }
        Log.i(TAG, "Started listening for widget updates")
    }

    fun stopListening() {
        host.stopListening()
        Log.i(TAG, "Stopped listening for widget updates")
    }

    fun onWidgetConfigurationResult(result: WidgetConfigurationResult) {
        coroutineScope.launch {
            val id = result.widgetId
            if (id == INVALID_APPWIDGET_ID) {
                Log.e(
                    TAG,
                    "Failed to add widget: ACTION_APPWIDGET_CONFIGURE did not return a valid widget id"
                )
                return@launch
            }
            if (!result.success) {
                    Log.e(
                        TAG,
                        "Failed to add widget: ACTION_APPWIDGET_CONFIGURE was not successful"
                    )
                    host.deleteAppWidgetId(result.widgetId)
                return@launch
            }
            Log.i(TAG, "Successfully configured widget")
            reload()
        }
    }

    fun getViewForWidget(widget: Widget, context: Context): AppWidgetHostView =
        host.createView(context, widget.id, widgetManager.getAppWidgetInfo(widget.id))

    @SuppressLint("InlinedApi")
    fun onAddWidgetResult(
        result: AddWidgetResult,
        configureWidget: (WidgetConfigurationRequest) -> Unit
    ) {
        coroutineScope.launch {
            val id = result.widgetId
            if (id == INVALID_APPWIDGET_ID) {
                Log.e(
                    TAG,
                    "Failed to add widget: ACTION_APPWIDGET_PICK did not return a valid widget id"
                )
                return@launch
            }
            if (!result.success) {
                host.deleteAppWidgetId(result.widgetId)
                return@launch
            }
            val info = widgetManager.getAppWidgetInfo(id)
            //Configuration is required if the widget has a configuration activity and, on Android 9+, isn't marked as both optional and reconfigurable
            val configurationRequired = info.configure != null && (VERSION.SDK_INT < P ||
                    !info.widgetFeatures.hasFlagSet(WIDGET_FEATURE_CONFIGURATION_OPTIONAL) ||
                    !info.widgetFeatures.hasFlagSet(WIDGET_FEATURE_RECONFIGURABLE))
            if (configurationRequired) {
                configureWidget(
                    WidgetConfigurationRequest(
                        widgetId = id,
                        configurationActivity = info.configure
                    )
                )
                Log.i(
                    TAG,
                    "Starting initial configuration for widget ${info.provider.flattenToShortString()}"
                )
            } else {
                Log.i(TAG, "Added widget ${info.provider.flattenToShortString()}")
                reload()
            }
        }
    }

    fun allocateWidgetId(): Int = host.allocateAppWidgetId()

    companion object {
        data class WidgetConfigurationRequest(
            internal val widgetId: Int,
            internal val configurationActivity: ComponentName
        )

        data class WidgetConfigurationResult(
            internal val widgetId: Int,
            internal val success: Boolean
        )

        object ConfigureWidget :
            ActivityResultContract<WidgetConfigurationRequest, WidgetConfigurationResult>() {
            override fun createIntent(context: Context, input: WidgetConfigurationRequest) =
                Intent(ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(input.configurationActivity)
                    .putExtra(EXTRA_APPWIDGET_ID, input.widgetId)

            override fun parseResult(resultCode: Int, intent: Intent?) =
                WidgetConfigurationResult(
                    success = resultCode == RESULT_OK,
                    widgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
                        ?: INVALID_APPWIDGET_ID
                )
        }

        data class AddWidgetResult(
            internal val widgetId: Int,
            internal val success: Boolean
        )

        object AddWidget : ActivityResultContract<Int, AddWidgetResult>() {
            override fun createIntent(context: Context, input: Int) =
                Intent(ACTION_APPWIDGET_PICK).putExtra(EXTRA_APPWIDGET_ID, input)
            override fun parseResult(resultCode: Int, intent: Intent?): AddWidgetResult =
                AddWidgetResult(
                    success = resultCode == RESULT_OK,
                    widgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
                        ?: INVALID_APPWIDGET_ID
                )
        }
    }
}

private fun Int.hasFlagSet(flag: Int): Boolean = (this and flag) > 0
