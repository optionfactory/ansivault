# AnsiVault playground

A small ansible project to try every feature of the plugin by hand. Open this `playground` directory as a project
in an IDE with the plugin installed (or run `./gradlew runIde` and open it from there).

Passwords, **for testing only**:

| Vault id | Password | Source |
|---|---|---|
| `dev` | `dev-password` | file `vault-pass/dev.txt` |
| `prod` | `prod-password` | `prompt`: asked by the plugin, then saved in the IDE credential store |

| File | Content |
|---|---|
| `ansible.cfg` | `vault_identity_list = dev@vault-pass/dev.txt, prod@prompt`, `vault_encrypt_identity = dev` |
| `group_vars/all/vars.yml` | properties: plain values to encrypt, inline vaults (`dev`, `prod`, a 1.1 vault without vault id, `!vault \|-`, a nested key) |
| `group_vars/all/vault.yml` | whole file vault (`dev`) |
| `group_vars/all/vault_prod.yml` | whole file vault (`prod`) |
| `broken/broken.yml` | vaults that must fail: wrong password, unsupported cipher, not hexadecimal |
| `site.yml` | playbook printing every decrypted value, to check that ansible still reads the vaults |
| `regenerate-vaults.sh` | rebuilds all the vaults with `ansible-vault` |

To start again from a clean state: `git checkout -- playground` (or `./regenerate-vaults.sh`), then
<kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd> > **Forget Saved Passwords**.

## Checklist

The shortcut is <kbd>Ctrl+Alt+Shift+A</kbd> (<kbd>⌘⌥⇧A</kbd> on macOS); the same action is in the editor context menu and in <kbd>Tools</kbd>.

### Settings: where the passwords come from

- [ ] <kbd>Settings</kbd> > <kbd>Tools</kbd> > <kbd>AnsiVault Configuration</kbd>: the tree shows `Project: ansible.cfg` found and used,
  `User`/`System` not found, `vault_identity_list` and `vault_encrypt_identity` set, `dev` from `vault-pass/dev.txt`,
  `prod` "asked when needed", encryption with `dev` from `vault_encrypt_identity`.
- [ ] Rename `vault-pass/dev.txt`, press **Refresh**: `dev` shows a warning "not found"; rename it back.
- [ ] After typing the prod password (below), `prod` shows "saved in the IDE credential store"; select it and
  **Forget Password of Selected Vault Id**: it goes back to "asked when needed".
- [ ] Type `prod` in "Vault id used to encrypt", **Apply**: the Encryption node shows `prod` from the project setting. Clear it again.

### Encrypt with the caret (`group_vars/all/vars.yml`)

- [ ] Caret on `db_password: s3cret-db-password`, shortcut: the value becomes `!vault |` with a `$ANSIBLE_VAULT;1.2;AES256;dev` block
  indented under the key; no password is asked (dev comes from the file, prod is not needed to encrypt).
- [ ] `quoted_double: "pa ss#word"  # ...`: the vault holds `pa ss#word` (no quotes) and the comment moves after `!vault | # ...`.
- [ ] `quoted_single: 'it''s a secret'`: the vault holds `it's a secret`.
- [ ] `token: alice-token-123` in the list item: the vault is indented under `token`.
- [ ] Caret on `name: playground` and **Undo** (<kbd>Ctrl+Z</kbd>) after encrypting: one undo restores the line.
- [ ] Caret on an empty or comment line: a hint says what the action needs, nothing changes.

### Encrypt and decrypt a selection

- [ ] Select a plain value and run the action: it becomes an inline vault indented at the selection column.
- [ ] Select an inline vault including `!vault |` (or only the `$ANSIBLE_VAULT` block) and run the action: it becomes the clear value.
- [ ] Create a new file `secret.yml` with some YAML, select all and run the action: the whole file becomes a vault (no `!vault |`).

### Vault editor

- [ ] Click the gutter icon on `api_key` (or caret on it + shortcut): an editor opens on the right, tab `[AnsiVault] vars.yml: app.api_key`,
  text `dev-api-key-value`; the prod password is **not** asked (only the vault's own vault id is needed).
- [ ] Change the text and **close the tab without saving**: the vault in `vars.yml` is re-encrypted (the hex changes, still `;dev`).
- [ ] Open it again, change the text and save (<kbd>Ctrl+S</kbd>): re-encrypted at once.
- [ ] Open it and close it without changes: `vars.yml` is untouched.
- [ ] `nested.deeply.hidden`: the tab shows the key path, the text has three lines.
- [ ] `strip_chomping` (`!vault |-`): opens like the others.
- [ ] `legacy_token` (`$ANSIBLE_VAULT;1.1`, no vault id): every password is tried, so prod may be asked; it opens with the dev password
  and after editing it stays a 1.1 vault.
- [ ] `group_vars/all/vault.yml`: the whole file vault opens with **YAML highlighting**.
- [ ] Change "Where to open the vault editor" in the settings (left, top, bottom, new tab) and open a vault again.
- [ ] While a vault editor is open, delete the vault in `vars.yml`, then edit and save the vault editor: an error says the changes cannot be written back.

### prod: asked password and wrong password

- [ ] Open `prod_api_key` (or `vault_prod.yml`): a dialog asks the `prod` password; type `prod-password`: it opens, and the next time it is not asked.
- [ ] **Forget Saved Passwords**, open `prod_api_key` again and type a wrong password: a dialog says the saved password of `prod`
  is wrong and offers to type it again; answer yes and type `prod-password`: it opens. Answer no instead: nothing happens.

### Errors (`broken/broken.yml`)

- [ ] `wrong_password` (vault id `dev`, encrypted with another password): the dev password from the file fails, then prod is tried
  (asked if not saved); then an error **notification** (not a dialog) "No vault password could decrypt vault id 'dev'", with a
  **Vault password settings** link that opens the settings. No offer to forget passwords: the dev one comes from a file.
- [ ] `unsupported_cypher`: notification "Unsupported vault cypher 'AES'".
- [ ] `not_hex`: notification about malformed data.

### Configuration variants

- [ ] Uncomment `vault_id_match = true` in `ansible.cfg`: the settings tree says a vault is decrypted only with its own vault id;
  `legacy_token` (no vault id = `default`) no longer opens. Comment it again.
- [ ] Start the IDE with `ANSIBLE_VAULT_ENCRYPT_IDENTITY=prod`: the settings tree shows it coming from the environment variable,
  and new vaults are written with `;prod`.

### Ansible can still read everything

- [ ] After the tests, from this directory: `ansible-playbook site.yml` (type `prod-password` when asked): every value is printed, no errors.
