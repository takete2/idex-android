package dev.idex.android.ui.sprites

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

class SpriteStore(context: Context) {
    private val assets = context.applicationContext.assets
    private val cache = object : LruCache<Int, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    fun get(id: Int): Bitmap? {
        cache.get(id)?.let { return it }
        val bitmap = runCatching {
            assets.open("sprites/%03d.png".format(id)).use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        cache.put(id, bitmap)
        return bitmap
    }
}
