# Diabeto - Application Mobile de Gestion du Diabète

## 📱 Description

Diabeto est une application Android moderne et complète pour la gestion du diabète. Elle permet aux professionnels de santé et aux patients de suivre efficacement la glycémie, les médicaments et les rendez-vous.

## 🏗️ Architecture

L'application suit les meilleures pratiques Android avec :

- **Architecture MVVM** : Séparation claire des responsabilités
- **Jetpack Compose** : UI moderne et déclarative
- **Room Database** : Persistance des données locale
- **Hilt** : Injection de dépendances
- **Navigation Compose** : Navigation fluide entre les écrans
- **Coroutines & Flow** : Programmation asynchrone réactive
- **Material Design 3** : Interface utilisateur moderne

## 📁 Structure du Projet

```
com.diabeto/
├── data/
│   ├── database/       # Configuration Room
│   ├── dao/            # Data Access Objects
│   ├── entity/         # Entités de la base de données
│   └── repository/     # Repositories pour l'accès aux données
├── di/                 # Injection de dépendances (Hilt)
├── ui/
│   ├── components/     # Composants réutilisables
│   ├── navigation/     # Configuration de la navigation
│   ├── screens/        # Écrans de l'application
│   ├── theme/          # Thème Material3
│   └── viewmodel/      # ViewModels
└── utils/              # Utilitaires
```

## ✨ Fonctionnalités

### 👥 Gestion des Patients
- Ajout, modification et suppression de patients
- Informations complètes (nom, contact, type de diabète, etc.)
- Recherche et filtrage

### 📊 Suivi Glycémique
- Enregistrement des lectures de glucose
- Graphique d'évolution
- Statistiques détaillées (moyenne, min/max, temps dans la cible)
- Classification automatique (hypo/hyperglycémie)
- Contexte de mesure (à jeun, après repas, etc.)

### 💊 Gestion des Médicaments
- Liste des médicaments par patient
- Dosage et fréquence
- Rappels de prise
- Activation/désactivation

### 📅 Rendez-vous
- Planification des consultations
- Types de rendez-vous (consultation, examen, téléconsultation)
- Confirmation des rendez-vous
- Rappels automatiques

### 📈 Tableau de Bord
- Vue d'ensemble des patients
- Statistiques globales
- Prochains rendez-vous
- Rappels de médicaments

## 🎨 Design

- **Thème Material Design 3** avec couleurs personnalisées
- **Responsive** : Adapté aux téléphones et tablettes
- **Dark mode** supporté
- **Animations fluides** avec Compose

## 🛠️ Technologies Utilisées

| Technologie | Version |
|------------|---------|
| Kotlin | 1.9.20 |
| Android SDK | 34 |
| Jetpack Compose | BOM 2024.02.00 |
| Room | 2.6.1 |
| Hilt | 2.48 |
| Navigation Compose | 2.7.7 |

## 🚀 Installation

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou supérieur
- JDK 17
- Android SDK 34

### Étapes

1. Cloner le projet :
```bash
git clone https://github.com/votre-repo/diabeto.git
```

2. Ouvrir dans Android Studio

3. Synchroniser le projet avec Gradle

4. Lancer l'application sur un émulateur ou appareil physique

## 📦 Construction

### Debug
```bash
./gradlew assembleDebug
```

### Release
```bash
./gradlew assembleRelease
```

## 🧪 Tests

```bash
# Tests unitaires
./gradlew test

# Tests instrumentés
./gradlew connectedAndroidTest
```

## 📝 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 👨‍💻 Auteur

Développé avec ❤️ pour améliorer la gestion du diabète.

## 🙏 Remerciements

- Material Design Team
- Android Jetpack Team
- Communauté Kotlin
