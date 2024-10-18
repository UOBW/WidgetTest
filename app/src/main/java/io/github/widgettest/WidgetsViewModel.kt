package io.github.widgettest

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.widgettest.WidgetsRepository.Companion.AddWidgetResult
import io.github.widgettest.WidgetsRepository.Companion.WidgetConfigurationRequest
import kotlinx.coroutines.flow.StateFlow

class WidgetsViewModel(
    private val widgetsRepository: WidgetsRepository
) : ViewModel() {
    val widgets: StateFlow<List<Widget>> get() = widgetsRepository.widgets

    fun startListening() = widgetsRepository.startListening()
    fun stopListening() = widgetsRepository.stopListening()

    fun getViewForWidget(widget: Widget, context: Context) =
        widgetsRepository.getViewForWidget(widget, context)

    fun onAddWidgetResult(result: AddWidgetResult, configureWidget: (WidgetConfigurationRequest) -> Unit): Unit =
        widgetsRepository.onAddWidgetResult(result, configureWidget)

    fun onWidgetConfigurationResult(result: WidgetsRepository.Companion.WidgetConfigurationResult) =
        widgetsRepository.onWidgetConfigurationResult(result)

    fun allocateWidgetId(): Int =
        widgetsRepository.allocateWidgetId()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                WidgetsViewModel(
                    WidgetsRepository(
                        host = AppWidgetHost(application, 0),
                        widgetManager = AppWidgetManager.getInstance(application),
                    )
                )
            }
        }
    }
}
