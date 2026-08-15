package me.eldodebug.soar.management.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import me.eldodebug.soar.logger.GlideLogger;

public class EventManager {

	private final Map<Class<?>, ArrayHelper<Data>> registryMap =
			new HashMap<Class<?>, ArrayHelper<Data>>();

	public synchronized void register(Object o) {

		for (Method method : o.getClass().getDeclaredMethods()) {
			if (!isMethodBad(method)) {
				register(method, o);
			}
		}
	}

	public synchronized void register(Object o, Class<? extends Event> clazz) {
		for (Method method : o.getClass().getDeclaredMethods()) {
			if (!isMethodBad(method, clazz)) {
				register(method, o);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void register(Method method, Object o) {
		Class<?> clazz = method.getParameterTypes()[0];
		final Data methodData = new Data(o, method, method.getAnnotation(EventTarget.class).value());

		if (!methodData.target.isAccessible()) {
			methodData.target.setAccessible(true);
		}

		if (registryMap.containsKey(clazz)) {
			if (!registryMap.get(clazz).contains(methodData)) {
				registryMap.get(clazz).add(methodData);
				sortListValue((Class<? extends Event>) clazz);
			}
		} else {
			registryMap.put((Class<? extends Event>) clazz, new ArrayHelper<Data>() {
				{
					this.add(methodData);
				}
			});
		}
	}

	public synchronized void unregister(final Object o) {

		for (ArrayHelper<Data> flexibleArray : registryMap.values()) {
			for (int i = flexibleArray.size() - 1; i >= 0; i--) {
				Data methodData = flexibleArray.get(i);
				if (methodData.source == o) {
					flexibleArray.remove(methodData);
				}
			}
		}

		cleanMap(true);
	}

	public synchronized void unregister(final Object o, final Class<? extends Event> clazz) {
		if (registryMap.containsKey(clazz)) {
			ArrayHelper<Data> handlers = registryMap.get(clazz);
			for (int i = handlers.size() - 1; i >= 0; i--) {
				Data methodData = handlers.get(i);
				if (methodData.source == o) {
					handlers.remove(methodData);
				}
			}

			cleanMap(true);
		}
	}

	public synchronized void cleanMap(boolean b) {

		Iterator<Entry<Class<?>, ArrayHelper<Data>>> iterator = registryMap.entrySet().iterator();

		while (iterator.hasNext()) {
			if (!b || iterator.next().getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}

	public synchronized void removeEnty(Class<? extends Event> clazz) {

		Iterator<Entry<Class<?>, ArrayHelper<Data>>> iterator = registryMap.entrySet().iterator();

		while (iterator.hasNext()) {
			if (iterator.next().getKey().equals(clazz)) {
				iterator.remove();
				break;
			}
		}
	}

	private void sortListValue(Class<? extends Event> clazz) {

		ArrayHelper<Data> flexibleArray = new ArrayHelper<Data>();

		for (byte b : Priority.VALUE_ARRAY) {
			for (Data methodData : registryMap.get(clazz)) {
				if (methodData.priority == b) {
					flexibleArray.add(methodData);
				}
			}
		}

		registryMap.put(clazz, flexibleArray);
	}

	private boolean isMethodBad(final Method method) {
		return method.getParameterTypes().length != 1
				|| !Event.class.isAssignableFrom(method.getParameterTypes()[0])
				|| !method.isAnnotationPresent(EventTarget.class);
	}

	private boolean isMethodBad(Method method, Class<? extends Event> clazz) {
		return isMethodBad(method) || !method.getParameterTypes()[0].equals(clazz);
	}

	public synchronized ArrayHelper<Data> get(final Class<? extends Event> clazz) {
		return registryMap.get(clazz);
	}

	public synchronized void shutdown() {
		registryMap.clear();
	}

	void dispatch(Event event) {
		Object[] handlers = snapshot(event.getClass());
		for (Object handler : handlers) {
			Data data = (Data) handler;
			if (data.disabled) {
				continue;
			}

			try {
				data.target.invoke(data.source, event);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause() == null ? e : e.getCause();
				rethrowFatal(cause);
				disableHandler(data, cause);
			} catch (Throwable throwable) {
				rethrowFatal(throwable);
				disableHandler(data, throwable);
			}
		}
	}

	private synchronized Object[] snapshot(Class<? extends Event> eventClass) {
		ArrayHelper<Data> handlers = registryMap.get(eventClass);
		return handlers == null ? new Object[0] : handlers.snapshot();
	}

	private static void rethrowFatal(Throwable throwable) {
		if (throwable instanceof VirtualMachineError) {
			throw (VirtualMachineError) throwable;
		}
		if (throwable instanceof ThreadDeath) {
			throw (ThreadDeath) throwable;
		}
	}

	private static void disableHandler(Data data, Throwable throwable) {
		data.disabled = true;
		GlideLogger.getLogger().error(
				"[GC/ERROR] Disabled failing event handler "
						+ data.source.getClass().getName() + "#" + data.target.getName(),
				throwable);
	}

}
