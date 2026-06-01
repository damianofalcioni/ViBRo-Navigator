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
import java.util.List;
import java.util.concurrent.Future;

final class PoiSuggestionSearchController {

    private static final int MAX_SUGGESTIONS = 10;
    private static final int SEARCH_DELAY_MS = 300;
    private static final int MIN_ONLINE_QUERY_LENGTH = 4;

    interface Presenter {
        void showHistory();

        void showSuggestions(
                @NonNull List<PoiSuggestion> suggestions,
                @NonNull String popupReason
        );

        void clearSuggestionsAndDismiss();
    }

    private final TaskScheduler mainThreadScheduler;
    private final PoiHistoryStore history;
    private final PoiSearchClient searchClient;
    private final String logTag;
    private final Presenter presenter;

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
        this.mainThreadScheduler = mainThreadScheduler;
        this.history = history;
        this.searchClient = searchClient;
        this.logTag = logTag;
        this.presenter = presenter;
    }

    void scheduleSearch(@NonNull String raw) {
        cancelPendingSearch();

        String query = raw.trim();
        if (showCoordinateSuggestion(query) || showHistoryMatches(query) || handleShortQuery(query)) {
            return;
        }

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

    private boolean showHistoryMatches(@NonNull String query) {
        List<PoiSuggestion> historySuggestions = matchingHistorySuggestions(query);
        if (historySuggestions.isEmpty()) {
            return false;
        }

        cancelInFlightSearch();
        AppLogger.d(logTag, "Showing matching history query=" + query
                + " items=" + historySuggestions.size());
        presenter.showSuggestions(historySuggestions, "history-search-results");
        return true;
    }

    private boolean handleShortQuery(@NonNull String query) {
        if (query.length() >= MIN_ONLINE_QUERY_LENGTH) {
            return false;
        }

        cancelInFlightSearch();
        if (query.isEmpty()) {
            AppLogger.d(logTag, "Empty query, showing history");
            presenter.showHistory();
        } else {
            AppLogger.d(logTag, "Query too short for search query=" + query);
            presenter.clearSuggestionsAndDismiss();
        }
        return true;
    }

    private void runSearch(@NonNull String query) {
        cancelInFlightSearch();
        int generation = ++searchGeneration;
        inFlight = PoiSearchDispatcher.submit(() -> {
            try {
                List<PoiSuggestion> suggestions = performSearch(query);
                mainThreadScheduler.post(() -> handleSearchSuccess(query, generation, suggestions));
            } catch (IOException e) {
                AppLogger.e(logTag, "Search failed query=" + query, e);
                mainThreadScheduler.post(() -> handleSearchFailure(generation));
            }
        });
    }

    @NonNull
    private List<PoiSuggestion> performSearch(@NonNull String query) throws IOException {
        AppLogger.i(logTag, "Running search query=" + query);
        List<Poi> results = searchClient.search(query, MAX_SUGGESTIONS);
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
        AppLogger.i(logTag, "Search finished query=" + query + " suggestions=" + suggestions.size());
        presenter.showSuggestions(suggestions, "search-results");
    }

    private void handleSearchFailure(int generation) {
        if (generation == searchGeneration) {
            presenter.showSuggestions(new ArrayList<>(), "search-failure");
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
        for (Poi poi : history.search(query, MAX_SUGGESTIONS)) {
            items.add(new PoiSuggestion(poi, true));
        }
        return items;
    }
}
