package me.eldodebug.soar.management.event;

import java.lang.reflect.Method;

public class Data {

	public final Object source;
	public final Method target;
	public final byte priority;
	public volatile boolean disabled;

	public Data(Object source, Method target, byte priority) {
		this.source = source;
		this.target = target;
		this.priority = priority;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Data)) {
			return false;
		}

		Data other = (Data) object;
		return source == other.source && target.equals(other.target);
	}

	@Override
	public int hashCode() {
		return 31 * System.identityHashCode(source) + target.hashCode();
	}
}
