package com.github.mmokeev.fsync

import com.github.mmokeev.fsync.setup.properties.*
import org.springframework.boot.*
import org.springframework.boot.autoconfigure.*
import org.springframework.boot.context.properties.*

@SpringBootApplication
@EnableConfigurationProperties(DirectoryProperties::class)
class FsyncApplication

fun main(args: Array<String>) {

    runApplication<FsyncApplication>(*args)
}
