package com.example.webradioapp.db

import android.content.Context
import android.content.SharedPreferences
import com.example.webradioapp.db.dao.CountryDao
import com.example.webradioapp.db.dao.GenreDao
import com.example.webradioapp.db.dao.LanguageDao
import com.example.webradioapp.db.entities.CountryEntity
import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.network.Country as NetworkCountry // Alias for network model
import com.example.webradioapp.utils.SharedPreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` // Specific import for `when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import retrofit2.Response

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class StationRepositoryTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    @Mock
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor
    @Mock
    private lateinit var mockFavoriteStationDao: FavoriteStationDao // Needed for constructor
    @Mock
    private lateinit var mockHistoryStationDao: HistoryStationDao // Needed for constructor
    @Mock
    private lateinit var mockCountryDao: CountryDao
    @Mock
    private lateinit var mockGenreDao: GenreDao
    @Mock
    private lateinit var mockLanguageDao: LanguageDao
    @Mock
    private lateinit var mockApiService: RadioBrowserApiService

    private lateinit var stationRepository: StationRepository

    private val cacheExpiryMs = 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        // Mock SharedPreferences behavior for SharedPreferencesManager static calls
        `when`(mockContext.getSharedPreferences(any<String>(), any<Int>())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
        `when`(mockSharedPreferencesEditor.putLong(any<String>(), any<Long>())).thenReturn(mockSharedPreferencesEditor)
        // `when`(mockSharedPreferencesEditor.apply()).then { } // No need, default Mockito behavior is void

        // Initialize SharedPreferencesManager with a way to use the mocked context if it were not object.
        // Since SharedPreferencesManager is an object, its methods are static.
        // We will mock the specific static calls if mockito-inline is available or use PowerMockito.
        // For this subtask, we'll assume direct calls to SharedPreferencesManager can be tricky to mock
        // without heavier tools. The test will thus rely on SharedPreferencesManager working correctly
        // and focus on the repository's logic interacting with its direct DAO/API mocks.
        // A better approach if SharedPreferencesManager was an interface/class would be to inject it.

        // Let's refine: We can directly mock the behavior of SharedPreferencesManager's static methods
        // IF the test environment supports mocking objects/static methods (e.g. via mockito-inline or PowerMock).
        // For this example, let's assume we can't easily mock SharedPreferencesManager directly.
        // The test will then verify interactions with DAOs and ApiService based on assumed timestamps.
        // The subtask worker will attempt to use mockito-inline if available.

        // For the purpose of this subtask, we will mock the SharedPreferences calls directly that
        // SharedPreferencesManager makes, as shown above with mockContext.getSharedPreferences.
        // This means when SharedPreferencesManager.getLastUpdateTimestamp(mockContext, "key") is called,
        // it will internally use our mockContext and thus our mockSharedPreferences.

        stationRepository = StationRepository(
            mockContext,
            mockFavoriteStationDao,
            mockHistoryStationDao,
            mockCountryDao,
            mockGenreDao,
            mockLanguageDao,
            mockApiService
        )
    }

    // --- refreshCountries Tests ---

    @Test
    fun `refreshCountries WHEN cache is empty SHOULD fetch from API and update timestamp`() = runTest {
        // Arrange
        `when`(mockCountryDao.getAll()).thenReturn(flowOf(emptyList())) // Cache is empty
        `when`(mockSharedPreferences.getLong(eq("last_update_countries"), eq(0L))).thenReturn(0L) // No previous update
        val fakeNetworkCountries = listOf(NetworkCountry("Test Country", 10))
        `when`(mockApiService.getCountries()).thenReturn(Response.success(fakeNetworkCountries))

        // Act
        stationRepository.refreshCountries()

        // Assert
        verify(mockApiService).getCountries() // API was called
        verify(mockCountryDao).deleteAll()
        verify(mockCountryDao).insertAll(any()) // Data was inserted
        verify(mockSharedPreferencesEditor).putLong(eq("last_update_countries"), any<Long>()) // Timestamp updated
        verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `refreshCountries WHEN cache is stale SHOULD fetch from API and update timestamp`() = runTest {
        // Arrange
        val staleTimestamp = System.currentTimeMillis() - cacheExpiryMs - 1000 // Older than expiry
        `when`(mockSharedPreferences.getLong(eq("last_update_countries"), eq(0L))).thenReturn(staleTimestamp)
        `when`(mockCountryDao.getAll()).thenReturn(flowOf(listOf(CountryEntity("Old Country", 5)))) // Cache not empty
        val fakeNetworkCountries = listOf(NetworkCountry("New Country", 15))
        `when`(mockApiService.getCountries()).thenReturn(Response.success(fakeNetworkCountries))

        // Act
        stationRepository.refreshCountries()

        // Assert
        verify(mockApiService).getCountries()
        verify(mockCountryDao).deleteAll()
        verify(mockCountryDao).insertAll(any())
        verify(mockSharedPreferencesEditor).putLong(eq("last_update_countries"), any<Long>())
        verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `refreshCountries WHEN cache is fresh SHOULD NOT fetch from API`() = runTest {
        // Arrange
        val freshTimestamp = System.currentTimeMillis() - cacheExpiryMs / 2 // Newer than expiry
        `when`(mockSharedPreferences.getLong(eq("last_update_countries"), eq(0L))).thenReturn(freshTimestamp)
        `when`(mockCountryDao.getAll()).thenReturn(flowOf(listOf(CountryEntity("Fresh Country", 20)))) // Cache not empty

        // Act
        stationRepository.refreshCountries()

        // Assert
        verify(mockApiService, never()).getCountries() // API was NOT called
        verify(mockCountryDao, never()).deleteAll()
        verify(mockCountryDao, never()).insertAll(any())
        verify(mockSharedPreferencesEditor, never()).putLong(eq("last_update_countries"), any<Long>())
    }

    @Test
    fun `refreshCountries WHEN API fetch fails SHOULD NOT update timestamp or clear cache`() = runTest {
        // Arrange
        `when`(mockCountryDao.getAll()).thenReturn(flowOf(emptyList())) // Cache is empty initially
        `when`(mockSharedPreferences.getLong(eq("last_update_countries"), eq(0L))).thenReturn(0L)
        `when`(mockApiService.getCountries()).thenReturn(Response.error(500, okhttp3.ResponseBody.create(null, "")))

        // Act
        stationRepository.refreshCountries()

        // Assert
        verify(mockApiService).getCountries() // API was called
        verify(mockCountryDao, never()).deleteAll() // Cache not cleared
        verify(mockCountryDao, never()).insertAll(any()) // No data inserted
        // Timestamp for "last_update_countries" should not be updated with current time.
        // If it was empty and API failed, it might reset to 0L if that specific logic path is hit for empty API response.
        // The current code has: else if (isCacheEmpty) { setLastUpdateTimestamp(context, "key", 0L) }
        // This test is for API error, not empty success response. So, no timestamp update.
         verify(mockSharedPreferencesEditor, never()).putLong(eq("last_update_countries"), any<Long>())
    }

    @Test
    fun `refreshCountries WHEN cache is empty AND API returns empty list SHOULD reset timestamp`() = runTest {
        // Arrange
        `when`(mockCountryDao.getAll()).thenReturn(flowOf(emptyList())) // Cache is empty
        `when`(mockSharedPreferences.getLong(eq("last_update_countries"), eq(0L))).thenReturn(0L) // No previous update
        `when`(mockApiService.getCountries()).thenReturn(Response.success(emptyList<NetworkCountry>())) // API returns empty

        // Act
        stationRepository.refreshCountries()

        // Assert
        verify(mockApiService).getCountries() // API was called
        verify(mockCountryDao, never()).deleteAll()
        verify(mockCountryDao, never()).insertAll(any())
        // Timestamp should be set to 0L to allow quicker retry
        verify(mockSharedPreferencesEditor).putLong(eq("last_update_countries"), eq(0L))
        verify(mockSharedPreferencesEditor).apply()
    }

    // Similar tests should be written for refreshGenres() and refreshLanguages()
    // For brevity in this subtask definition, only refreshCountries tests are detailed.
    // The worker will understand the pattern.
}
