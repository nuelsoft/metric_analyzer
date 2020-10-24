package com.soft.metric_analyzer

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.lang.Error
import java.lang.Exception
import java.util.*


class MainActivity : FlutterActivity() {
    private val CHANNEL = "analyzer/data"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "fetch") {
                fetch()
                result.success(null);
            } else {
                result.notImplemented()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun checkForPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        print(this.applicationInfo.uid)
        val mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, this.applicationInfo.uid, this.packageName)
        return mode == MODE_ALLOWED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun fetch() {
        if (!checkForPermission())
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

        if (!checkForPermission())
            return;

        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        var bucket: NetworkStats.Bucket
        // Get Wi-Fi traffic statistics for the device so far
        // Get Wi-Fi traffic statistics for the device so far
        bucket = networkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_WIFI, "", 0, System.currentTimeMillis())
        Log.i("Info", "Total: " + (bucket.rxBytes + bucket.txBytes))


        val tm: TelephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager;

        val subId = UUID.randomUUID().toString();

        val summaryStats: NetworkStats;
        var summaryRx = 0;
        var summaryTx = 0;

        val summaryBucket: NetworkStats.Bucket = NetworkStats.Bucket();
        var summaryTotal = 0;

        summaryStats = networkStatsManager.querySummary(NetworkCapabilities.TRANSPORT_WIFI, "", getTimesMonthMorning(), System.currentTimeMillis());

        do {
            summaryStats.getNextBucket(summaryBucket)
//            val summaryUid = summaryBucket.uid
//            if (uid === summaryUid) {
            summaryRx += summaryBucket.rxBytes.toInt()
            summaryTx += summaryBucket.txBytes.toInt()
//            }

            var packageName: String? = this.packageManager.getNameForUid(summaryBucket.uid)
//
            if (packageName != null) {

                Log.i(MainActivity::class.java.simpleName, "app name: " + getName(packageName) + " package name: " + this.packageManager.getNameForUid(summaryBucket.uid) + " uid:" + summaryBucket.uid + " rx:" + summaryBucket.rxBytes +
                        " tx:" + summaryBucket.txBytes)
            }

            summaryTotal += (summaryBucket.rxBytes + summaryBucket.txBytes.toInt()).toInt()
        } while (summaryStats.hasNextBucket())
    }

    fun getName(packageName: String): String {
        return try {
            val packageInfo = this.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            this.packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
        } catch (e: Exception) {
            packageName;
        }
    }

    fun getTimesMonthMorning(): Long {
        val cal: Calendar = Calendar.getInstance()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
        return cal.getTimeInMillis()
    }
}
