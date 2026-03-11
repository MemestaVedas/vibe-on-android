package moe.memesta.vibeon.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/**
 * Handles widget button clicks and forwards requests to the WebSocket client.
 */
class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val keyAction = ActionParameters.Key<String>("action")
        val action = parameters[keyAction] ?: return
        val client = (context.applicationContext as? moe.memesta.vibeon.VibeonApp)
            ?.container?.webSocketClient ?: return

        when (action) {
            "play_pause" -> {
                val isPlaying = moe.memesta.vibeon.MediaNotificationManager.wsClient
                    ?.isPlaying?.value ?: false
                if (isPlaying) client.sendPause() else client.sendPlay()
            }
            "prev" -> client.sendPrevious()
            "next" -> client.sendNext()
            "toggle_output" -> {
                val isMobile = moe.memesta.vibeon.MediaNotificationManager.isMobilePlayback
                if (isMobile) client.sendStopMobilePlayback()
                else client.sendStartMobilePlayback()
            }
            "like" -> {
                val path = moe.memesta.vibeon.MediaNotificationManager.currentTrackPath ?: return
                client.sendToggleFavorite(path)
            }
            "toggle_more_options" -> {
                val dataStore = WidgetStateDefinition.getDataStore(context, "")
                dataStore.updateData { it.copy(showingMoreOptions = true) }
                VibeonWidget().updateAll(context)
            }
            "close_more_options" -> {
                val dataStore = WidgetStateDefinition.getDataStore(context, "")
                dataStore.updateData { it.copy(showingMoreOptions = false) }
                VibeonWidget().updateAll(context)
            }
            "toggle_shuffle" -> {
                client.sendToggleShuffle()
            }
            "cycle_repeat" -> {
                client.sendToggleRepeat()
            }
            "cycle_volume" -> {
                // Get current volume from state
                val wsVolume = moe.memesta.vibeon.MediaNotificationManager.wsClient?.volume?.value ?: 1.0
                val nextVolume = when {
                    wsVolume > 0.51 -> 0.5 // >50 -> 50
                    wsVolume > 0.01 -> 0.0 // 50 -> 0
                    else -> 1.0 // 0 -> 100
                }
                client.sendSetVolume(nextVolume)
            }
        }
    }
}

