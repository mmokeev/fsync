package com.github.mmokeev.fsync.services

import com.github.mmokeev.fsync.setup.properties.*
import com.github.mmokeev.fsync.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.*
import org.springframework.boot.context.event.*
import org.springframework.context.event.*
import org.springframework.stereotype.*
import java.io.*
import java.lang.IllegalStateException
import java.lang.management.*
import java.nio.file.*
import java.util.concurrent.*

private val logger = KotlinLogging.logger {}

@ExperimentalStdlibApi
@Service
class SyncService(
    private val directoryProperties: DirectoryProperties,
    private val syncDispatcher: SyncDispatcher
) {

    @EventListener(ApplicationReadyEvent::class)
    fun sync() = runBlocking {
        val currentDirectory = File(directoryProperties.producerDir)
        setIdForEveryFileInDirectory(currentDirectory)

        val watchChannel = currentDirectory.asWatchChannel(
            mode = KWatchChannel.Mode.Recursive,
            tag = ManagementFactory.getRuntimeMXBean().name
        )

// TODO сделать остановку по событию

        val job = launch {
            try {
                watchChannel.consumeEach { event: KWatchEvent ->
                    logger.info { "Got event: $event" }
//                    processEventForLocalConsumer(event)
                    syncDispatcher.addEvent(event)
                }
                logger.info { "[SyncService] Service has finished monitoring" }
            } finally {
                watchChannel.close()
                println("Cancelled")
            }
        }
        job.join()

        logger.info { "Finishing" }
    }

    private fun processEventForLocalConsumer(event: KWatchEvent) {
        val consumersDir = directoryProperties.consumerDirs.map { File(it) }

        when (event.state) {
            KWatchEvent.State.Initialized -> {
                consumersDir.forEach {
                    it.deleteRecursively()
                    event.file.copyRecursively(it, true)
                }
            }
            KWatchEvent.State.Created, KWatchEvent.State.Modified -> {
                val relative = event.file.relativeTo(File(directoryProperties.producerDir))

                consumersDir.forEach {
                    val resolvedFile = it.resolve(relative)
                    event.file.copyRecursively(resolvedFile, true)
                }
            }
            KWatchEvent.State.Deleted -> {
                val relative = event.file.relativeTo(File(directoryProperties.producerDir))
                consumersDir.forEach {
                    val resolvedFile = it.resolve(relative)
                    resolvedFile.deleteRecursively()
                }
            }
        }
    }

    private fun setIdForEveryFileInDirectory(file: File) {
        logger.info(file.absolutePath)
        Files.walk(file.toPath())
            .forEach { it.toFile().setId()}
    }
}