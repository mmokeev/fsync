package com.github.mmokeev.fsync

import com.github.mmokeev.fsync.setup.properties.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.*
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DirectoryProperties::class)
class FsyncApplication

fun main(args: Array<String>) {
	runApplication<FsyncApplication>(*args)
}
