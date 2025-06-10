package com.example.webradioapp.network

import android.util.Log // Ajout de l'import Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull // Ajout de l'import pour toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.IOException // Ajout de l'import IOException


object ApiClient {

    private const val USER_AGENT = "WebradioApp/1.0"
    private val TAG = "ApiClient" // Pour les logs

    private var currentBaseUrl: String? = null
    private var retrofitInstance: Retrofit? = null
    private var serviceInstance: RadioBrowserApiService? = null
    private var okHttpClientInstance: OkHttpClient? = null


    private class UserAgentInterceptor(private val userAgent: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build()
            return chain.proceed(requestWithUserAgent)
        }
    }

    private class ServerFailoverInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            var request = chain.request()
            var lastException: IOException? = null
            // Essayer jusqu'à N serveurs différents (taille de la liste des serveurs connus + 1 pour le cas de la réinitialisation)
            // Ou un nombre fixe d'essais pour éviter les boucles si la liste des serveurs est petite.
            val maxTry = ServerProvider.knownServers.size.coerceAtLeast(1) * 2
            var tryCount = 0

            while (tryCount < maxTry) {
                val currentUrlForRequest = ServerProvider.getActiveServerUrl()
                if (currentUrlForRequest == null) {
                    Log.e(TAG, "ServerFailoverInterceptor: No API server available.")
                    throw lastException ?: IOException("No API server available to make the request and no prior exceptions.")
                }

                val newHttpUrl = request.url.newBuilder()
                    .scheme(currentUrlForRequest.toHttpUrlOrNull()?.scheme ?: "https")
                    .host(currentUrlForRequest.toHttpUrlOrNull()?.host ?: "")
                    // Pas besoin de port si l'URL de base l'inclut déjà ou si c'est le port par défaut (80/443)
                    // .port(currentUrlForRequest.toHttpUrlOrNull()?.port ?: -1)
                    .build()
                request = request.newBuilder().url(newHttpUrl).build()
                Log.d(TAG, "ServerFailoverInterceptor: Attempting request to ${request.url} (Try ${tryCount + 1})")

                try {
                    val response = chain.proceed(request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "ServerFailoverInterceptor: Request successful to ${request.url}")
                        return response
                    } else {
                        Log.w(TAG, "ServerFailoverInterceptor: Server error ${response.code} from ${request.url}")
                        response.close() // Fermer la réponse non réussie
                        ServerProvider.reportFailedServer(currentUrlForRequest)
                        // Si c'est une erreur serveur (pas réseau), on ne retente pas forcément avec le même type de requête.
                        // Cependant, le reportFailedServer va changer le serveur pour le prochain essai.
                        // Pour des erreurs comme 404, 500, etc., on passe au serveur suivant.
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "ServerFailoverInterceptor: IOException for ${request.url}", e)
                    lastException = e
                    ServerProvider.reportFailedServer(currentUrlForRequest)
                }

                tryCount++
                if (tryCount >= maxTry) {
                    Log.e(TAG, "ServerFailoverInterceptor: Max tries reached.")
                    break
                }
                 // ServerProvider.getActiveServerUrl() sera appelé au début de la prochaine itération
                 // et devrait fournir un nouveau serveur si reportFailedServer a fonctionné.
            }
            throw lastException ?: IOException("All API server attempts failed after $maxTry tries.")
        }
    }

    private fun getOkHttpClient(): OkHttpClient {
        if (okHttpClientInstance == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC // BASIC pour moins de verbosité en prod
            }
            okHttpClientInstance = OkHttpClient.Builder()
                .addInterceptor(UserAgentInterceptor(USER_AGENT))
                .addInterceptor(ServerFailoverInterceptor())
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS) // Timeout plus court pour basculer plus vite
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
        return okHttpClientInstance!!
    }

    private fun getRetrofitInstance(): Retrofit {
        val newBaseUrl = ServerProvider.getActiveServerUrl()
            ?: throw IllegalStateException("No active API server URL available from ServerProvider.")

        // Si l'URL de base a changé OU si l'instance Retrofit n'existe pas encore
        if (retrofitInstance == null || newBaseUrl != currentBaseUrl) {
            Log.i(TAG, "Creating new Retrofit instance for base URL: $newBaseUrl")
            currentBaseUrl = newBaseUrl

            retrofitInstance = Retrofit.Builder()
                .baseUrl(currentBaseUrl!!) // newBaseUrl est non-null ici
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            // Forcer la recréation du service si l'instance Retrofit change
            serviceInstance = retrofitInstance!!.create(RadioBrowserApiService::class.java)
        }
        return retrofitInstance!!
    }

    // Propriété d'accès public pour le service API
    val instance: RadioBrowserApiService
        get() {
            // Assure que Retrofit est initialisé (et donc serviceInstance aussi si Retrofit a été recréé)
            getRetrofitInstance()
            return serviceInstance!!
        }

    // Méthode pour forcer un cycle de serveur et réinitialiser l'client API
    // Utile si une logique externe détecte un problème persistant et veut forcer un changement.
    @Synchronized
    fun cycleServerAndResetClient() {
        Log.i(TAG, "Forcing server cycle and API client reset.")
        ServerProvider.cycleServer() // Demande au provider de changer de serveur
        // Réinitialiser les instances pour forcer leur recréation avec la nouvelle URL de base
        retrofitInstance = null
        serviceInstance = null
        // currentBaseUrl sera mis à jour lors de la prochaine getRetrofitInstance()
    }
}
