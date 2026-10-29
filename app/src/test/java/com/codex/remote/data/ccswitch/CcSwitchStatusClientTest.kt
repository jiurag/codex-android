package com.codex.remote.data.ccswitch

import com.codex.remote.data.ssh.RemoteCommandResult
import com.codex.remote.domain.RemotePlatform
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CcSwitchStatusClientTest {
    @Test
    fun readsCurrentProviderAndQuotaWithoutCallingTheSecretBearingCurrentCommand() = runTest {
        val commands = mutableListOf<String>()
        val client = CcSwitchStatusClient(RemotePlatform.POSIX) { command, _ ->
            commands += command
            when {
                command.contains("provider list") -> RemoteCommandResult(
                    exitStatus = 0,
                    stdout = PROVIDER_LIST,
                    stderr = "",
                )

                command.contains("provider quota deepseek --json") -> RemoteCommandResult(
                    exitStatus = 0,
                    stdout = QUOTA_JSON,
                    stderr = "",
                )

                else -> error("Unexpected command: $command")
            }
        }

        val status = client.readCurrentStatus()

        assertEquals(2, commands.size)
        assertFalse(commands.any { it.contains("provider current") })
        assertEquals("deepseek", status?.providerId)
        assertEquals("DeepSeek", status?.providerName)
        assertEquals(true, status?.available)
        assertEquals("123.45", status?.quotas?.single()?.remaining)
        assertEquals("CNY", status?.quotas?.single()?.unit)
    }

    @Test
    fun missingCcSwitchFallsBackWithoutThrowing() = runTest {
        val client = CcSwitchStatusClient(RemotePlatform.POSIX) { _, _ ->
            RemoteCommandResult(
                exitStatus = 127,
                stdout = "",
                stderr = "cc-switch: command not found",
            )
        }

        assertNull(client.readCurrentStatus())
    }

    @Test
    fun installedCcSwitchFailureIsReportedWithoutUsingOpenAiRateLimits() = runTest {
        val client = CcSwitchStatusClient(RemotePlatform.POSIX) { _, _ ->
            RemoteCommandResult(
                exitStatus = 2,
                stdout = "",
                stderr = "provider configuration is invalid",
            )
        }

        val status = client.readCurrentStatus()

        assertEquals("error", status?.status)
        assertEquals(false, status?.available)
        assertEquals("provider configuration is invalid", status?.error)
    }

    @Test
    fun parsesAnsiAndCheckedTableWhenCurrentLineIsMissing() {
        val output = "\u001B[32m│ ✓ ┆ deepseek ┆ DeepSeek ┆ https://api.deepseek.com │\u001B[0m"

        assertEquals("deepseek", parseCurrentProviderId(output))
    }

    @Test
    fun malformedQuotaOutputBecomesAnExplicitStatusError() = runTest {
        val client = CcSwitchStatusClient(RemotePlatform.POSIX) { command, _ ->
            if (command.contains("provider list")) {
                RemoteCommandResult(0, PROVIDER_LIST, "")
            } else {
                RemoteCommandResult(
                    exitStatus = 2,
                    stdout = "",
                    stderr = "usage script failed",
                )
            }
        }

        val status = client.readCurrentStatus()

        assertEquals("deepseek", status?.providerId)
        assertEquals("error", status?.status)
        assertEquals(false, status?.available)
        assertEquals("usage script failed", status?.error)
        assertTrue(status?.quotas?.isEmpty() == true)
    }

    private companion object {
        val PROVIDER_LIST = """
            ┌───┬──────────┬──────────┬──────────────────────────┐
            │   ┆ ID       ┆ Name     ┆ API URL                  │
            ╞═══╪══════════╪══════════╪══════════════════════════╡
            │ ✓ ┆ deepseek ┆ DeepSeek ┆ https://api.deepseek.com │
            └───┴──────────┴──────────┴──────────────────────────┘

            ℹ Application: codex
            → Current: deepseek
        """.trimIndent()

        val QUOTA_JSON = """
            {
              "app": "codex",
              "providerId": "deepseek",
              "providerName": "DeepSeek",
              "status": "ok",
              "available": true,
              "queriedAt": 1793253980715,
              "result": {
                "type": "script",
                "quota": {
                  "success": true,
                  "data": [
                    {
                      "planName": "CNY",
                      "isValid": true,
                      "remaining": 123.45,
                      "unit": "CNY"
                    }
                  ]
                }
              },
              "error": null
            }
        """.trimIndent()
    }
}
