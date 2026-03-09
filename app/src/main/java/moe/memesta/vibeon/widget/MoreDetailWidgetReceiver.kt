package moe.memesta.vibeon.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Widget receiver for More Detail widget – a 400x400dp widget with dynamic theming
 * based on album art colors using Material 3 color schemes.
 */
class MoreDetailWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoreDetailWidget()
}
