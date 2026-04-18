# Scripts utilitaires DiaSmart

## download-gemma.py — Telecharger le modele Gemma 3 1B en local

Telecharge `gemma3-1b-it-int4.task` (529 Mo) depuis HuggingFace vers la racine
du projet. Utile pour :
- Publier le modele sur une nouvelle release GitHub
- Tester le chargement MediaPipe en local

### Prerequis

1. Compte HuggingFace (gratuit) : https://huggingface.co/join
2. Accepter la licence Gemma : https://huggingface.co/google/gemma-3-1b-it
3. Token Read : https://huggingface.co/settings/tokens

### Usage

```bash
cd C:\Users\PC\Desktop\diabetomobile
python scripts/download-gemma.py
```

Le script demandera le token HuggingFace interactivement, ou peut le lire via
la variable d'environnement `HF_TOKEN` :

```bash
HF_TOKEN=hf_xxxxxx python scripts/download-gemma.py
```

## Architecture d'hebergement du modele

Le modele est herberge en tant qu'asset d'une **GitHub Release** :
```
https://github.com/theongos/diasmart-mobile/releases/download/v2.1.1/gemma3-1b-it-int4.task
```

**Pourquoi pas Firebase Storage ?**
Firebase Storage necessite le plan Blaze (payant) depuis 2024. GitHub Release
est gratuit, bande passante illimitee, CDN Fastly mondial.

### Publier le modele sur une nouvelle release

```bash
# 1. Telecharger Gemma (si pas deja fait)
python scripts/download-gemma.py

# 2. Creer la release avec l'asset Gemma
gh release create vX.Y.Z \
  DiaSmart-vX.Y.Z.apk \
  gemma3-1b-it-int4.task \
  --title "DiaSmart vX.Y.Z" \
  --notes "Release notes..."

# 3. Mettre a jour MODEL_URL dans app/src/main/java/com/diabeto/data/repository/LocalAIManager.kt
#    pour pointer vers le nouveau tag
```
