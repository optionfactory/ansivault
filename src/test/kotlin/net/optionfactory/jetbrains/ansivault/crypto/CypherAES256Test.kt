package net.optionfactory.jetbrains.ansivault.crypto

import net.optionfactory.jetbrains.ansivault.crypto.data.VaultContent
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherAES256
import net.optionfactory.jetbrains.ansivault.crypto.decoders.WrongPasswordException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CypherAES256Test {

    @Test
    fun `pads to the block size, a full block when already aligned`() {
        assertEquals(16, CypherAES256.pad(ByteArray(0)).size)
        assertEquals(16, CypherAES256.pad(ByteArray(15)).size)
        assertEquals(32, CypherAES256.pad(ByteArray(16)).size)
        assertArrayEquals(ByteArray(3) { 1 }, CypherAES256.unpad(CypherAES256.pad(ByteArray(3) { 1 })))
    }

    @Test(expected = IOException::class)
    fun `rejects a zero padding`() {
        CypherAES256.unpad(byteArrayOf(1, 2, 0))
    }

    @Test(expected = IOException::class)
    fun `rejects a padding longer than the block`() {
        CypherAES256.unpad(ByteArray(32) { 17 })
    }

    @Test(expected = IOException::class)
    fun `rejects inconsistent padding bytes`() {
        CypherAES256.unpad(byteArrayOf(9, 9, 3, 1, 3))
    }

    @Test
    fun `round trips empty and unicode text`() {
        listOf("", "àèìòù € 日本語 🔐", "a".repeat(1000)).forEach { text ->
            val encrypted = VaultHandler.encrypt(text.toByteArray(Charsets.UTF_8), "pässword")
            assertEquals(text, String(VaultHandler.decrypt(encrypted, "pässword"), Charsets.UTF_8))
        }
    }

    @Test
    fun `writes lower case hex lines of 80 chars like ansible-vault`() {
        val lines = String(VaultHandler.encrypt("x".toByteArray(), "secret"), Charsets.UTF_8).lines()
        assertEquals("\$ANSIBLE_VAULT;1.1;AES256", lines.first())
        lines.drop(1).forEach { line ->
            assertTrue(line, line.matches(Regex("[0-9a-f]{1,80}")))
        }
    }

    @Test(expected = WrongPasswordException::class)
    fun `wrong password fails the HMAC check`() {
        VaultHandler.decrypt(VaultHandler.encrypt("x".toByteArray(), "secret"), "other")
    }

    @Test
    fun `vault content survives a round trip`() {
        val content = VaultContent(byteArrayOf(1, 2), byteArrayOf(3, 4), byteArrayOf(5, 6, 7))
        val parsed = VaultContent(String(content.toByteArray()).let { net.optionfactory.jetbrains.ansivault.crypto.data.HexMarshaller.decode(it) })
        assertArrayEquals(content.salt, parsed.salt)
        assertArrayEquals(content.hmac, parsed.hmac)
        assertArrayEquals(content.data, parsed.data)
    }

    @Test
    fun `malformed vault content is an IOException`() {
        listOf("", "abcd", "abcd\n", "abcd\n\n00", "zz\n00\n00").forEach { body ->
            try {
                VaultContent(body.toByteArray())
                throw AssertionError("'$body' should be rejected")
            } catch (e: IOException) {
                assertFalse(e is WrongPasswordException)
            }
        }
    }

    @Test
    fun `parses vault headers`() {
        VaultInfo("\$ANSIBLE_VAULT;1.1;AES256").let {
            assertTrue(it.isEncryptedVault)
            assertTrue(it.isValidVault())
            assertEquals(null, it.vaultId)
        }
        VaultInfo("  \$ANSIBLE_VAULT;1.2;AES256;prod  \n0011").let {
            assertTrue(it.isValidVault())
            assertEquals("prod", it.vaultId)
        }
        assertFalse(VaultInfo("\$ANSIBLE_VAULT;1.3;AES256").isValidVault())
        assertFalse(VaultInfo("\$ANSIBLE_VAULT;1.1;AES256;id").isValidVault())
        assertFalse(VaultInfo("\$ANSIBLE_VAULT;1.1").isEncryptedVault)
        assertFalse(VaultInfo("password: \$ANSIBLE_VAULT;1.1;AES256").isEncryptedVault)
        assertFalse(VaultInfo("").isEncryptedVault)
    }

    @Test(expected = IOException::class)
    fun `unsupported cypher is an IOException`() {
        VaultHandler.decrypt("\$ANSIBLE_VAULT;1.1;AES\n3030".toByteArray(), "secret")
    }
}
