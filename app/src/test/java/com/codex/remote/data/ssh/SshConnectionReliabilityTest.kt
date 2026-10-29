package com.codex.remote.data.ssh

import net.schmizz.keepalive.KeepAliveProvider
import net.schmizz.keepalive.KeepAliveRunner
import net.schmizz.sshj.SSHClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SshConnectionReliabilityTest {
    @Test
    fun configuresKeepAliveWithBoundedFailureDetection() {
        val config = androidCompatibleSshConfig()
        val ssh = SSHClient(config)
        try {
            configureSshKeepAlive(ssh)

            assertSame(KeepAliveProvider.KEEP_ALIVE, config.keepAliveProvider)
            assertEquals(30, ssh.connection.keepAlive.keepAliveInterval)
            val keepAlive = ssh.connection.keepAlive
            assertTrue(keepAlive is KeepAliveRunner)
            assertEquals(3, (keepAlive as KeepAliveRunner).maxAliveCount)
        } finally {
            runCatching { ssh.close() }
        }
    }

    @Test
    fun recognizesDisconnectedTransportMessages() {
        assertTrue(IllegalStateException("Not connected").isRemoteConnectionLost())
        assertTrue(IOException("Broken pipe").isRemoteConnectionLost())
        assertTrue(IllegalStateException("wrapper", IOException("connection reset")).isRemoteConnectionLost())
        assertTrue(IllegalStateException("远端 app-server 已断开").isRemoteConnectionLost())
        assertTrue(isRemoteConnectionLost(IllegalArgumentException("unrelated"), connected = false))
    }

    @Test
    fun doesNotMisclassifyCcSwitchBusinessErrorsAsDisconnects() {
        assertFalse(IllegalStateException("provider configuration is invalid").isRemoteConnectionLost())
        assertFalse(isRemoteConnectionLost(IllegalStateException("usage script failed"), connected = true))
    }
}
