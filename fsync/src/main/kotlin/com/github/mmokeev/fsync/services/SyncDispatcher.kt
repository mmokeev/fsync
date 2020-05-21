package com.github.mmokeev.fsync.services

import com.github.mmokeev.fsync.utils.*
import mu.*
import org.springframework.boot.context.event.*
import org.springframework.context.event.*
import org.springframework.stereotype.*
import java.io.*
import java.lang.IllegalStateException
import java.util.concurrent.*

private val logger = KotlinLogging.logger {}

@Service
class SyncDispatcher {

    private val blockingDeque = LinkedBlockingDeque<KWatchEvent>()

    @EventListener(ApplicationReadyEvent::class)
    fun initConsumers() {
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute(EventConsumer(blockingDeque))
        executorService.shutdown()
    }

    fun addEvent(event: KWatchEvent) {
        blockingDeque.addLast(event)
    }

    class EventConsumer(
        private val deque: BlockingDeque<KWatchEvent>,
        private val waitTimeInSeconds: Long = 1
    ) : Runnable {

        override fun run() {
            while (true) {
                val firstEvent = deque.takeFirst()
                when (firstEvent.state) {
//                    KWatchEvent.State.Initialized -> setIdForEveryFileInDirectory(firstEvent.file)
                    KWatchEvent.State.Created -> {
                        var secondEvent = deque.pollFirst(waitTimeInSeconds, TimeUnit.SECONDS)!!

                        if (secondEvent.state == KWatchEvent.State.Modified) {
                            while (true) {
                                if (secondEvent.file.isDirectory) {
                                    processCreated(firstEvent.file)
                                    break
                                } else if (secondEvent.file.absolutePath == firstEvent.file.absolutePath) {
                                    secondEvent = deque.pollFirst(waitTimeInSeconds, TimeUnit.SECONDS)!!
                                } else {
                                    logger.error("[EventConsumer] wrong events: \n $firstEvent \n $secondEvent")
                                    throw IllegalStateException()
                                }
                            }
                        }
                    }
                    KWatchEvent.State.Modified -> processFileModified(firstEvent.file)
                    KWatchEvent.State.Deleted -> {
                        val secondEvent = deque.pollFirst(waitTimeInSeconds, TimeUnit.SECONDS)
                        if (secondEvent != null) {

                            val secondFile = secondEvent.file

                            if (secondEvent.state == KWatchEvent.State.Modified && secondFile.isDirectory) {
                                processFileDeleted(secondFile)
                            } else if (
                                secondEvent.state == KWatchEvent.State.Created
                                && secondFile.isFile
                                && (secondFile.getId() != null)
                            ) {
                                val thirdEvent = deque.pollFirst(waitTimeInSeconds, TimeUnit.SECONDS)
                                if (thirdEvent != null && thirdEvent.file.isDirectory && thirdEvent.state == KWatchEvent.State.Modified) {
                                    // TODO смотреть 4-й евент и, если там папка родитель исходного места, то ее тоже скипать
                                    // такое можно произойти, когда из f/p.txt переносим в f/q/p.txt. Будет modified и q, и f
                                    if (firstEvent.file.parentFile.absolutePath == secondEvent.file.parentFile.absolutePath) {
                                        processRename(secondFile)
                                    } else {
                                        val fourthEvent = deque.pollFirst(waitTimeInSeconds, TimeUnit.SECONDS)
                                        processFileMoved(secondFile, thirdEvent.file.absolutePath, fourthEvent.file.absolutePath)
                                    }
                                }
                            }
                        }
                    }
                    KWatchEvent.State.Initialized -> true
                }

            }
        }

        private fun processCreatedOrModified(file: File) {
            println("CreatedOrModified: ${file.absolutePath}")
        }

        private fun processCreated(file: File) {
            file.setId()
            println("Created: ${file.absolutePath}")
//            processCreatedOrModified(file)
        }

        private fun processFileModified(file: File) {
            println("Modified: ${file.absolutePath}")
//            processCreatedOrModified(file)
        }

        private fun processFileDeleted(file: File) {
            println("Deleted: ${file.absolutePath}")
        }

        private fun processFileMoved(file: File, from: String, to: String) {
            println("Moved: ${file.absolutePath} from $from, to $to")

        }

        private fun processRename(file: File) {
            println("Renamed: ${file.absolutePath}")
        }



    }
}