package net.optionfactory.jetbrains.ansivault

import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@Ignore
class AnsivaultIT {

    @Test
    fun `can encrypt with Ansivault and decrypt with ansible-vault`() {
        val tempDir = Files.createTempDirectory("ansivault")

        val clearText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
        val password = "secret"

        val instance = AnsibleVaultSecret(password)
        val encrypted = instance.encrypt(text = clearText)
        val encryptedFile = File.createTempFile("vault", ".yml", tempDir.toFile())
        encryptedFile.writeText(encrypted)
        val passFile = File.createTempFile("pass", ".txt", tempDir.toFile())
        passFile.writeText(password)

        println(encryptedFile)

        val command = listOf(
            "ansible-vault",
            "decrypt",
            "--vault-password-file",
            passFile.absolutePath,
            encryptedFile.absolutePath
        )

        val process: Process = ProcessBuilder()
            .command(command)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor(10, TimeUnit.SECONDS)

        val cliDecrypted = encryptedFile.readText()

        assertEquals(0, process.exitValue())
        assertEquals(clearText, cliDecrypted)
    }

    @Test
    fun `can encrypted with ansible-vault and decrypt with Ansivault`() {
        val tempDir = Files.createTempDirectory("ansivaultenc")

        val clearText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
        val password = "secret"

        val passFile = File.createTempFile("pass", ".txt", tempDir.toFile())
        passFile.writeText(password)
        val clearTextFile: File = File.createTempFile("vault", ".yml", tempDir.toFile())
        clearTextFile.writeText(clearText)

        val command = listOf(
            "ansible-vault",
            "encrypt",
            "--vault-password-file",
            passFile.absolutePath,
            clearTextFile.absolutePath
        )

        val process: Process = ProcessBuilder()
            .command(command)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor(10, TimeUnit.SECONDS)

        assertEquals(0, process.exitValue())

        val instance = AnsibleVaultSecret(password)
        val decrypted = instance.decrypt(clearTextFile.readText())

        assertEquals(clearText, decrypted)
    }
}
