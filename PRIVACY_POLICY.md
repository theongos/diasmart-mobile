# Politique de Confidentialité — DiaSmart

**Dernière mise à jour : Mars 2026**

---

## 1. Introduction

Bienvenue sur **DiaSmart**, une application mobile de gestion du diabète développée par **NGOS THEODORE**. Nous prenons la protection de vos données personnelles très au sérieux, en particulier compte tenu de la nature sensible des informations de santé que vous nous confiez.

La présente Politique de Confidentialité a pour objet de vous informer de manière claire, transparente et complète sur la façon dont DiaSmart collecte, utilise, stocke, partage et protège vos données personnelles, conformément au **Règlement Général sur la Protection des Données (RGPD)** de l'Union Européenne et aux législations applicables en matière de protection des données.

En utilisant l'application DiaSmart, vous reconnaissez avoir lu et accepté les termes de la présente politique. Si vous n'êtes pas d'accord avec ces termes, veuillez ne pas utiliser l'application.

**Responsable du traitement :**
- Nom : NGOS THEODORE
- Adresse e-mail : ngostheo30@gmail.com

---

## 2. Données Collectées

Dans le cadre du fonctionnement de DiaSmart, nous collectons les catégories de données suivantes :

### 2.1 Données de Santé (données sensibles)
- Mesures de glycémie (taux de glucose sanguin)
- Taux d'HbA1c
- Traitements médicamenteux (médicaments, posologies, horaires de prise)
- Rendez-vous médicaux et consultations
- Entrées du journal de santé personnel
- Objectifs glycémiques personnalisés
- Historique des tendances glycémiques

### 2.2 Données Personnelles d'Identification
- Nom et prénom
- Adresse e-mail
- Date de naissance
- Type de diabète (Type 1, Type 2, gestationnel, etc.)
- Informations de profil médical

### 2.3 Données d'Utilisation
- Données de navigation et d'interaction au sein de l'application
- Identifiants de session et jetons d'authentification
- Journaux d'erreurs et données de diagnostic techniques
- Préférences et paramètres de l'application
- Informations sur l'appareil (modèle, système d'exploitation, version de l'application)
- Adresse IP (collectée par les services Firebase lors des connexions)

---

## 3. Finalités du Traitement des Données

Vos données sont collectées et traitées pour les finalités suivantes :

- **Fourniture du service** : permettre le suivi et la gestion personnalisée de votre diabète
- **Communication patient-médecin** : faciliter le partage sécurisé de données de santé avec votre professionnel de santé désigné
- **Assistance intelligente** : alimenter le chatbot basé sur l'intelligence artificielle (Google Gemini) pour vous fournir des informations générales sur la gestion du diabète
- **Téléconsultation** : permettre les appels vidéo sécurisés avec votre médecin via Jitsi/WebRTC
- **Notifications et rappels** : vous envoyer des rappels de médicaments, d'objectifs et de rendez-vous via Firebase Cloud Messaging (FCM)
- **Sauvegarde et restauration** : assurer la continuité de vos données en cas de changement d'appareil
- **Amélioration du service** : analyser les données d'utilisation anonymisées pour améliorer les fonctionnalités de l'application
- **Sécurité** : prévenir les fraudes, les accès non autorisés et assurer l'intégrité des données

La base légale du traitement de vos données de santé est votre **consentement explicite**, que vous accordez lors de l'inscription et que vous pouvez retirer à tout moment.

---

## 4. Stockage des Données

### 4.1 Base de données locale (Room Database)
Certaines données sont stockées localement sur votre appareil dans une base de données Room (SQLite) chiffrée. Cela permet l'accès à vos données hors connexion et garantit une disponibilité immédiate.

### 4.2 Cloud Firebase (Google Cloud)
Vos données sont synchronisées et sauvegardées sur les serveurs Firebase de Google, conformément aux standards de sécurité de Google Cloud Platform. Les services Firebase utilisés sont :

| Service Firebase | Utilisation |
|---|---|
| **Firebase Authentication** | Gestion de la connexion et de l'authentification sécurisée des utilisateurs |
| **Cloud Firestore** | Stockage et synchronisation en temps réel des données de santé |
| **Firebase Cloud Messaging (FCM)** | Envoi de notifications push (rappels, alertes) |
| **Firebase Hosting** | Hébergement du site web et de la documentation |

### 4.3 Sauvegarde Cloud
La sauvegarde cloud est disponible pour permettre la restauration de vos données en cas de perte ou de changement d'appareil. Cette fonctionnalité est activée avec votre consentement.

Les serveurs Firebase peuvent être localisés dans différentes régions géographiques. Pour plus d'informations sur la localisation des données Google, consultez : [https://firebase.google.com/support/privacy](https://firebase.google.com/support/privacy)

---

## 5. Services Tiers

DiaSmart intègre les services tiers suivants, chacun disposant de sa propre politique de confidentialité :

### 5.1 Google Gemini AI
Le chatbot intégré à DiaSmart utilise l'API Google Gemini pour fournir des réponses à vos questions générales sur le diabète. Les requêtes soumises au chatbot peuvent être traitées par les serveurs de Google. Nous vous déconseillons de partager des informations d'identification personnelle directement dans le chatbot.

- Politique de confidentialité Google : [https://policies.google.com/privacy](https://policies.google.com/privacy)

### 5.2 Jitsi / WebRTC
Les fonctionnalités de téléconsultation par appel vidéo sont réalisées via Jitsi, une solution open source basée sur WebRTC. Les communications vidéo sont chiffrées de bout en bout.

- Politique de confidentialité Jitsi : [https://jitsi.org/security/](https://jitsi.org/security/)

### 5.3 Google Firebase (Firebase Suite)
Voir section 4 pour le détail des services Firebase utilisés.

---

## 6. Partage des Données

Vos données personnelles et de santé ne sont **jamais vendues** à des tiers. Le partage de vos données est strictement limité aux cas suivants :

### 6.1 Partage Patient-Médecin
Le seul partage de vos données de santé prévu par DiaSmart est celui que vous initiez vous-même entre vous (le patient) et votre médecin ou professionnel de santé désigné. Ce partage :
- Requiert votre **consentement explicite et préalable**
- Est limité au professionnel de santé que vous avez vous-même sélectionné
- Peut être révoqué à tout moment depuis les paramètres de l'application

### 6.2 Sous-traitants techniques
Vos données peuvent être traitées par nos sous-traitants techniques (Google Firebase, Google Gemini, Jitsi) dans le strict cadre de la fourniture des services décrits dans la présente politique, et sous couvert de contrats garantissant la protection de vos données.

### 6.3 Obligations légales
Nous pouvons être amenés à divulguer vos données si la loi l'exige ou en réponse à une demande légale valide d'une autorité compétente.

---

## 7. Vos Droits (RGPD)

Conformément au RGPD, vous disposez des droits suivants concernant vos données personnelles :

### 7.1 Droit d'accès
Vous pouvez demander à obtenir une copie de toutes les données personnelles que nous détenons vous concernant.

### 7.2 Droit de rectification
Vous pouvez demander la correction de données inexactes ou incomplètes vous concernant.

### 7.3 Droit à l'effacement (« droit à l'oubli »)
Vous pouvez demander la suppression de vos données personnelles. Notez que la suppression du compte entraîne la suppression définitive et irréversible de toutes vos données de santé stockées dans le cloud.

### 7.4 Droit à la portabilité des données
Vous pouvez demander l'exportation de vos données dans un format structuré, couramment utilisé et lisible par machine (CSV ou JSON).

### 7.5 Droit d'opposition et de limitation du traitement
Vous pouvez vous opposer au traitement de vos données ou demander sa limitation dans certaines circonstances.

### 7.6 Droit de retrait du consentement
Vous pouvez retirer votre consentement au traitement de vos données à tout moment, sans que cela n'affecte la licéité du traitement effectué avant ce retrait.

### 7.7 Droit d'introduire une réclamation
Si vous estimez que le traitement de vos données porte atteinte à vos droits, vous avez le droit d'introduire une réclamation auprès d'une autorité de protection des données compétente.

**Pour exercer l'un de ces droits**, contactez-nous à : **ngostheo30@gmail.com**

Nous nous engageons à répondre à votre demande dans un délai maximum de **30 jours**.

---

## 8. Conservation des Données

Vos données sont conservées selon les durées suivantes :

| Type de données | Durée de conservation |
|---|---|
| Données de compte (profil, e-mail) | Jusqu'à la suppression du compte |
| Données de santé (glycémie, HbA1c, etc.) | Jusqu'à la suppression du compte ou demande d'effacement |
| Journaux d'activité et données techniques | 12 mois maximum |
| Données de sauvegarde | 30 jours après la suppression du compte |
| Données de consentement | 5 ans à compter du retrait du consentement |

À l'expiration de ces délais, vos données sont supprimées de façon permanente et sécurisée.

---

## 9. Sécurité des Données

Nous mettons en œuvre des mesures techniques et organisationnelles appropriées pour protéger vos données contre tout accès non autorisé, altération, divulgation ou destruction :

- **Chiffrement des données** : les données sensibles sont chiffrées en transit (TLS/HTTPS) et au repos
- **Règles de sécurité Firebase** : des règles d'accès strictes garantissent que chaque utilisateur ne peut accéder qu'à ses propres données
- **Accès authentifié** : toutes les opérations sur les données nécessitent une authentification préalable via Firebase Authentication
- **Base de données locale chiffrée** : la base Room sur l'appareil est protégée par chiffrement
- **Chiffrement des appels vidéo** : les communications via Jitsi/WebRTC sont chiffrées de bout en bout
- **Mots de passe** : les mots de passe ne sont jamais stockés en clair ; ils sont gérés par Firebase Authentication

Malgré ces mesures, aucun système de transmission ou de stockage électronique n'est sécurisé à 100 %. Nous vous encourageons à utiliser un mot de passe robuste et à activer les options de sécurité disponibles sur votre appareil.

---

## 10. Cookies et Technologies Similaires

### 10.1 Application Mobile
L'application mobile DiaSmart n'utilise pas de cookies au sens traditionnel du terme. Des technologies similaires (jetons de session, identifiants locaux) sont utilisées pour maintenir votre session active et assurer le bon fonctionnement de l'application.

### 10.2 Site Web DiaSmart
Le site web associé à DiaSmart peut utiliser des cookies techniques strictement nécessaires au fonctionnement du site. Ces cookies ne collectent pas d'informations permettant de vous identifier à des fins publicitaires. Aucun cookie de traçage ou de publicité comportementale n'est utilisé.

---

## 11. Protection des Mineurs

DiaSmart est destinée aux personnes âgées de **16 ans et plus**. Nous ne collectons pas sciemment des données personnelles concernant des enfants de moins de 16 ans sans le consentement préalable vérifiable d'un parent ou tuteur légal.

Si vous êtes le parent ou tuteur d'un enfant de moins de 16 ans et que vous pensez qu'il nous a fourni des données personnelles sans votre consentement, veuillez nous contacter immédiatement à **ngostheo30@gmail.com**. Nous procéderons à la suppression de ces données dans les meilleurs délais.

Dans le cas où DiaSmart est utilisée pour la gestion du diabète d'un enfant, le compte doit être créé et géré par un parent ou tuteur légal.

---

## 12. Transferts Internationaux de Données

Vos données peuvent être transférées et traitées dans des pays situés hors de votre pays de résidence, notamment aux États-Unis, où sont hébergés les services Google Firebase et Google Gemini. Ces transferts sont encadrés par les **Clauses Contractuelles Types (CCT)** approuvées par la Commission Européenne et par les mécanismes de conformité de Google, qui est certifié sous les accords de transfert de données appropriés.

---

## 13. Modifications de la Politique de Confidentialité

Nous nous réservons le droit de modifier la présente Politique de Confidentialité à tout moment pour refléter les évolutions légales, techniques ou fonctionnelles de DiaSmart.

En cas de modification substantielle, vous serez informé :
- Par une notification dans l'application
- Par e-mail à l'adresse associée à votre compte
- Par la mise à jour de la date « Dernière mise à jour » en haut de ce document

La version mise à jour entrera en vigueur à la date indiquée. Votre utilisation continue de l'application après cette date constituera votre acceptation des modifications. Si vous n'acceptez pas les modifications, vous avez le droit de supprimer votre compte et de cesser d'utiliser l'application.

---

## 14. Contact

Pour toute question, préoccupation ou demande relative à la présente Politique de Confidentialité ou au traitement de vos données personnelles, veuillez nous contacter :

**NGOS THEODORE**
Développeur de DiaSmart
E-mail : **ngostheo30@gmail.com**

Nous nous engageons à traiter votre demande dans un délai de **30 jours ouvrables**.

---

## 15. Droit Applicable

La présente Politique de Confidentialité est régie par le **Règlement (UE) 2016/679 du Parlement européen et du Conseil** du 27 avril 2016 (RGPD), ainsi que par toute législation nationale applicable en matière de protection des données personnelles.

---

*DiaSmart — Votre partenaire intelligent dans la gestion du diabète*
*© 2026 NGOS THEODORE. Tous droits réservés.*
