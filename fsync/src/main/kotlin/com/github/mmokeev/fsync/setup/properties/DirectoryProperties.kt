package com.github.mmokeev.fsync.setup.properties

import org.springframework.boot.context.properties.*

@ConstructorBinding
@ConfigurationProperties("fsync")
data class DirectoryProperties(
    val producerDir: String,
    val consumerDirs: List<String>
)