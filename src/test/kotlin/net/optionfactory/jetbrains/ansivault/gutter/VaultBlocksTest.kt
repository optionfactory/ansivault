package net.optionfactory.jetbrains.ansivault.gutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultBlocksTest {
    private val yaml = """
        # comment
        db:
          - name: "main"
            credentials:
              # the password
              password: !vault |
                ${'$'}ANSIBLE_VAULT;1.1;AES256
                6162
                6364
              user: admin
        'api key': !vault |
          ${'$'}ANSIBLE_VAULT;1.2;AES256;prod
          6566
        plain: ${'$'}ANSIBLE_VAULT is a string
        """.trimIndent().lines()

    @Test
    fun `finds the vault headers`() {
        assertEquals(listOf(6, 11), VaultBlocks.headerLines(yaml))
    }

    @Test
    fun `reads the whole block up to the first non hex line`() {
        val block = VaultBlocks.at(yaml, 6)!!
        assertEquals(8, block.lastLine)
        assertEquals(8, block.indent)
        assertEquals("\$ANSIBLE_VAULT;1.1;AES256\n6162\n6364", block.vaultText)
        assertNull(VaultBlocks.at(yaml, 5))
    }

    @Test
    fun `builds the key path of a vault`() {
        assertEquals("db.name.credentials.password", VaultBlocks.keyPath(yaml, 6))
        assertEquals("api key", VaultBlocks.keyPath(yaml, 11))
    }
}
