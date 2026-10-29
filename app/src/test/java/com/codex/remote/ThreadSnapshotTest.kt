package com.codex.remote

import com.codex.remote.domain.RemoteThread
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadSnapshotTest {
    @Test
    fun emptyServerListDoesNotHideAStartedThread() {
        val started = thread(id = "new-thread", title = "新会话", updatedAt = 20)

        val visible = mergeVisibleThreads(
            serverThreads = emptyList(),
            unlistedThreads = listOf(started),
        )

        assertEquals(listOf(started), visible)
    }

    @Test
    fun serverVersionReplacesTheLocalPlaceholderOnceListed() {
        val placeholder = thread(id = "new-thread", title = "新会话", updatedAt = 20)
        val persisted = thread(id = "new-thread", title = "真实标题", updatedAt = 30)
        val existing = thread(id = "older", title = "旧会话", updatedAt = 10)

        val visible = mergeVisibleThreads(
            serverThreads = listOf(persisted, existing),
            unlistedThreads = listOf(placeholder, existing),
        )

        assertEquals(listOf(persisted, existing), visible)
        assertEquals("真实标题", visible.first().title)
    }

    private fun thread(id: String, title: String, updatedAt: Long) = RemoteThread(
        id = id,
        title = title,
        cwd = "/root",
        updatedAt = updatedAt,
        status = "idle",
    )
}
