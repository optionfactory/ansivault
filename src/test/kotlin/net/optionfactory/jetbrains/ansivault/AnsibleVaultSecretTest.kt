package net.optionfactory.jetbrains.ansivault

import org.junit.Assert
import org.junit.Test

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
        var password = "secret"
        val got = AnsibleVaultSecret(password).decrypt(vaulted)
        Assert.assertEquals("TEST", got)
    }

    @Test
    fun `can encrypt and decrypt a message`() {
        var clearText = "Lorem ipsum dolor sit amet"
        var password = "secret"
        val instance = AnsibleVaultSecret(password)
        val encrypt = instance.encrypt(text = clearText)
        println("Encrypt '$encrypt'")
        val decrypted = instance.decrypt(encrypt)
        Assert.assertEquals(clearText, decrypted)
    }
}
