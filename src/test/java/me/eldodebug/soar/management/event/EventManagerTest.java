package me.eldodebug.soar.management.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventManagerTest {

    @Test
    public void duplicateRegistrationIsIgnored() {
        EventManager manager = new EventManager();
        Listener listener = new Listener();

        manager.register(listener);
        manager.register(listener);

        assertEquals(1, manager.get(FirstEvent.class).size());
        assertEquals(1, manager.get(SecondEvent.class).size());
    }

    @Test
    public void filteredRegistrationOnlyAddsRequestedEvent() {
        EventManager manager = new EventManager();
        Listener listener = new Listener();

        manager.register(listener, FirstEvent.class);

        assertEquals(1, manager.get(FirstEvent.class).size());
        assertNull(manager.get(SecondEvent.class));
    }

    @Test
    public void unregisterRemovesEveryHandlerFromTheSource() {
        EventManager manager = new EventManager();
        Listener listener = new Listener();

        manager.register(listener);
        manager.unregister(listener);

        assertNull(manager.get(FirstEvent.class));
        assertNull(manager.get(SecondEvent.class));
    }

    @Test
    public void listenerRemovalDuringDispatchDoesNotSkipOtherHandlers() {
        EventManager manager = new EventManager();
        RemovingListener removing = new RemovingListener(manager);
        CountingListener counting = new CountingListener();
        manager.register(removing);
        manager.register(counting);

        manager.dispatch(new FirstEvent());

        assertEquals(1, removing.calls);
        assertEquals(1, counting.calls);
        assertEquals(1, manager.get(FirstEvent.class).size());
    }

    @Test
    public void failingListenerIsDisabledWithoutStoppingLaterHandlers() {
        EventManager manager = new EventManager();
        FailingListener failing = new FailingListener();
        CountingListener counting = new CountingListener();
        manager.register(failing);
        manager.register(counting);

        manager.dispatch(new FirstEvent());
        manager.dispatch(new FirstEvent());

        assertEquals(1, failing.calls);
        assertEquals(2, counting.calls);
        assertTrue(manager.get(FirstEvent.class).get(0).disabled);
    }

    private static final class Listener {
        @EventTarget
        private void onFirst(FirstEvent event) {
        }

        @EventTarget
        private void onSecond(SecondEvent event) {
        }
    }

    private static final class RemovingListener {
        private final EventManager manager;
        private int calls;

        private RemovingListener(EventManager manager) {
            this.manager = manager;
        }

        @EventTarget(Priority.FIRST)
        private void onFirst(FirstEvent event) {
            calls++;
            manager.unregister(this);
        }
    }

    private static final class CountingListener {
        private int calls;

        @EventTarget(Priority.SECOND)
        private void onFirst(FirstEvent event) {
            calls++;
        }
    }

    private static final class FailingListener {
        private int calls;

        @EventTarget(Priority.FIRST)
        private void onFirst(FirstEvent event) {
            calls++;
            throw new IllegalStateException("expected test failure");
        }
    }

    private static final class FirstEvent extends Event {
    }

    private static final class SecondEvent extends Event {
    }
}
