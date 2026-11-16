package io.github.xiaolong125.lethe.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Emits the package name whenever an app is installed, removed, updated or changes state outside this tool
 * (e.g. via system Settings), so the list never shows stale entries.
 */
fun packageChanges(context: Context): Flow<String> = callbackFlow {
  val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      intent.data?.schemeSpecificPart?.let { trySend(it) }
    }
  }
  val filter = IntentFilter().apply {
    addAction(Intent.ACTION_PACKAGE_ADDED)
    addAction(Intent.ACTION_PACKAGE_REMOVED)
    addAction(Intent.ACTION_PACKAGE_REPLACED)
    addAction(Intent.ACTION_PACKAGE_CHANGED)
    addDataScheme("package")
  }
  // System package broadcasts are still delivered to non-exported receivers.
  ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
  awaitClose { context.unregisterReceiver(receiver) }
}
