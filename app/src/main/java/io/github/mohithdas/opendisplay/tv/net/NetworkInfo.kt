package io.github.mohithdas.opendisplay.tv.net

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Local IPv4 lookup for the manual-connection fallback: when mDNS discovery
 * doesn't reach the Mac (some routers/corporate networks block multicast),
 * the user can type this address + port straight into the Mac app's host
 * override instead.
 *
 * Uses [NetworkInterface] rather than the deprecated
 * `WifiManager.connectionInfo.ipAddress` so it keeps working over Ethernet
 * adapters or a USB-tethering-provided interface too, not just WiFi.
 */
object NetworkInfo {

    fun localIPv4Address(context: Context): String? = localIPv4InetAddress(context)?.hostAddress

    /** Same lookup as [localIPv4Address], as an [Inet4Address] ready to bind a
     * [java.net.ServerSocket] to directly — so the socket only listens on the
     * WiFi-reachable interface instead of every interface (see SECURITY.md/SCR-006). */
    fun localIPv4InetAddress(context: Context): Inet4Address? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val activeAddress = try {
            val network = connectivity?.activeNetwork
            connectivity?.getLinkProperties(network)?.linkAddresses
                ?.asSequence()
                ?.map { it.address }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
        } catch (_: Exception) {
            null
        }
        if (activeAddress != null) return activeAddress
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLinkLocalAddress }
        } catch (e: Exception) {
            null
        }
    }
}
