package me.eldodebug.soar.management.event;

import java.lang.reflect.InvocationTargetException;

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
		
		Glide instance = Glide.getInstance();
		EventManager eventManager = instance.getEventManager();
		ArrayHelper<Data> dataList = eventManager.get(event.getClass());
		
		if (dataList != null) {
			for (Data data : dataList) {
				try {
					data.target.invoke(data.source, event);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
