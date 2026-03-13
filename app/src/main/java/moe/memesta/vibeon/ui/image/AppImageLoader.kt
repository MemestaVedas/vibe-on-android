package moe.memesta.vibeon.ui.image

import android.content.Context
import coil.ImageLoader

object AppImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        val existing = instance
        if (existing != null) return existing

        return synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext).build().also {
                instance = it
            }
        }
    }
}
