package com.example.webradioapp.network

import android.util.Log
import java.util.Collections
import java.util.LinkedList

object ServerProvider {
    private val TAG = "ServerProvider"

    // Liste étendue de serveurs de base connus.
    // Deviendra modifiable par updateServerList.
    private var baseKnownServers: List<String> = listOf(
        "https://de1.api.radio-browser.info/",
        "https://fr1.api.radio-browser.info/",
        "https://nl1.api.radio-browser.info/",
        "https://at1.api.radio-browser.info/",
        "https://de2.api.radio-browser.info/",
        "https://fi1.api.radio-browser.info/",
        "https://us1.api.radio-browser.info/",
        "https://us2.api.radio-browser.info/"
    )

    private var shuffledServerList: LinkedList<String> = LinkedList()
    private var activeServer: String? = null
    private val currentCycleFailedServers = mutableSetOf<String>()

    val knownServers: List<String> // Getter public pour la liste de base actuelle
        @Synchronized get() = Collections.unmodifiableList(baseKnownServers.toList()) // Retourne une copie pour éviter modification externe non contrôlée


    init {
        resetAndShuffleServers()
        selectNextAvailableServer()
    }

    @Synchronized
    private fun resetAndShuffleServers() {
        shuffledServerList.clear()
        // Utilise une copie de baseKnownServers pour s'assurer que si baseKnownServers est modifié par updateServerList,
        // cette méthode utilise la version actuelle.
        shuffledServerList.addAll(baseKnownServers.toList())
        Collections.shuffle(shuffledServerList)
        currentCycleFailedServers.clear()
        Log.d(TAG, "Server list has been reset and shuffled. Total servers in shuffle list: ${shuffledServerList.size}. Base list size: ${baseKnownServers.size}")
    }

    @Synchronized
    private fun selectNextAvailableServer(): String? {
        if (shuffledServerList.isEmpty()) {
            Log.w(TAG, "Shuffled server list is exhausted. Resetting for a new cycle based on current baseKnownServers.")
            resetAndShuffleServers()
            if (shuffledServerList.isEmpty()) {
                 Log.e(TAG, "Base known server list is effectively empty. No server can be selected.")
                 activeServer = null
                 return null
            }
        }

        var attemptsBeforeReset = shuffledServerList.size
        var selectedThisTry: String? = null

        for (i in 0 until attemptsBeforeReset) {
            val potentialServer = shuffledServerList.removeFirst()
            shuffledServerList.addLast(potentialServer) // Rotation: remettre à la fin

            if (!currentCycleFailedServers.contains(potentialServer)) {
                activeServer = potentialServer
                Log.i(TAG, "New active server selected: $activeServer (out of ${baseKnownServers.size} base servers)")
                return activeServer
            }
        }

        // Si on arrive ici, tous les serveurs dans le shuffle actuel ont été marqués comme failed dans ce cycle.
        // Forcer un reset complet et une nouvelle tentative.
        Log.w(TAG, "All servers in the current shuffle cycle have failed. Forcing reset and new selection.")
        resetAndShuffleServers() // Réinitialise currentCycleFailedServers aussi
        if (shuffledServerList.isNotEmpty()) {
            // Prend le premier de la nouvelle liste mélangée (qui ne devrait pas être dans currentCycleFailedServers)
            activeServer = shuffledServerList.removeFirst()
            shuffledServerList.addLast(activeServer!!)
            Log.i(TAG, "New active server selected after forced reset: $activeServer")
        } else {
            Log.e(TAG, "No server available even after forced reset (baseKnownServers might be empty).")
            activeServer = null
        }
        return activeServer
    }

    @Synchronized
    fun getActiveServerUrl(): String? {
        // Si le serveur actif est null ou a échoué entre-temps, en sélectionner un nouveau.
        if (activeServer == null || currentCycleFailedServers.contains(activeServer)) {
           Log.d(TAG, "Active server is null or has failed ($activeServer). Attempting to select a new one.")
           return selectNextAvailableServer()
        }
        return activeServer
    }

    @Synchronized
    fun reportFailedServer(serverUrl: String): String? {
        Log.w(TAG, "Server reported as failed: $serverUrl")
        currentCycleFailedServers.add(serverUrl)

        if (serverUrl == activeServer || activeServer == null) {
            Log.i(TAG, "Active server $serverUrl failed. Selecting next available server.")
            return selectNextAvailableServer()
        }
        Log.d(TAG, "Reported failed server $serverUrl was not the active one ($activeServer). Keeping current active server for now.")
        return activeServer
    }

    @Synchronized
    fun cycleServer(): String? {
        Log.i(TAG, "Cycling server manually requested.")
        if (activeServer != null) {
            // Simuler un échec du serveur actif pour forcer la sélection d'un nouveau différent
            // On l'ajoute aux échecs du cycle pour qu'il ne soit pas immédiatement repris par selectNextAvailableServer
            // si la liste shuffledServerList est courte ou si c'est le seul restant non marqué comme failed.
            currentCycleFailedServers.add(activeServer!!)
            Log.d(TAG, "Marking current active server $activeServer as 'failed for this cycle' to ensure selection of a different one, if possible.")
        }
        // Forcer une nouvelle sélection. Si activeServer était le dernier valide du cycle, cela va trigger un reset.
        return selectNextAvailableServer()
    }

    @Synchronized
    fun updateServerList(newServers: List<String>) {
        Log.i(TAG, "Attempting to update server list with ${newServers.size} new servers.")
        if (newServers.isNotEmpty()) {
            baseKnownServers = LinkedList(newServers) // Réassigner la liste de base
            Log.i(TAG, "baseKnownServers updated. Size: ${baseKnownServers.size}")
            resetAndShuffleServers()      // Réinitialiser les listes de travail et les états d'échec
            selectNextAvailableServer()   // Sélectionner un nouveau serveur actif à partir de la nouvelle liste
            Log.i(TAG, "Server list updated. New active server: $activeServer")
        } else {
            Log.w(TAG, "Attempted to update server list with an empty list. No changes made to baseKnownServers.")
        }
    }
}
