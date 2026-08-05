package me.eldodebug.soar.management.security;

import me.eldodebug.soar.Glide;

public class SecurityFeature {

	public SecurityFeature() {
		Glide.getInstance().getEventManager().register(this);
	}
}
