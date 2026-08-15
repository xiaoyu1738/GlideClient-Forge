package me.eldodebug.soar.management.event;

import me.eldodebug.soar.Glide;

public abstract class Event {

	private boolean cancelled;

	public Event call() {
		this.cancelled = false;
		Event.call(this);
		return this;
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	private static void call(Event event) {
		EventManager eventManager = Glide.getInstance().getEventManager();
		if (eventManager != null) {
			eventManager.dispatch(event);
		}
	}
}
