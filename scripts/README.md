# Scripts utilitaires DiaSmart

## upload-gemma.js — Upload du modele Gemma 3 1B vers Firebase Storage

### Etape 1 : Activer Firebase Storage

Ouvre :
```
https://console.firebase.google.com/project/project-d-r1997t/storage
```
Clique **Get Started** → **Start in production mode** → region `europe-west1` ou `us-central1` → **Done**.

### Etape 2 : Telecharger la cle de service

Ouvre :
```
https://console.firebase.google.com/project/project-d-r1997t/settings/serviceaccounts/adminsdk
```
Clique **Generate new private key** → sauvegarde le fichier comme :
```
scripts/serviceAccountKey.json
```
**ATTENTION** : ce fichier ne doit JAMAIS etre commit dans git. Il est deja dans `.gitignore`.

### Etape 3 : Telecharger Gemma 3 1B

Depuis HuggingFace (accepter la licence Gemma avant) :
```
https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task
```
Sauvegarde le fichier a la racine du projet :
```
C:\Users\PC\Desktop\diabetomobile\gemma3-1b-it-int4.task
```

### Etape 4 : Deployer les regles Storage

```bash
firebase deploy --only storage
```

### Etape 5 : Lancer l'upload

Depuis la racine du projet :
```bash
cd functions && npm install firebase-admin  # (deja installe normalement)
cd ..
node scripts/upload-gemma.js
```

L'upload prend ~5-15 minutes selon la connexion (~550 MB).

### Verification

Dans la console Firebase Storage tu devrais voir :
```
/models/gemma3-1b-it-int4.task  (~550 MB)
```

Dans l'app, le telechargement fonctionnera automatiquement depuis l'ecran
"Mode hors-ligne" des que l'utilisateur clique "Telecharger le modele IA".
