#!/usr/bin/env bash
# Rebuilds every vault of the playground with ansible-vault (git checkout playground/ also restores them).
# Needs ansible-core; asks nothing: the prod password is passed through a temporary file.
set -euo pipefail
cd "$(dirname "$0")"
# do not read ./ansible.cfg (it would prompt for prod): an empty config instead
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
touch "$TMP/empty.cfg"; export ANSIBLE_CONFIG="$TMP/empty.cfg"
PROD="$TMP/prod.txt"
printf 'prod-password\n' > "$PROD"
DEV_ID="dev@vault-pass/dev.txt"
PROD_ID="prod@$PROD"
DEFAULT_ID="default@vault-pass/dev.txt"   # a 1.1 vault without vault id, encrypted with the dev password

inline() { # inline <vault id source> <encrypt id> <name> <value> [indent]
  ansible-vault encrypt_string --vault-id "$1" --encrypt-vault-id "$2" --name "$3" "$4" 2>/dev/null | sed "s/^/${5:-}/"
}

# --- group_vars/all/vars.yml: properties, some in clear (to encrypt with the caret) and some inline vaults
{
cat <<'YAML'
# Properties file: plain values to encrypt with the caret, and inline vaults to open with the caret or the gutter icon.

app:
  name: playground                      # not a secret: leave it in clear
  # --- to encrypt: put the caret on the line and press Ctrl+Alt+Shift+A
  db_password: s3cret-db-password
  quoted_double: "pa ss#word"           # double quoted, '#' inside the quotes, trailing comment
  quoted_single: 'it''s a secret'
  users:
    - name: alice
      token: alice-token-123          # list item: the vault must be indented under 'token'
YAML
echo "  # --- inline vaults: dev (1.2, password from file)"
inline "$DEV_ID" dev api_key "dev-api-key-value" "  "
echo "  # --- prod (1.2, password asked once and saved in the IDE credential store)"
inline "$PROD_ID" prod prod_api_key "prod-api-key-value" "  "
echo "  # --- 1.1 vault without vault id: decrypted by trying every password (dev works)"
inline "$DEFAULT_ID" default legacy_token "legacy-token-value" "  "
echo "  # --- block scalar with strip chomping indicator (!vault |-): also recognised"
inline "$DEV_ID" dev strip_chomping "chomped-value" "  " | sed 's/!vault |$/!vault |-/'
echo "nested:"
echo "  deeply:"
echo "    # --- the vault editor tab is named after the key path: vars.yml: nested.deeply.hidden"
inline "$DEV_ID" dev hidden "multi
line
secret" "    "
} > group_vars/all/vars.yml

# --- group_vars/all/vault.yml: a whole file vault (dev), opened with YAML highlighting
CLEAR="$TMP/clear.yml"
cat > "$CLEAR" <<'YAML'
# Whole file vault: decrypted it is YAML, edited with YAML highlighting
vault_db_root_password: root-password
vault_users:
  - name: alice
    password: alice-password
  - name: bob
    password: bob-password
YAML
cp "$CLEAR" group_vars/all/vault.yml
ansible-vault encrypt --vault-id "$DEV_ID" --encrypt-vault-id dev group_vars/all/vault.yml >/dev/null

# --- group_vars/all/vault_prod.yml: a whole file vault with the prod vault id
printf '# Whole file vault with the prod vault id\nvault_prod_certificate_key: prod-certificate-key\n' > group_vars/all/vault_prod.yml
ansible-vault encrypt --vault-id "$PROD_ID" --encrypt-vault-id prod group_vars/all/vault_prod.yml >/dev/null

# --- broken/: vaults that must fail with a notification (not loaded by ansible: outside group_vars)
{
echo "# Vaults that must fail with an error notification (outside group_vars: ansible does not load them)."
echo
echo "# encrypted with a password that is neither dev nor prod: 'wrong password' (HMAC mismatch)"
printf 'not-a-configured-password\n' > "$TMP/other.txt"
inline "dev@$TMP/other.txt" dev wrong_password "unreachable"
cat <<'YAML'
# unsupported cypher
unsupported_cypher: !vault |
  $ANSIBLE_VAULT;1.1;AES
  3030
# not hexadecimal
not_hex: !vault |
  $ANSIBLE_VAULT;1.1;AES256
  zz
YAML
} > broken/broken.yml
echo "vaults regenerated"
