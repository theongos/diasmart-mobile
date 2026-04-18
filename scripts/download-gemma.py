"""
Telecharge gemma3-1b-it-int4.task depuis HuggingFace.

Prerequis :
  1. Compte HuggingFace (gratuit) : https://huggingface.co/join
  2. Accepter la licence Gemma : https://huggingface.co/google/gemma-3-1b-it
  3. Token Read : https://huggingface.co/settings/tokens
  4. Placer le token dans la variable HF_TOKEN ci-dessous OU le saisir au prompt

Usage :
  python scripts/download-gemma.py
"""
import os
import sys
import shutil
from pathlib import Path

REPO_ID = "litert-community/Gemma3-1B-IT"
FILENAME = "gemma3-1b-it-int4.task"
DEST = Path(__file__).parent.parent / FILENAME  # racine du projet

# ─── Token (soit via env var, soit via prompt interactif) ──────────────────
token = os.environ.get("HF_TOKEN")
if not token:
    print("=" * 70)
    print("TOKEN HUGGINGFACE REQUIS")
    print("=" * 70)
    print("1. Va sur : https://huggingface.co/settings/tokens")
    print("2. Cree un token de type 'Read' (gratuit)")
    print("3. Accepte la licence Gemma sur : https://huggingface.co/google/gemma-3-1b-it")
    print("4. Colle le token ci-dessous (il commence par hf_...)")
    print("=" * 70)
    token = input("Token HF : ").strip()

if not token.startswith("hf_"):
    print("[ERR] Le token doit commencer par 'hf_'. Annulation.")
    sys.exit(1)

# ─── Telechargement ────────────────────────────────────────────────────────
from huggingface_hub import hf_hub_download, login

print(f"[..] Login HuggingFace...")
login(token=token, add_to_git_credential=False)

print(f"[..] Telechargement de {FILENAME} (~550 MB)...")
print(f"     Patience, cela peut prendre 5-20 min selon ta connexion.")

try:
    path = hf_hub_download(
        repo_id=REPO_ID,
        filename=FILENAME,
        local_dir=str(DEST.parent),
        local_dir_use_symlinks=False,
    )
except Exception as e:
    print(f"[ERR] Echec : {e}")
    if "403" in str(e) or "gated" in str(e).lower():
        print("     -> Tu n'as pas accepte la licence Gemma.")
        print("        Va sur https://huggingface.co/google/gemma-3-1b-it")
        print("        et clique sur 'Acknowledge license'.")
    sys.exit(1)

src = Path(path)
if src.resolve() != DEST.resolve():
    shutil.move(str(src), str(DEST))

size_mb = DEST.stat().st_size / 1024 / 1024
print(f"[OK] Fichier telecharge : {DEST}")
print(f"     Taille : {size_mb:.1f} MB")
print("")
print(">>> Prochaine etape : node scripts/upload-gemma.js (apres avoir active Storage)")
