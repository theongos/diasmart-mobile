package com.diabeto

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

/**
 * Application principale DiaSmart avec Hilt.
 *
 * Firebase App Check est DÉSACTIVÉ pour le moment car l'app est
 * distribuée en side-loading (APK direct). App Check nécessite soit
 * un debug token enregistré dans la console, soit Play Integrity
 * (Play Store uniquement). Sans token enregistré, App Check bloque
 * silencieusement TOUTES les requêtes Firebase (Auth, Firestore),
 * causant le blocage sur la page de connexion.
 *
 * À réactiver quand l'app sera sur le Play Store.
 */
@HiltAndroidApp
class DiabetoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // App Check DÉSACTIVÉ — cause le blocage de connexion en side-loading
        // initFirebaseAppCheck()
    }
}
