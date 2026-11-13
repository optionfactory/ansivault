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
### In development