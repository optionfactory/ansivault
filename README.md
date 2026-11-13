# ansivault

![Build](https://github.com/optionfactory/ansivault/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26457-ansivault.svg)](https://plugins.jetbrains.com/plugin/26457-ansivault)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26457-ansivault.svg)](https://plugins.jetbrains.com/plugin/26457-ansivault)


<!-- Plugin description -->
Easy Vault and Unvault ansible secret file and YML properties, with support for multiple passwords through
[vault ids](https://docs.ansible.com/ansible/latest/vault_guide/vault_managing_passwords.html#managing-multiple-passwords-with-vault-ids).

The ansible configuration is looked up in this order:
1. The file pointed by the environment variable `ANSIBLE_CONFIG`
2. The first `ansible.cfg` found in the project path. This is made to support a project structure like `<root>/infrastructure/ansible/ansible.cfg`
3. `$HOME/.ansible.cfg`
4. `/etc/ansible/ansible.cfg`

These `[defaults]` settings are supported, each one overridable by its `ANSIBLE_*` environment variable:
- `vault_password_file` (`ANSIBLE_VAULT_PASSWORD_FILE`): the vault password file
- `vault_identity_list` (`ANSIBLE_VAULT_IDENTITY_LIST`): multiple passwords as `label@path` or `label@prompt`, e.g. `dev@~/.vault_dev, prod@prompt`
- `vault_identity` (`ANSIBLE_VAULT_IDENTITY`): the vault id of `vault_password_file` (default `default`)
- `vault_encrypt_identity` (`ANSIBLE_VAULT_ENCRYPT_IDENTITY`): the vault id used to encrypt when several are configured
- `vault_id_match` (`ANSIBLE_VAULT_ID_MATCH`): decrypt a vault only with the password of its own vault id

Relative password file paths are resolved against the directory of the `ansible.cfg` defining them.

Passwords sourced from `prompt`, or asked when nothing is configured, are requested in a single dialog and saved
in the secure credential store; they can be forgotten in <kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd>.

Vaults encrypted with the `default` vault id are written as `$ANSIBLE_VAULT;1.1`, any other vault id as `$ANSIBLE_VAULT;1.2;AES256;<vault id>`.
The vault id used to encrypt can be chosen per project in Settings, overriding `vault_encrypt_identity`;
editing an existing vault keeps its vault id, like `ansible-vault edit`.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ansivault"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26457-ansivault) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/26457-ansivault/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/optionfactory/ansivault/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
