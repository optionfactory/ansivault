package net.optionfactory.jetbrains.ansivault

import org.junit.Assert
import org.junit.Test
import java.io.IOException

class AnsibleVaultSecretTest {
    @Test
    fun `can decode ansible vault secret`() {
        var vaulted = """
          ${'$'}ANSIBLE_VAULT;1.1;AES256
          37386131653161396135363330653566663637333564396465373437626431353765396364633732
          6534633165363866356262633839636431393439636339630a336165326231356338306234393339
          61326536393435343464393039383764353261313835636139313036346536313162396465376266
          3661393166313461300a303462623633623866636433346135373132626335393533333766386139
          3730
      """.trimIndent()
        val password = "secret"
        val got = AnsibleVaultSecret(password).decrypt(vaulted)
        Assert.assertEquals("TEST", got)
    }

    @Test
    fun `can encrypt and decrypt a message`() {
        val clearText = "Lorem ipsum dolor sit amet"
        val password = "secret"
        val instance = AnsibleVaultSecret(password)
        val encrypt = instance.encrypt(text = clearText)
        println("Encrypt '$encrypt'")
        val decrypted = instance.decrypt(encrypt)
        Assert.assertEquals(clearText, decrypted)
    }

    private val vaultWithId = """
          ${'$'}ANSIBLE_VAULT;1.2;AES256;dev
          64663434373165346264306531616263333263666238343161616135306166323065366530366632
          3065306230623363643133313630333933643638663639310a633932623034306661316438306263
          31323864373265646163373937333363303764633031613862663364323765623434353063353830
          3839323366666234640a643333343938303332656234356466323933373665646263353062656635
          3530
      """.trimIndent()

    @Test
    fun `can decode a vault with vault id using the single password`() {
        Assert.assertEquals("TEST", AnsibleVaultSecret("secret").decrypt(vaultWithId))
    }

    @Test
    fun `tries the matching vault id first then the others`() {
        val passwords = linkedMapOf("prod" to "wrong", "other" to "secret")
        Assert.assertEquals("TEST", AnsibleVaultSecret(passwords).decrypt(vaultWithId))
    }

    @Test(expected = IOException::class)
    fun `vault id match uses only the password of the vault id`() {
        AnsibleVaultSecret(mapOf("prod" to "secret"), vaultIdMatch = true).decrypt(vaultWithId)
    }

    @Test
    fun `vault id match decrypts with the matching id`() {
        val secret = AnsibleVaultSecret(mapOf("prod" to "wrong", "dev" to "secret"), vaultIdMatch = true)
        Assert.assertEquals("TEST", secret.decrypt(vaultWithId))
    }

    @Test
    fun `default vault id writes the 1_1 header`() {
        val encrypted = AnsibleVaultSecret("secret").encrypt("x")
        Assert.assertTrue(encrypted.startsWith("${'$'}ANSIBLE_VAULT;1.1;AES256\n"))
    }

    @Test
    fun `encrypts with the configured vault id`() {
        val secret = AnsibleVaultSecret(mapOf("dev" to "a", "prod" to "b"), encryptVaultId = "prod")
        val encrypted = secret.encrypt("Lorem ipsum")
        Assert.assertTrue(encrypted.startsWith("${'$'}ANSIBLE_VAULT;1.2;AES256;prod\n"))
        Assert.assertEquals("Lorem ipsum", AnsibleVaultSecret(mapOf("x" to "b")).decrypt(encrypted))
    }

    @Test(expected = IOException::class)
    fun `refuses to guess the vault id with multiple passwords`() {
        AnsibleVaultSecret(mapOf("dev" to "a", "prod" to "b")).encrypt("x")
    }

    @Test
    fun `editing keeps the original vault id`() {
        val (clear, editSecret) = AnsibleVaultSecret("secret").decryptForEdit(vaultWithId)
        val reEncrypted = editSecret.encrypt(clear + "!")
        Assert.assertTrue(reEncrypted.startsWith("${'$'}ANSIBLE_VAULT;1.2;AES256;dev\n"))
        Assert.assertEquals("TEST!", AnsibleVaultSecret("secret").decrypt(reEncrypted))
    }

    @Test
    fun `parses the vault identity list`() {
        Assert.assertEquals(
            listOf("dev" to "~/.dev", "prod" to "prompt", "team" to "plain.txt"),
            AnsibleVaultSecret.parseVaultIdentityList(" dev@~/.dev, prod@prompt ,plain.txt, bad;id@x,", "team"),
        )
    }
}
