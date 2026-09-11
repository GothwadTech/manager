package com.gothwad.manager

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class LanDevice(
    val ip: String,
    val hostname: String,
    val openPorts: List<Int>
)

object LanScanner {

    fun scanSubnet(context: Context, onProgress: (Int, Int) -> Unit, onComplete: (List<LanDevice>) -> Unit) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipInt == 0) {
            onComplete(emptyList())
            return
        }

        val ipStr = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
        val prefix = ipStr.substringBeforeLast(".") + "."

        val discovered = mutableListOf<LanDevice>()
        val executor = Executors.newFixedThreadPool(20)
        val totalHosts = 254
        var finished = 0

        for (i in 1..totalHosts) {
            val targetIp = "$prefix$i"
            executor.execute {
                try {
                    val address = InetAddress.getByName(targetIp)
                    // Check ping or probe ports 445 (SMB), 80 (HTTP), 21 (FTP), 8080
                    val ports = listOf(445, 80, 21, 8080)
                    val openPorts = mutableListOf<Int>()
                    for (port in ports) {
                        try {
                            val socket = Socket()
                            socket.connect(InetSocketAddress(targetIp, port), 200)
                            socket.close()
                            openPorts.add(port)
                        } catch (ignored: Exception) {}
                    }

                    if (openPorts.isNotEmpty() || address.isReachable(300)) {
                        val host = address.canonicalHostName ?: targetIp
                        synchronized(discovered) {
                            discovered.add(LanDevice(targetIp, host, openPorts))
                        }
                    }
                } catch (ignored: Exception) {}

                synchronized(executor) {
                    finished++
                    onProgress(finished, totalHosts)
                }
            }
        }

        executor.shutdown()
        Thread {
            executor.awaitTermination(15, TimeUnit.SECONDS)
            onComplete(discovered.sortedBy { it.ip })
        }.start()
    }
}
