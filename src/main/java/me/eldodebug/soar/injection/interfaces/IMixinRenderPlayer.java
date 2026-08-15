package me.eldodebug.soar.injection.interfaces;

import me.eldodebug.soar.management.mods.impl.skin3d.layers.BodyLayerFeatureRenderer;
import me.eldodebug.soar.management.mods.impl.skin3d.layers.HeadLayerFeatureRenderer;

public interface IMixinRenderPlayer {
	public boolean glide$hasThinArms();
	public HeadLayerFeatureRenderer glide$getHeadLayer();
	public BodyLayerFeatureRenderer glide$getBodyLayer();
}
