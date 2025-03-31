package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.DataManager
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import java.util.concurrent.CompletableFuture


private const val subsystem = "AnsibleVaultSecret"
private const val keyPrefix = "password_"
private const val fakeUser = "vault-secret"

class CredentialManager {

    companion object {
        fun getCredential(): Credentials? {
            val credentialAttributes = CredentialAttributes(
                serviceName = generateServiceName(subsystem, keyPrefix + getCurrentProject()),
                userName = fakeUser,
            )
            return PasswordSafe.instance.get(credentialAttributes)
        }

        fun deleteCredential() {
            val credentialAttributes = CredentialAttributes(
                serviceName = generateServiceName(subsystem, keyPrefix + getCurrentProject()),
                userName = fakeUser,
            )
            PasswordSafe.instance.set(credentialAttributes, null)
        }

        fun setCredential(password: String) {
            val credentialAttributes = CredentialAttributes(
                serviceName = generateServiceName(subsystem, keyPrefix + getCurrentProject()),
                userName = fakeUser,
            )
            val credentials = Credentials(fakeUser, password)
            PasswordSafe.instance.set(credentialAttributes, credentials)
        }

        private fun getCurrentProject(): String {
            val asyncResult = CompletableFuture<DataContext>()
            DataManager.getInstance().dataContextFromFocusAsync.onSuccess { context: DataContext -> asyncResult.complete(context) }.onError { it: Throwable -> asyncResult.completeExceptionally(it) }
            return asyncResult.get().getData(CommonDataKeys.PROJECT)?.name.orEmpty()
        }
    }
}
