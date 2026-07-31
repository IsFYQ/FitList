package com.example.healthcheckin.util

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID

object UuidV7 {

    private val random = SecureRandom()

    fun generate(): String {
        val timestamp = System.currentTimeMillis()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)

        val timestampBytes = ByteBuffer.allocate(8).putLong(timestamp).array()
        bytes[0] = timestampBytes[0]
        bytes[1] = timestampBytes[1]
        bytes[2] = timestampBytes[2]
        bytes[3] = timestampBytes[3]
        bytes[4] = timestampBytes[4]
        bytes[5] = timestampBytes[5]
        bytes[6] = timestampBytes[6]

        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        val buffer = ByteBuffer.wrap(bytes)
        val mostSigBits = buffer.getLong(0)
        val leastSigBits = buffer.getLong(8)
        return UUID(mostSigBits, leastSigBits).toString()
    }

    fun generateDeviceId(): String = generate()
}
