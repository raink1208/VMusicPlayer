package com.github.rain1208.vmusicplayerserver.utils

import de.huxhorn.sulky.ulid.ULID
import org.springframework.stereotype.Component

@Component
class ULIDGenerator {
    private val ulid = ULID()

    fun generate(): String {
        return ulid.nextULID()
    }
}