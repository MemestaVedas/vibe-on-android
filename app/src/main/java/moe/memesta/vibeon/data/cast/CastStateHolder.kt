package moe.memesta.vibeon.data.cast

import android.content.Context
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight cast route observer used as a foundation for full cast handoff.
 */
class CastStateHolder(context: Context) {
    private val mediaRouter = MediaRouter.getInstance(context.applicationContext)
    private val routeSelector: MediaRouteSelector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

    private val _routes = MutableStateFlow<List<MediaRouter.RouteInfo>>(emptyList())
    val routes: StateFlow<List<MediaRouter.RouteInfo>> = _routes.asStateFlow()

    private val _selectedRoute = MutableStateFlow<MediaRouter.RouteInfo?>(null)
    val selectedRoute: StateFlow<MediaRouter.RouteInfo?> = _selectedRoute.asStateFlow()

    private val callback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshRoutes()
        }

        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshRoutes()
        }

        override fun onRouteSelected(
            router: MediaRouter,
            route: MediaRouter.RouteInfo,
            reason: Int
        ) {
            _selectedRoute.value = route
            refreshRoutes()
        }

        override fun onRouteUnselected(
            router: MediaRouter,
            route: MediaRouter.RouteInfo,
            reason: Int
        ) {
            _selectedRoute.value = null
            refreshRoutes()
        }
    }

    fun startDiscovery() {
        mediaRouter.addCallback(
            routeSelector,
            callback,
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
        )
        refreshRoutes()
    }

    fun stopDiscovery() {
        mediaRouter.removeCallback(callback)
    }

    private fun refreshRoutes() {
        val routes = mutableListOf<MediaRouter.RouteInfo>()
        val count = mediaRouter.routes.size
        for (i in 0 until count) {
            val route = mediaRouter.routes[i]
            if (route.isEnabled) {
                routes.add(route)
            }
        }
        _routes.value = routes
        _selectedRoute.value = mediaRouter.selectedRoute
    }
}
