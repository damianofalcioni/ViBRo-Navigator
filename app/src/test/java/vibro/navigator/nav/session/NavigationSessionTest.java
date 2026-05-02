package vibro.navigator.nav.session;


import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationSessionTest {

    @Test
    public void buildState_marksPausedSessionsAndClearsPauseStateOnResume() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        ));

        assertTrue(session.start(context, 0L));
        assertTrue(session.pause());

        NavState pausedState = session.buildState(
                context,
                NavState.NO_DEADLINE,
                0L,
                null,
                null,
                null
        );

        assertTrue(pausedState.pauseStatus.paused);
        assertTrue(pausedState.routeStatus.progress.detailBlock.contains(context.getString(R.string.nav_paused_notice)));
        assertTrue(session.resume());

        NavState resumedState = session.buildState(
                context,
                NavState.NO_DEADLINE,
                0L,
                null,
                null,
                null
        );

        assertFalse(resumedState.pauseStatus.paused);
    }
}
