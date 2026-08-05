package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;

public class AnimationsMod extends Mod {

	private static AnimationsMod instance;
	
	private BooleanSetting blockHitSetting = new BooleanSetting(TranslateText.BLOCK_HIT, this, true);
	private BooleanSetting pushingSetting = new BooleanSetting(TranslateText.PUSHING, this, true);
	private BooleanSetting pushingParticleSetting = new BooleanSetting(TranslateText.PUSHING_PARTICLES, this, true);
	private BooleanSetting sneakSetting = new BooleanSetting(TranslateText.SNEAK, this, true);
	private BooleanSetting smoothSneakSetting = new BooleanSetting(TranslateText.SNEAKSMOOTH, this, false);
	private NumberSetting smoothSneakSpeedSetting = new NumberSetting(TranslateText.SMOOTH_SPEED, this, 6, 0.5, 20, false);
	private BooleanSetting healthSetting = new BooleanSetting(TranslateText.HEALTH, this, true);
	
	private BooleanSetting armorDamageSetting = new BooleanSetting(TranslateText.ARMOR_DAMAGE, this, false);
	private BooleanSetting itemSwitchSetting = new BooleanSetting(TranslateText.ITEM_SWITCH, this, false);
	private BooleanSetting rodSetting = new BooleanSetting(TranslateText.ROD, this, false);

	public AnimationsMod() {
		super(TranslateText.OLD_ANIMATION, TranslateText.OLD_ANIMATION_DESCRIPTION, ModCategory.RENDER, "oldoam1.7smoothsneak");
		
		instance = this;
	}

	public static AnimationsMod getInstance() {
		return instance;
	}

	public BooleanSetting getBlockHitSetting() {
		return blockHitSetting;
	}

	public BooleanSetting getPushingSetting() {
		return pushingSetting;
	}

	public BooleanSetting getPushingParticleSetting() {
		return pushingParticleSetting;
	}

	public BooleanSetting getSneakSetting() {
		return sneakSetting;
	}

	public BooleanSetting getSmoothSneakSetting() { return smoothSneakSetting; }

	public float getSmoothSneakSpeedSetting() { return smoothSneakSpeedSetting.getValueFloat(); }

	public BooleanSetting getHealthSetting() {
		return healthSetting;
	}

	public BooleanSetting getArmorDamageSetting() {
		return armorDamageSetting;
	}

	public BooleanSetting getItemSwitchSetting() {
		return itemSwitchSetting;
	}

	public BooleanSetting getRodSetting() {
		return rodSetting;
	}
}
