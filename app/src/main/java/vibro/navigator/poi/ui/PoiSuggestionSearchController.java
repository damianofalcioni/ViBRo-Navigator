package vibro.navigator.poi.ui;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.logging.AppLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

final class PoiSuggestionSearchController {

    private static final int MAX_HISTORY_SUGGESTIONS = 10;
    private static final int MAX_ONLINE_SUGGESTIONS = 10;
    private static final int SEARCH_DELAY_MS = 300;
    private static final int MIN_ONLINE_QUERY_LENGTH = 3;

    interface Presenter {
        void showHistory();

        void showSuggestions(
                @NonNull List<PoiSuggestion> suggestions,
                @NonNull String popupReason
        );

        void clearSuggestionsAndDismiss();
    }

    interface SearchDispatcher {
        @NonNull
        Future<?> submit(@NonNull Runnable runnable);
    }

    private final TaskScheduler mainThreadScheduler;
    private final PoiHistoryStore history;
    private final PoiSearchClient searchClient;
    private final String logTag;
    private final Presenter presenter;
    private final SearchDispatcher searchDispatcher;

    private Future<?> inFlight;
    private Runnable pendingSearch;
    private int searchGeneration;

    PoiSuggestionSearchController(
            @NonNull TaskScheduler mainThreadScheduler,
            @NonNull PoiHistoryStore history,
            @NonNull PoiSearchClient searchClient,
            @NonNull String logTag,
            @NonNull Presenter presenter
    ) {
        this(
                mainThreadScheduler,
                history,
                searchClient,
                logTag,
                presenter,
                PoiSearchDispatcher::submit
        );
    }

    PoiSuggestionSearchController(
            @NonNull TaskScheduler mainThreadScheduler,
            @NonNull PoiHistoryStore history,
            @NonNull PoiSearchClient searchClient,
            @NonNull String logTag,
            @NonNull Presenter presenter,
            @NonNull SearchDispatcher searchDispatcher
    ) {
        this.mainThreadScheduler = mainThreadScheduler;
        this.history = history;
        this.searchClient = searchClient;
        this.logTag = logTag;
        this.presenter = presenter;
        this.searchDispatcher = searchDispatcher;
    }

    void scheduleSearch(@NonNull String raw) {
        cancelPendingSearch();

        String query = raw.trim();
        if (showCoordinateSuggestion(query) || handleEmptyQuery(query)) {
            return;
        }

        List<PoiSuggestion> historySuggestions = matchingHistorySuggestions(query);
        if (handleShortQuery(query, historySuggestions)) {
            return;
        }

        cancelInFlightSearch();
        showTypedQuerySuggestions(query, historySuggestions);
        pendingSearch = () -> runSearch(query);
        AppLogger.d(logTag, "Scheduling search query=" + query);
        mainThreadScheduler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
    }

    void cancelPendingSearch() {
        if (pendingSearch != null) {
            mainThreadScheduler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    void cancelInFlightSearch() {
        searchGeneration++;
        if (inFlight != null) {
            inFlight.cancel(true);
            inFlight = null;
            AppLogger.d(logTag, "Cancelled in-flight search");
        }
    }

    private boolean showCoordinateSuggestion(@NonNull String query) {
        Poi coords = CoordinateParser.tryParse(query, query);
        if (coords == null) {
            return false;
        }

        cancelInFlightSearch();
        AppLogger.d(logTag, "Recognized direct coordinate entry query=" + query);
        presenter.showSuggestions(singleSuggestion(coords, false), "coordinate-entry");
        return true;
    }

    private boolean handleEmptyQuery(@NonNull String query) {
        if (!query.isEmpty()) {
            return false;
        }
        cancelInFlightSearch();
        AppLogger.d(logTag, "Empty query, showing history");
        presenter.showHistory();
        return true;
    }

    private boolean handleShortQuery(
            @NonNull String query,
            @NonNull List<PoiSuggestion> historySuggestions
    ) {
        if (query.length() >= MIN_ONLINE_QUERY_LENGTH) {
            return false;
        }

        cancelInFlightSearch();
        if (historySuggestions.isEmpty()) {
            AppLogger.d(logTag, "Query too short for search query=" + query);
            presenter.clearSuggestionsAndDismiss();
        } else {
            showTypedQuerySuggestions(query, historySuggestions);
        }
        return true;
    }

    private void showTypedQuerySuggestions(
            @NonNull String query,
            @NonNull List<PoiSuggestion> historySuggestions
    ) {
        List<PoiSuggestion> suggestions = suggestionsWithExternalMapSearch(query, historySuggestions);
        if (suggestions.isEmpty()) {
            presenter.clearSuggestionsAndDismiss();
            return;
        }
        AppLogger.d(logTag, "Showing typed query suggestions query=" + query
                + " items=" + suggestions.size());
        presenter.showSuggestions(suggestions, "history-search-results");
    }

    private void runSearch(@NonNull String query) {
        pendingSearch = null;
        int generation = ++searchGeneration;
        inFlight = searchDispatcher.submit(() -> {
            try {
                List<PoiSuggestion> suggestions = performSearch(query);
                mainThreadScheduler.post(() -> handleSearchSuccess(query, generation, suggestions));
            } catch (IOException e) {
                AppLogger.e(logTag, "Search failed query=" + query, e);
                mainThreadScheduler.post(() -> handleSearchFailure(query, generation));
            }
        });
    }

    @NonNull
    private List<PoiSuggestion> performSearch(@NonNull String query) throws IOException {
        AppLogger.i(logTag, "Running search query=" + query);
        List<Poi> results = searchClient.search(query, MAX_ONLINE_SUGGESTIONS);
        List<PoiSuggestion> suggestions = new ArrayList<>();
        for (Poi poi : results) {
            suggestions.add(new PoiSuggestion(poi, false));
        }
        return suggestions;
    }

    private void handleSearchSuccess(
            @NonNull String query,
            int generation,
            @NonNull List<PoiSuggestion> suggestions
    ) {
        if (generation != searchGeneration) {
            AppLogger.d(logTag, "Discarding stale search result query=" + query);
            return;
        }
        inFlight = null;
        List<PoiSuggestion> combined = suggestionsWithCurrentHistory(query, suggestions);
        AppLogger.i(logTag, "Search finished query=" + query + " suggestions=" + combined.size());
        presenter.showSuggestions(combined, "search-results");
    }

    private void handleSearchFailure(@NonNull String query, int generation) {
        if (generation == searchGeneration) {
            inFlight = null;
            showTypedQuerySuggestions(query, matchingHistorySuggestions(query));
        }
    }

    @NonNull
    private static List<PoiSuggestion> singleSuggestion(@NonNull Poi poi, boolean deletable) {
        List<PoiSuggestion> items = new ArrayList<>();
        items.add(new PoiSuggestion(poi, deletable));
        return items;
    }

    @NonNull
    private List<PoiSuggestion> matchingHistorySuggestions(@NonNull String query) {
        List<PoiSuggestion> items = new ArrayList<>();
        for (Poi poi : history.search(query, MAX_HISTORY_SUGGESTIONS)) {
            items.add(new PoiSuggestion(poi, true));
        }
        return items;
    }

    @NonNull
    private List<PoiSuggestion> suggestionsWithCurrentHistory(
            @NonNull String query,
            @NonNull List<PoiSuggestion> onlineSuggestions
    ) {
        List<PoiSuggestion> combined = matchingHistorySuggestions(query);
        Set<String> knownKeys = knownSuggestionKeys(combined);
        for (PoiSuggestion suggestion : onlineSuggestions) {
            if (knownKeys.add(suggestion.poi().stableKey())) {
                combined.add(suggestion);
            }
        }
        return suggestionsWithExternalMapSearch(query, combined);
    }

    @NonNull
    private static Set<String> knownSuggestionKeys(@NonNull List<PoiSuggestion> suggestions) {
        Set<String> keys = new HashSet<>();
        for (PoiSuggestion suggestion : suggestions) {
            if (!suggestion.isExternalMapSearch()) {
                keys.add(suggestion.poi().stableKey());
            }
        }
        return keys;
    }

    @NonNull
    private static List<PoiSuggestion> suggestionsWithExternalMapSearch(
            @NonNull String query,
            @NonNull List<PoiSuggestion> suggestions
    ) {
        if (!shouldOfferExternalMapSearch(query, suggestions)) {
            return suggestions;
        }
        List<PoiSuggestion> withExternalSearch = new ArrayList<>(suggestions);
        withExternalSearch.add(PoiSuggestion.externalMapSearch(query));
        return withExternalSearch;
    }

    private static boolean shouldOfferExternalMapSearch(
            @NonNull String query,
            @NonNull List<PoiSuggestion> suggestions
    ) {
        return query.trim().length() >= MIN_ONLINE_QUERY_LENGTH || !suggestions.isEmpty();
    }
}
