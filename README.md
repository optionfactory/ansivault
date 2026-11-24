# ansivault

![Build](https://github.com/optionfactory/ansivault/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26457-ansivault.svg)](https://plugins.jetbrains.com/plugin/26457-ansivault)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26457-ansivault.svg)](https://plugins.jetbrains.com/plugin/26457-ansivault)


<!-- Plugin description -->
Encrypt, decrypt and edit [Ansible Vault](https://docs.ansible.com/ansible/latest/vault_guide/index.html) secrets
right in the editor: whole vault files and inline `!vault |` YAML values, with the same configuration as `ansible-vault`,
including multiple passwords through
[vault ids](https://docs.ansible.com/ansible/latest/vault_guide/vault_managing_passwords.html#managing-multiple-passwords-with-vault-ids).

- **One action for everything**: <kbd>Ctrl+Alt+Shift+A</kbd> (<kbd>⌘⌥⇧A</kbd> on macOS), also in the editor context menu and in <kbd>Tools</kbd>
  - on a selection: encrypts it, or decrypts it when it is a vault;
  - on a `key: value` line: encrypts the value as an inline `!vault |`;
  - on a vault: opens it decrypted in an editor.
- **Edit vaults in place**: a gutter icon on every vault opens it decrypted, side by side;
  the text is encrypted back when saved or when the editor is closed, keeping the vault id (like `ansible-vault edit`).
- **Reads your ansible configuration**: `ansible.cfg` and `ANSIBLE_*` variables, password files, `prompt` passwords
  saved in the IDE credential store, `vault_identity_list`, `vault_encrypt_identity`, `vault_id_match`.
- **Shows where the passwords come from**: <kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd>
  lists the config files searched, the vault settings found and the password source of each vault id.
- **Compatible with ansible-vault**: vaults written by the plugin are read by ansible and vice versa (`$ANSIBLE_VAULT;1.1` and `1.2`, AES256).
<!-- Plugin description end -->

## Usage

### Encrypt and decrypt

**Ansible Vault Encrypt/Decrypt** works on the selection, or on the line of the caret when nothing is selected:

| Where | What it does |
|---|---|
| Selection of plain text at the start of the file | encrypts the whole file as a vault |
| Selection of plain text elsewhere | replaces it with an inline `!vault \|` vault, indented to the selection column |
| Selection of a vault (with or without its `!vault \|` tag) | replaces it with the decrypted text |
| Caret on a `key: value` line | encrypts the value as an inline vault; plain, `"double"` and `'single'` quoted values, trailing `# comments` are kept |
| Caret on a vault, or on its `key: !vault \|` line | opens the vault editor (see below) |

```yaml
# before: caret on the line, Ctrl+Alt+Shift+A
db_password: s3cret   # the db
# after
db_password: !vault | # the db
  $ANSIBLE_VAULT;1.1;AES256
  6231653639...
```

The shortcut can be changed in <kbd>Settings</kbd> > <kbd>Keymap</kbd> (search "Ansible Vault").

### Vault editor

Click the AnsiVault icon in the gutter of a vault (or run the action with the caret on it): the vault opens decrypted in an editor,
in the right half of the current one by default (<kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd>
to open it left, above, below or in a new tab).

- the tab is named after the file and the YAML key path, e.g. `[AnsiVault] vars.yml: db.credentials.password`;
- a whole file vault is edited with the type of the file (YAML highlighting), an inline vault as plain text;
- saving, or closing the editor, encrypts the text back into the original file if it changed, with the same vault id and the password that opened it;
- if the vault was deleted from the original file in the meantime, the changes cannot be written back and an error is shown.

### Configuration

The plugin reads the vault settings of ansible. The configuration files are searched in this order:

1. the file of the `ANSIBLE_CONFIG` environment variable;
2. the `ansible.cfg` in the project root, or else the shallowest one in the project (up to 5 levels deep, skipping hidden,
   `node_modules`, `build`, `target`, `out`, `dist`, `venv` directories), for layouts like `<root>/infrastructure/ansible/ansible.cfg`;
3. `~/.ansible.cfg`;
4. `/etc/ansible/ansible.cfg`.

These `[defaults]` settings are supported, each one overridden by its `ANSIBLE_*` environment variable:

| Setting | Environment variable | Meaning |
|---|---|---|
| `vault_password_file` | `ANSIBLE_VAULT_PASSWORD_FILE` | file with the vault password |
| `vault_identity_list` | `ANSIBLE_VAULT_IDENTITY_LIST` | several passwords as `label@path` or `label@prompt`, e.g. `dev@~/.vault_dev, prod@prompt` |
| `vault_identity` | `ANSIBLE_VAULT_IDENTITY` | vault id of `vault_password_file` and of unlabelled sources (default `default`) |
| `vault_encrypt_identity` | `ANSIBLE_VAULT_ENCRYPT_IDENTITY` | vault id used to encrypt when there are several |
| `vault_id_match` | `ANSIBLE_VAULT_ID_MATCH` | decrypt a vault only with the password of its own vault id |

Relative password file paths are resolved against the directory of the `ansible.cfg` that defines them, `~` against the home directory.

```ini
# ansible.cfg
[defaults]
vault_identity_list = dev@vault-pass/dev.txt, prod@prompt
vault_encrypt_identity = dev
```

**Passwords.** For each vault id the password is read from its file; `prompt` passwords (and the password asked when nothing
is configured) are requested once, in a single dialog, and saved in the IDE credential store. To decrypt, the password of the
vault's own vault id is tried first, then the others (only its own with `vault_id_match`); to encrypt, only the password of
the encrypt vault id is needed.

**Vault ids.** Vaults encrypted with the `default` vault id are written as `$ANSIBLE_VAULT;1.1;AES256`, any other vault id as
`$ANSIBLE_VAULT;1.2;AES256;<vault id>`. With several vault ids the one used to encrypt comes from the project setting
(<kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd>), else `vault_encrypt_identity`.

### Settings

<kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd>:

- **Forget Saved Passwords**: removes the passwords of the project saved in the IDE credential store; they are asked again when needed;
- **Vault id used to encrypt**: overrides `vault_encrypt_identity` for this project;
- **Where to open the vault editor**: right, left, upper or bottom half of the editor, or a new tab;
- **Where the vault passwords come from**: a tree with the configuration files in search order (found or not, which one is used),
  the vault settings and where each one is defined (file or environment variable), the password source of each vault id
  (file, missing file, saved or to be asked) and the vault id used to encrypt. Select a vault id with a saved password and
  **Forget Password of Selected Vault Id** to forget only that one. Problems are marked with a warning.

### Troubleshooting

- **"HMAC Digest doesn't match - possibly it's the wrong password"**: the password of that vault id is wrong. If it was saved by the
  plugin you are offered to forget it and type it again; otherwise check the password file shown in the settings tree.
- **"Multiple vault ids (...): choose the one to encrypt with"**: set `vault_encrypt_identity`, or the vault id in the settings.
- **Environment variables are ignored**: the IDE sees the environment it was started with. On macOS an IDE started from the Dock
  does not get the variables of your shell: start it from a terminal, or put the settings in `ansible.cfg`.
- **"Cannot access native keychain" on Linux**: the IDE credential store needs `libsecret` (e.g. `sudo apt-get install libsecret-1-0 gnome-keyring`);
  without it the passwords are kept only until the IDE is closed.
- **The settings tree is the place to start**: it shows exactly what the plugin reads and from where.

### Limitations

- Executable password files (scripts, `*-client`) are read as text, not executed.
- Ansible reads only the first configuration file found; the plugin looks for each setting in the next files too.
- Only the `AES256` cipher (the one used by every ansible version since 2.0) is supported.

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


## Compatibility

IntelliJ-based IDEs 2024.3 to 2026.3 (build 243 to 263.*), verified with the IntelliJ Plugin Verifier.

## Development

```bash
./gradlew check            # tests; the ansible-vault compatibility tests run when ansible-vault is installed
./gradlew buildPlugin      # build/distributions/ansivault-<version>.zip
./gradlew runIde           # an IDE with the plugin
./gradlew verifyPlugin     # IntelliJ Plugin Verifier on the recommended IDEs
./gradlew verifyPlugin -PverifyIde=IU:2026.3   # on one IDE only
```

The [`playground`](playground/README.md) directory is a small ansible project to try every feature of the plugin by hand,
with a checklist of what to test.

See the [changelog](CHANGELOG.md) for the changes of each version.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
