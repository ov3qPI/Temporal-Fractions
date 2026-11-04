#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SCRIPT_ID='TemporalFractions_publish_v20261104T220019Z'
ZIP="${1:?Supply the Temporal-Fractions repository ZIP path}"
[[ -f "$ZIP" ]] || { printf 'ZIP missing: %s\n' "$ZIP" >&2;exit 1; }
for x in git unzip;do command -v "$x" >/dev/null 2>&1 || { printf 'Missing %s; install using pkg install git unzip\n' "$x" >&2;exit 1; };done
if ! command -v gh >/dev/null 2>&1;then pkg install -y gh;fi
command -v gh >/dev/null 2>&1 || { printf 'GitHub CLI unavailable\n' >&2;exit 1; }
if ! gh auth status >/dev/null 2>&1;then printf 'Authorize GitHub CLI for ov3qPI\n';gh auth login --hostname github.com --git-protocol https --web;fi
LOGIN="$(gh api user --jq .login)"
[[ "$LOGIN" == 'ov3qPI' ]] || { printf 'Connected GitHub account is %s; expected ov3qPI\n' "$LOGIN" >&2;exit 1; }
DEST="$HOME/projects/Temporal-Fractions"
if [[ ! -e "$DEST" ]];then TMP="$(mktemp -d "$HOME/.temporalfractions_publish.XXXXXXXX")";trap 'rm -rf -- "${TMP:-}"' EXIT;unzip -q "$ZIP" -d "$TMP";[[ -f "$TMP/Temporal-Fractions/build_v20261104T215458Z.sh" ]] || { printf 'Missing expected source tree\n' >&2;exit 1; };mkdir -p "$(dirname "$DEST")";mv "$TMP/Temporal-Fractions" "$DEST";else [[ -f "$DEST/build_v20261104T215458Z.sh" ]] || { printf 'Destination exists but is not Temporal Fractions: %s\n' "$DEST" >&2;exit 1; };fi
cd "$DEST"
if [[ ! -d .git ]];then git init -b main;fi
if [[ -z "$(git config user.name || true)" ]];then git config user.name "$LOGIN";fi
if [[ -z "$(git config user.email || true)" ]];then ID="$(gh api user --jq .id)";git config user.email "${ID}+${LOGIN}@users.noreply.github.com";fi
git add --all
if ! git diff --cached --quiet;then git commit -m 'Initial Temporal Fractions source release v20261104T215458Z';fi
if gh repo view ov3qPI/Temporal-Fractions >/dev/null 2>&1;then if ! git remote get-url origin >/dev/null 2>&1;then git remote add origin https://github.com/ov3qPI/Temporal-Fractions.git;fi;git push -u origin main;else gh repo create ov3qPI/Temporal-Fractions --public --description 'Astronomical-scale fraction countdown timers for Android, built with Kotlin and Termux' --source=. --remote=origin --push;fi
printf 'Published: https://github.com/ov3qPI/Temporal-Fractions\nLocal checkout: %s\n' "$DEST"
