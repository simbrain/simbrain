package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class FileUtilsChecksumTest {

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun `verify checksum accepts a matching file and rejects a corrupted one`() {
        val file = File.createTempFile("simbrain-checksum", ".bin")
        try {
            file.writeText("payload")
            val sha = sha256(file)
            assertTrue(verifyChecksum(file, sha))
            file.writeText("payload-corrupted")
            assertFalse(verifyChecksum(file, sha), "a changed file must fail its recorded checksum")
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a cache hit with a checksum verifies once and writes a marker`() {
        val sub = "test-cache-${UUID.randomUUID()}"
        val dir = File(getSystemCacheDirectory(), sub).apply { mkdirs() }
        try {
            val target = File(dir, "data.bin").apply { writeText("cached payload") }
            val sha = sha256(target)

            val fetched = fetchFileWithCache("http://invalid.invalid/data.bin", sub, sha)
            assertEquals(target, fetched, "a verified cached file is served without a download")
            val marker = File(dir, "data.bin.verified")
            assertTrue(marker.exists(), "first verification records a marker")
            assertEquals("$sha:${target.length()}", marker.readText().trim())

            // A second hit trusts the marker; served even though hashing is skipped.
            assertEquals(target, fetchFileWithCache("http://invalid.invalid/data.bin", sub, sha))
        } finally {
            dir.deleteRecursively()
        }
    }
}
