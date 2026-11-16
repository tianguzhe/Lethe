package io.github.xiaolong125.lethe.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "IconLoader"
private const val CACHE_BYTES = 12 * 1024 * 1024

/**
 * Decodes app icons on demand and keeps the most recently shown ones in a byte-bounded LRU cache, instead of
 * holding a bitmap for every installed package in memory.
 */
class IconLoader(context: Context, sizeDp: Int) {
  private val pm = context.packageManager
  private val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
  private val cache = object : LruCache<String, ImageBitmap>(CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
  }

  // Limits concurrent decodes during fast flings so the IO pool is not flooded.
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  private val decodeDispatcher = Dispatchers.IO.limitedParallelism(4)

  fun cached(pkg: String): ImageBitmap? = cache.get(pkg)

  /** @return the icon, or null when the package has none readable (UI then shows the default icon). */
  suspend fun load(pkg: String): ImageBitmap? {
    cache.get(pkg)?.let { return it }
    return withContext(decodeDispatcher) {
      try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
        } else {
          @Suppress("DEPRECATION")
          pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }
        pm.getApplicationIcon(info).toBitmap(sizePx, sizePx).asImageBitmap().also { cache.put(pkg, it) }
      } catch (e: Exception) {
        Log.w(TAG, "icon failed for $pkg", e)
        null
      }
    }
  }

  /** Drops a stale icon after the package was updated or removed. */
  fun evict(pkg: String) {
    cache.remove(pkg)
  }
}
