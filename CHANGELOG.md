<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Ansivault Changelog

## [0.0.1]
### Added
- Initial release can look up vault password, and encrypt and decrypt any file
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

## [0.0.2]
### Fixed
- Error when decrypting a vault

## [0.0.3]
### Modified
- Bump compatibility minimum version
- Bump to JVM 21
### Fixed
- Deprecation warning

## [0.0.4]
### Modified
- Bump various lib versions
- compatibility with IDEA 2025.2

## [0.0.5]
### Fixed
- NPE reading file specified in configuration, but the vault-password file doesn't exists.

## [0.0.6]
### Skipped
- Release skipped accidentally

## [0.0.7]
### Added
- Bump various lib versions
- Support for IDEA 2023.1

## [0.0.8]
### Added
- Support for IDEA 2026.1

## [0.0.9]
### Added
- Bump various lib versions
- Support for IDEA 2026.2
### Fixed
- Avoid error when selected text is null

## [0.0.10]
### Added
- Gutter icon to edit encrypted vault blocks in a dedicated editor tab, with the decrypted text re-encrypted on close
- Configurable placement of the vault editor: right, left, upper or bottom half of the editor, or a new tab
- Vault editor tab named after the file and the key path of the property, e.g. `[AnsiVault] elaine.yaml: a.b.secret`
### Fixed
- NPE when the password dialog is cancelled

## [0.0.11]
### Changed
- Vault editor re-encrypts the block on save (Ctrl+S) instead of on tab close, and only when the decrypted text has changed
- Vault editor no longer uses internal IntelliJ APIs
### Fixed
- Deprecated `Document.addDocumentListener(DocumentListener)` usage in the gutter listener

## [0.0.12]
### Added
- Extend plugin compatibility through IntelliJ Platform 2026.3

## [0.0.13]
### Fixed
- Replace deprecated `FileEditorManager.openFile(VirtualFile, boolean)` usage

## [0.0.14]
### Added
- Vault ids (#18): `$ANSIBLE_VAULT;1.2` vaults, `vault_identity_list`, `vault_identity`, `vault_encrypt_identity`, `vault_id_match` (and their `ANSIBLE_*` env vars)
- Project setting to choose the vault id used to encrypt when several are configured
- One dialog asks the passwords of all vault ids sourced from `prompt`
### Changed
- Relative password file paths are resolved against the directory of the `ansible.cfg` defining them
- User config file is `~/.ansible.cfg` (was `~/ansible.cfg`), `ANSIBLE_CONFIG` is read from the environment as config path
- Debug logs no longer contain secrets at debug level (moved to trace)
- "Forget Saved Passwords" in Settings forgets the passwords of all the vault ids of the project
### Fixed
- Salts are generated with `SecureRandom`
- Selecting a 1.2 vault and running "Ansible Encrypt" encrypted it a second time
- Encrypt/decrypt errors are shown in a dialog instead of the IDE error reporter

## [Unreleased]
### Fixed
- Closing the vault editor tab re-encrypts the text if it changed, even without saving; an error is shown if the vault can no longer be written back
- "Ansible Encrypt" no longer asks the passwords when nothing is selected, and is disabled without a selection
- Indentation of an inline vault uses the real column of the selection (was the visual one, wrong with soft wraps and folding)
- `!vault |-`, `!vault |+` and trailing spaces are recognised when decrypting a selection
- Vault password files are trimmed, like ansible
- A malformed `ansible.cfg` is logged instead of breaking encryption
### Changed
- A wrong saved password can be forgotten and typed again right from the error dialog
- `ansible.cfg` lookup in the project is bounded and skips hidden and build/dependency directories
- Gutter icons are refreshed when typing pauses, only for file editors
- Vaults are written in lower case hex, like `ansible-vault`
- Constant time HMAC comparison and strict PKCS#7 padding check
- Key derivation uses the JDK PBKDF2: dropped the `de.rtner:PBKDF2` dependency
- Removed dead code (JCE policy check, stub `AES` cypher, unused stream APIs)
### Build
- The Kotlin plugin version comes from the version catalog (an inline version overrode it)
- `gradleVersion` aligned with the wrapper (9.9.1)
- `AnsivaultIT` runs whenever `ansible-vault` is installed; CI installs it
- `verifyPlugin` can target a single IDE with `-PverifyIde=<type>:<version>` (e.g. `-PverifyIde=IU:2026.3`), default still the recommended IDEs
### Verified
- Plugin Verifier: compatible, with no deprecated, experimental, internal or override-only API usages, with IC 2024.3.7.1, 2025.1.7.2, 2025.2.6.4 and IU 2025.3.6.2, 2026.1.5.1, 2026.2.5, 2026.3