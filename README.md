# ansivault

![Build](https://github.com/optionfactory/ansivault/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26457-ansivault.svg)](https://plugins.jetbrains.com/plugin/26457-ansivault)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26457-ansivault.svg)](https://plugins.jetbrains.com/plugin/26457-ansivault)


<!-- Plugin description -->
Easy Vault and Unvault ansible secret file and YML properties. This plugin search for vault password in that order:
1. System property `ANSIBLE_CONFIG`
2. `ansible.cfg` file containing the path of vault password file (`[default]` -> `vault_password_file` property) in current project path. This is made to support a project structure like `<root>/infrastructure/ansible/ansible.cfg`
3. `$HOME/ansible.cfg`
4. `/etc/ansible/ansible.cfg`
5. Fallback with user prompt. In that case the password is saved on secure credential store, and can be deleted in Settings.

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
