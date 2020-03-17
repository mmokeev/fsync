package com.github.mmokeev.fsync

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FsyncApplication

fun main(args: Array<String>) {
	runApplication<FsyncApplication>(*args)
}
