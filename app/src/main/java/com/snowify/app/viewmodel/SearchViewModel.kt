package com.snowify.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.local.dao.SearchHistoryDao
import com.snowify.app.data.local.entity.SearchHistoryEntity
import com.snowify.app.data.model.SearchResults
import com.snowify.app.data.repository.SongRepository
import com.snowify.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val searchHistoryDao: SearchHistoryDao,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val searchHistory: Flow<List<String>> = searchHistoryDao.getSearchHistory()
        .map { entities -> entities.map { it.query } }

    private val _searchResults = MutableStateFlow(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = SearchResults()
                        _isSearching.value = false
                    } else {
                        executeSearch(query)
                    }
                }
        }
    }

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                Log.d("SearchVM", "Searching: '$query'")
                val results = songRepository.search(query, true)
                Log.d("SearchVM", "Results: ${results.songs.size} songs, ${results.artists.size} artists, ${results.albums.size} albums")
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("SearchVM", "Search failed for '$query'", e)
                _searchResults.value = SearchResults()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun setQuery(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = SearchResults()
            _isSearching.value = false
        }
    }

    fun clearQuery() {
        searchQuery.value = ""
        _searchResults.value = SearchResults()
        _isSearching.value = false
    }


    fun saveToHistory(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchHistoryDao.insertSearchQuery(SearchHistoryEntity(query))
        }
    }

    fun deleteFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryDao.deleteSearchQuery(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearHistory()
        }
    }
}
