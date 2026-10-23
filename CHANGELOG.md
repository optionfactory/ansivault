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

## [Unreleased]
### In development