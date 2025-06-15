package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView // Added
import android.widget.LinearLayout // Added
// import android.widget.EditText // Removed
import android.widget.ProgressBar
// import android.widget.Spinner // Removed
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.ApiClient
import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.network.Country // Assuming Country is in this package
import com.example.webradioapp.network.Tag // Assuming Tag is in this package
import com.example.webradioapp.network.model.toDomain
import com.example.webradioapp.services.StreamingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SearchFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerViewStations: RecyclerView
    private lateinit var stationAdapter: StationAdapter
    private lateinit var progressBar: ProgressBar
    // private lateinit var tvError: TextView // Removed

    // New state views
    private lateinit var layoutSearchMessageState: LinearLayout
    private lateinit var ivSearchMessageIcon: ImageView
    private lateinit var tvSearchMessageText: TextView

    // New AutoCompleteTextViews
    private lateinit var actvCountryDropdown: AutoCompleteTextView
    private lateinit var actvCategoryDropdown: AutoCompleteTextView

    private lateinit var buttonClearFilters: Button
    private lateinit var buttonApplyFilters: Button

    private val anyCountryString = "Any Country"
    private val anyCategoryString = "Any Category"
    private lateinit var countryAdapter: ArrayAdapter<String>
    private lateinit var categoryAdapter: ArrayAdapter<String> // Added for categories

    // private var filterCountryJob: Job? = null // Removed

    private var countriesList: List<Country> = emptyList()
    private var allDisplayCountryNames: MutableList<String> = mutableListOf(anyCountryString)
    private var tagsList: List<Tag> = emptyList()

    private val apiService: RadioBrowserApiService by lazy { ApiClient.instance }
    private val stationViewModel: com.example.webradioapp.viewmodels.StationViewModel by viewModels()
    private val favoritesViewModel: com.example.webradioapp.viewmodels.FavoritesViewModel by viewModels() // To observe all favorites
    private var searchJob: Job? = null // This is for the main search, keep it separate
    private var currentStationList: List<RadioStation> = emptyList()
    private var currentFavoritesSet: Set<String> = emptySet()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize class member countryAdapter
        // Use this.anyCountryString which is a class member
        this.countryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf(this.anyCountryString))
        this.countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Initialize UI elements from the view
        searchView = view.findViewById(R.id.search_view)
        recyclerViewStations = view.findViewById(R.id.recycler_view_stations)
        progressBar = view.findViewById(R.id.progress_bar_search)
        // tvError = view.findViewById(R.id.tv_search_error) // Removed

        layoutSearchMessageState = view.findViewById(R.id.layout_search_message_state)
        ivSearchMessageIcon = view.findViewById(R.id.iv_search_message_icon)
        tvSearchMessageText = view.findViewById(R.id.tv_search_message_text)

        buttonClearFilters = view.findViewById(R.id.button_clear_search_filters)
        buttonApplyFilters = view.findViewById(R.id.button_apply_filters_search)

        actvCountryDropdown = view.findViewById(R.id.actv_country_dropdown)
        actvCategoryDropdown = view.findViewById(R.id.actv_category_dropdown)

        // Set adapters
        actvCountryDropdown.setAdapter(this.countryAdapter)
        // Initialize categoryAdapter and set it
        this.categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf(this.anyCategoryString))
        actvCategoryDropdown.setAdapter(this.categoryAdapter)


        buttonClearFilters.setOnClickListener {
            actvCountryDropdown.setText(anyCountryString, false)
            actvCategoryDropdown.setText(anyCategoryString, false)
            // Trigger search with current SearchView text and now-cleared spinner selections
            performSearch(searchView.query?.toString())
        }

        buttonApplyFilters.setOnClickListener { // Added
            // Clear focus from SearchView to hide keyboard, if it has focus
            searchView.clearFocus()
            // Perform search using the current text in SearchView and selected spinner values
            performSearch(searchView.query?.toString())
        }

        setupRecyclerView()
        setupSearchView() // setupSearchView might also trigger performSearch, ensure order is fine
        observeFavoriteChanges()

        showSearchMessageState("Search for radio stations above.", R.drawable.ic_search) // Initial state

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSpinnerData() // Load data after view is created

        // Removed TextWatcher for etFilterCountry
    }

    private fun loadSpinnerData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val countriesResponse = apiService.getCountries()
                if (countriesResponse.isSuccessful) {
                    countriesList = countriesResponse.body() ?: emptyList()
                    val newFullCountryList = mutableListOf(anyCountryString)
                    newFullCountryList.addAll(countriesList.map { it.name }.sorted())

                    allDisplayCountryNames.clear()
                    allDisplayCountryNames.addAll(newFullCountryList)

                    // Update the existing countryAdapter instance in place
                    countryAdapter.clear()
                    countryAdapter.addAll(newFullCountryList)
                    countryAdapter.notifyDataSetChanged()
                    actvCountryDropdown.setText(anyCountryString, false) // Set initial text without filtering

                } else {
                    Log.e("SearchFragment", "Failed to load countries: ${countriesResponse.message()}")
                    Toast.makeText(requireContext(), "Could not load country list. Search functionality may be limited.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Error loading countries", e)
                Toast.makeText(requireContext(), "Error initializing country filter. Please try again.", Toast.LENGTH_LONG).show()
            }

            try {
                val tagsResponse = apiService.getTags(limit = 200) // Fetch more relevant tags
                if (tagsResponse.isSuccessful) {
                    tagsList = tagsResponse.body() ?: emptyList()
                    val displayTags: MutableList<String> = mutableListOf(anyCategoryString)
                    val sortedTagNames: List<String> = tagsList.map { it.name }.distinct().sorted()
                    displayTags.addAll(sortedTagNames)

                    // Update the existing categoryAdapter instance
                    categoryAdapter.clear()
                    categoryAdapter.addAll(displayTags)
                    categoryAdapter.notifyDataSetChanged()
                    actvCategoryDropdown.setText(anyCategoryString, false) // Set initial text without filtering
                } else {
                    Log.e("SearchFragment", "Failed to load tags/categories: ${tagsResponse.message()}")
                    Toast.makeText(requireContext(), "Could not load category list. Search functionality may be limited.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Error loading tags/categories", e)
                Toast.makeText(requireContext(), "Error initializing category filter. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        stationAdapter = StationAdapter(
            requireContext(),
            onPlayClicked = { station ->
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                }
                activity?.startService(serviceIntent)
                // Also add to history via ViewModel
                stationViewModel.addStationToHistory(station)
            },
            onFavoriteToggle = { station ->
                stationViewModel.toggleFavoriteStatus(station)
                // The observer for favoriteStationsFlow should ideally update the list
                // and re-submit, or we need to manually update the item's state.
                // For now, the adapter's item will be rebound if list is resubmitted.
            }
        )
        recyclerViewStations.layoutManager = LinearLayoutManager(context)
        recyclerViewStations.adapter = stationAdapter
    }

    private fun observeFavoriteChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            favoritesViewModel.favoriteStations.collect { favoriteStations ->
                val favoriteIds = favoriteStations.map { it.id }.toSet()
                currentFavoritesSet = favoriteIds // Ajouter cette ligne

                // La logique suivante pour mettre à jour currentStationList et l'adapter est déjà là
                // et devrait utiliser 'favoriteIds' (qui est la même chose que currentFavoritesSet à ce point)
                // On peut la laisser telle quelle ou la faire utiliser currentFavoritesSet pour la cohérence
                // Pour l'instant, laissons-la utiliser 'favoriteIds' car c'est local à ce scope.
                // La principale utilité de currentFavoritesSet est pour performSearch.
                currentStationList = currentStationList.map { station ->
                    station.copy(isFavorite = favoriteIds.contains(station.id))
                }
                stationAdapter.submitList(currentStationList.toList())
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchJob?.cancel() // Cancel previous job
                performSearch(query)
                searchView.clearFocus() // Hide keyboard
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel() // Cancel previous job
                // Debounce search: wait for user to stop typing for a bit
                if (!newText.isNullOrBlank() && newText.length >= 2) { // Minimum characters to search
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(500) // Debounce delay
                        performSearch(newText)
                    }
                } else if (newText.isNullOrBlank()){
                    // Clear results if search text is empty
                     stationAdapter.submitList(emptyList())
                     currentStationList = emptyList()
                     showSearchMessageState("Search for radio stations above.", R.drawable.ic_search)
                }
                return true
            }
        })
    }

    private fun performSearch(nameQueryFromSearchView: String?) {
        showLoading(true)
        // tvError.visibility = View.GONE // Handled by showLoading

        val selectedCountryName = actvCountryDropdown.text.toString()
        val countryQueryValue = if (selectedCountryName.isNotBlank() && selectedCountryName != anyCountryString) selectedCountryName else null

        val selectedCategoryName = actvCategoryDropdown.text.toString()
        val categoryQueryValue = if (selectedCategoryName.isNotBlank() && selectedCategoryName != anyCategoryString) selectedCategoryName else null

        val nameQuery = nameQueryFromSearchView?.trim()?.takeIf { it.isNotEmpty() }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SearchFragment", "Performing search with Name: $nameQuery, Country: $countryQueryValue, Category: $categoryQueryValue")

                val response = apiService.searchStations(
                    name = nameQuery,
                    country = countryQueryValue,
                    tag = categoryQueryValue,
                    limit = 50,
                    hideBroken = true // Keep filtering out broken stations
                )
                if (response.isSuccessful) {
                    val apiStations = response.body() ?: emptyList()
                    // Map to domain and then merge favorite status
                    val favoriteIdsToUse = currentFavoritesSet

                    currentStationList = apiStations.mapNotNull { it.toDomain() }.map { station ->
                        station.copy(isFavorite = favoriteIdsToUse.contains(station.id))
                    }

                    if (currentStationList.isNotEmpty()) {
                        stationAdapter.submitList(currentStationList.toList()) // Submit new list
                        recyclerViewStations.visibility = View.VISIBLE
                        layoutSearchMessageState.visibility = View.GONE // Hide message state
                    } else {
                        stationAdapter.submitList(emptyList())
                        val activeFilters = mutableListOf<String>()
                        if (nameQuery != null) activeFilters.add("Name: '$nameQuery'")
                        if (countryQueryValue != null) activeFilters.add("Country: '$countryQueryValue'")
                        if (categoryQueryValue != null) activeFilters.add("Category: '$categoryQueryValue'")
                        val errorMsg = if (activeFilters.isNotEmpty()) "No stations found for ${activeFilters.joinToString()}" else "No stations found."
                        showSearchMessageState(errorMsg, R.drawable.ic_radio_placeholder) // Or ic_no_results
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SearchFragment", "API Error: ${response.code()} - ${response.message()} - Body: $errorBody")
                    val errorMsg = when (response.code()) {
                        404 -> "Could not find stations matching your request (Error 404)."
                        500, 502, 503, 504 -> "The station server seems to be having trouble. Please try again later."
                        else -> "Could not fetch stations. Please try again. (Code: ${response.code()})"
                    }
                    showSearchMessageState(errorMsg, R.drawable.ic_error_outline) // Or a generic error icon
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Network/Conversion Error: ${e.message}", e)
                showSearchMessageState("A network error occurred. Please check your connection and try again.", R.drawable.ic_error_outline)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            layoutSearchMessageState.visibility = View.GONE
            recyclerViewStations.visibility = View.GONE
        }
    }

    private fun showSearchMessageState(message: String, iconResId: Int? = null) {
        showLoading(false)
        recyclerViewStations.visibility = View.GONE
        layoutSearchMessageState.visibility = View.VISIBLE
        tvSearchMessageText.text = message
        if (iconResId != null) {
            ivSearchMessageIcon.setImageResource(iconResId)
            ivSearchMessageIcon.visibility = View.VISIBLE
        } else {
            ivSearchMessageIcon.visibility = View.GONE // Or set a default
        }
    }

    // Removed showEmptyState as it's merged into showSearchMessageState

    override fun onDestroyView() {
        searchJob?.cancel() // Cancel any running search when view is destroyed
        super.onDestroyView()
    }
}
