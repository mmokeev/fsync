package com.github.mmokeev.fsync

import com.github.mmokeev.fsync.utils.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.*
import java.io.File

class WatchServiceTests {

    @Test
    fun `watch current directory for initalization event` () {
        runBlocking {
            val currentDirectory  = File(System.getProperty("user.dir"))

            val watchChannel = currentDirectory.asWatchChannel()

            assertFalse(watchChannel.isClosedForSend)
            assertNull(watchChannel.tag)
            assertEquals(currentDirectory.absolutePath, watchChannel.file.absolutePath)
            assertEquals(KWatchChannel.Mode.Recursive, watchChannel.mode)

            launch {
                watchChannel.consumeEach { event ->
                    // there is always the first event triggered and here we only test that
                    assertEquals(KWatchEvent.State.Initialized, event.state)
                    assertEquals(currentDirectory.absolutePath, event.file.absolutePath)
                }
            }

            assertFalse(watchChannel.isClosedForSend)

            watchChannel.close()

            assertTrue(watchChannel.isClosedForSend)
        }
    }
}