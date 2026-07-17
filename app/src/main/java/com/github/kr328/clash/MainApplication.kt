package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.compat.currentProcessName
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.util.sendServiceRecreated
import com.github.kr328.clash.util.clashDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Suppress("unused")
class MainApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        val processName = currentProcessName

        Log.d("Process $processName started")

        if (processName == packageName) {
            // The service process re-runs extraction synchronously before the core
            // needs the files, so the UI process must not pay for the copy (tens of
            // MB on first launch after install/upgrade) on the main thread.
            Global.launch(Dispatchers.IO) { extractGeoFiles() }

            Remote.launch()
        } else {
            extractGeoFiles()

            sendServiceRecreated()
        }
    }

    private fun extractGeoFiles() {
        clashDir.mkdirs()

        val updateDate = packageManager.getPackageInfo(packageName, 0).lastUpdateTime

        // Only reap temp files that are clearly stale: another process may be
        // extracting right now, and deleting its live temp would break its rename.
        val staleBefore = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        clashDir.listFiles { f -> f.name.endsWith(".tmp") && f.lastModified() < staleBefore }
            ?.forEach { it.delete() }

        for (name in GEO_ASSETS) {
            val target = File(clashDir, name)
            if (target.exists() && target.lastModified() >= updateDate) {
                continue
            }

            // The UI and service processes may extract concurrently on cold start:
            // write to a per-process temp file and rename into place atomically.
            val temp = File(clashDir, "$name.${android.os.Process.myPid()}.tmp")
            try {
                FileOutputStream(temp).use { output ->
                    assets.open(name).use { it.copyTo(output) }
                }

                if (!temp.renameTo(target)) {
                    target.delete()

                    if (!temp.renameTo(target) && !target.exists()) {
                        Log.w("Unable to extract $name to $target")
                    }
                }
            } finally {
                temp.delete()
            }
        }
    }

    fun finalize() {
        Global.destroy()
    }

    companion object {
        private val GEO_ASSETS =
            listOf("geoip.metadb", "geosite.dat", "ASN.mmdb", "BundleMRS.7z")
    }
}
