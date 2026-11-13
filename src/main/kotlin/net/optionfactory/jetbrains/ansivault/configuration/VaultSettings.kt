package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

enum class VaultEditorPlacement {
    SPLIT_RIGHT,
    SPLIT_LEFT,
    SPLIT_TOP,
    SPLIT_BOTTOM,
    NEW_TAB,
}

@Service(Service.Level.PROJECT)
@State(name = "AnsiVaultSettings", storages = [Storage("ansivault.xml")])
class VaultSettings : PersistentStateComponent<VaultSettings.State> {

    class State {
        var editorPlacement: VaultEditorPlacement = VaultEditorPlacement.SPLIT_RIGHT
        /** Vault id used to encrypt when several are configured; blank = ansible's vault_encrypt_identity. */
        var encryptVaultId: String = ""
        /** Vault ids with a password in the PasswordSafe, which cannot list its own keys. */
        var savedVaultIds: MutableSet<String> = mutableSetOf()
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): VaultSettings = project.service()
    }
}
