package io.github.mohithdas.opendisplay.tv.net

import java.net.InetAddress
import java.net.ServerSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ListenerPortBinderTest {
    private val loopback = InetAddress.getByName("127.0.0.1")

    @Test
    fun bindsPreferredPortWhenAvailable() {
        val port = ServerSocket(0, 50, loopback).use { it.localPort }
        ListenerPortBinder.bindFirstAvailable(loopback, port, port).use {
            assertEquals(port, it.localPort)
        }
    }

    @Test
    fun fallsForwardSequentiallyWhenPortsAreOccupied() {
        ServerSocket(0, 50, loopback).use { occupied ->
            if (occupied.localPort >= 65534) return
            ListenerPortBinder.bindFirstAvailable(
                loopback,
                occupied.localPort,
                occupied.localPort + 2,
            ).use { selected ->
                assertNotEquals(occupied.localPort, selected.localPort)
                assertEquals(occupied.localPort + 1, selected.localPort)
            }
        }
    }

    @Test
    fun reportsExhaustedRange() {
        ServerSocket(0, 50, loopback).use { occupied ->
            assertThrows(ListenerPortsUnavailableException::class.java) {
                ListenerPortBinder.bindFirstAvailable(loopback, occupied.localPort, occupied.localPort)
            }
        }
    }
}

