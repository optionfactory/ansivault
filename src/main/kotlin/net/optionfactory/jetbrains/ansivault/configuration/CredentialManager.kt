package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo.Companion.DEFAULT_VAULT_ID


private const val subsystem = "AnsibleVaultSecret"
private const val keyPrefix = "password_"
private const val fakeUser = "vault-secret"

class CredentialManager {

    companion object {
        fun getPassword(project: Project, vaultId: String): String? {
            return PasswordSafe.instance.get(attributes(project, vaultId))?.getPasswordAsString()
        }

        fun setPassword(project: Project, vaultId: String, password: String) {
            PasswordSafe.instance.set(attributes(project, vaultId), Credentials(fakeUser, password))
            VaultSettings.getInstance(project).state.savedVaultIds.add(vaultId)
        }

        fun hasPasswords(project: Project): Boolean {
            return knownVaultIds(project).any { getPassword(project, it) != null }
        }

        fun deletePasswords(project: Project) {
            knownVaultIds(project).forEach { PasswordSafe.instance.set(attributes(project, it), null) }
            VaultSettings.getInstance(project).state.savedVaultIds.clear()
        }

        private fun knownVaultIds(project: Project): Set<String> {
            return VaultSettings.getInstance(project).state.savedVaultIds + DEFAULT_VAULT_ID
        }

        private fun attributes(project: Project, vaultId: String): CredentialAttributes {
            // the default vault id keeps the pre vault-id key, so passwords saved by older versions still work
            val key = if (vaultId == DEFAULT_VAULT_ID) keyPrefix + project.name else "$keyPrefix${project.name}@$vaultId"
            return CredentialAttributes(serviceName = generateServiceName(subsystem, key), userName = fakeUser)
        }
    }
}
