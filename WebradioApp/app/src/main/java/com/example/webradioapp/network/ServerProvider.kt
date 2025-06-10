package com.example.webradioapp.network

import android.util.Log
import java.util.Collections
import java.util.LinkedList

object ServerProvider {
    private val TAG = "ServerProvider"

    // Liste de serveurs de base connus. En production, cela viendrait d'un DNS lookup.
    val knownServers: LinkedList<String> = LinkedList(listOf( // Made public for ApiClient's maxTry
        "https://de1.api.radio-browser.info/",
        "https://fr1.api.radio-browser.info/",
        "https://nl1.api.radio-browser.info/",
        "https://at1.api.radio-browser.info/"
        // TODO: Ajouter d'autres serveurs si connus et fiables ou implémenter le DNS lookup
    ))

    private var activeServer: String? = null
    private val failedServers = mutableSetOf<String>()
    private var currentIndex = -1 // Pour une sélection cyclique simple en cas de fallback

    init {
        selectNextAvailableServer()
    }

    @Synchronized
    private fun selectNextAvailableServer(): String? {
        val availableServers = LinkedList(knownServers)
        availableServers.removeAll(failedServers)

        if (availableServers.isEmpty()) {
            if (failedServers.isNotEmpty()) { // Tous les serveurs connus ont échoué au moins une fois
                Log.w(TAG, "All known servers have failed. Resetting failed list and attempting to cycle through known servers.")
                failedServers.clear() // Réinitialiser la liste des échecs
                // Essayer de parcourir la liste originale de manière cyclique
                currentIndex = (currentIndex + 1) % knownServers.size
                activeServer = knownServers[currentIndex]
                Log.i(TAG, "Reset failed servers. New active server (cyclical): $activeServer")
                return activeServer
            } else { // knownServers est vide dès le départ
                Log.e(TAG, "No known servers configured.")
                activeServer = null
                return null
            }
        }

        // Randomiser les serveurs disponibles restants pour la sélection
        Collections.shuffle(availableServers)
        activeServer = availableServers.first
        Log.i(TAG, "New active server selected: $activeServer")
        return activeServer
    }

    @Synchronized
    fun getActiveServerUrl(): String? {
        // Si aucun serveur actif, ou si le serveur actif est dans la liste des échecs (ce qui ne devrait pas arriver avec la logique actuelle mais par sécurité)
        if (activeServer == null || failedServers.contains(activeServer)) {
            return selectNextAvailableServer()
        }
        return activeServer
    }

    @Synchronized
    fun reportFailedServer(serverUrl: String): String? {
        Log.w(TAG, "Server reported as failed: $serverUrl")
        failedServers.add(serverUrl)
        // Si le serveur qui a échoué est celui qu'on pensait actif, ou si on n'en avait pas, on en choisit un nouveau.
        if (serverUrl == activeServer || activeServer == null) {
            return selectNextAvailableServer()
        }
        // Sinon, on garde l'actuel s'il n'a pas échoué.
        return activeServer
    }

    // Méthode pour forcer un changement de serveur, utile pour les tests ou une action utilisateur
    @Synchronized
    fun cycleServer(): String? {
        Log.i(TAG, "Cycling server manually.")
        // Simule un échec du serveur actif pour forcer la sélection d'un nouveau
        if(activeServer != null) {
            failedServers.add(activeServer!!)
        }
        return selectNextAvailableServer()
    }
}
