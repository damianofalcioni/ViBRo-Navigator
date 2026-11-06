package vibro.navigator.auto;

import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.poi.Poi;

/** Host-only destination UI, including fallbacks for the declared Car API 1 minimum. */
final class ViBRoCarDestinationTemplates {
    interface Actions {
        void cancel();
        void retry();
        void select(Poi poi);
    }

    private final CarContext context;
    ViBRoCarDestinationTemplates(CarContext context) {
        this.context = context;
    }

    Template build(List<Poi> results, boolean loading, int message, boolean retryable, Actions actions) {
        Action cancel = new Action.Builder().setTitle(text(R.string.action_cancel))
                .setOnClickListener(actions::cancel).build();
        return results.isEmpty() ? messageTemplate(cancel, loading, message, retryable, actions)
                : resultTemplate(results, cancel, actions);
    }

    @SuppressWarnings("deprecation") // Header setters support the retained Car API 1 hosts.
    private Template resultTemplate(List<Poi> results, Action cancel, Actions actions) {
        boolean hasActionStrip = context.getCarAppApiLevel() >= 2;
        ItemList.Builder list = new ItemList.Builder();
        int count = Math.min(results.size(), hasActionStrip ? 5 : 4);
        for (Poi poi : results.subList(0, count)) {
            list.addItem(new Row.Builder().setTitle(poi.displayLabel())
                    .setOnClickListener(() -> actions.select(poi)).build());
        }
        if (!hasActionStrip) {
            list.addItem(new Row.Builder().setTitle(text(R.string.action_cancel))
                    .setOnClickListener(actions::cancel).build());
        }
        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setTitle(text(R.string.hint_search_destination)).setHeaderAction(Action.APP_ICON)
                .setSingleList(list.build());
        if (hasActionStrip) {
            builder.setActionStrip(new ActionStrip.Builder().addAction(cancel).build());
        }
        return builder.build();
    }

    @SuppressWarnings("deprecation") // Header setters support the retained Car API 1 hosts.
    private Template messageTemplate(Action cancel, boolean loading, int message, boolean retryable, Actions actions) {
        MessageTemplate.Builder builder = new MessageTemplate.Builder(
                text(loading ? R.string.msg_map_poi_loading : message))
                .setTitle(text(R.string.auto_title)).setHeaderAction(Action.APP_ICON).addAction(cancel);
        if (context.getCarAppApiLevel() >= 2) { builder.setLoading(loading); }
        if (!loading && retryable) {
            builder.addAction(new Action.Builder().setTitle(text(R.string.auto_retry))
                    .setOnClickListener(actions::retry).build());
        }
        return builder.build();
    }

    private String text(int id) { return AndroidAppLanguage.getString(context, id); }
}
