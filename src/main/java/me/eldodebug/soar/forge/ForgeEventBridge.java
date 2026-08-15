package me.eldodebug.soar.forge;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiGameMenu;
import me.eldodebug.soar.hooks.IngameOverlayRenderer;
import me.eldodebug.soar.hooks.GuiChatHook;
import me.eldodebug.soar.injection.interfaces.IMixinGuiScreen;
import me.eldodebug.soar.injection.interfaces.IMixinMinecraft;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.event.impl.EventAttackEntity;
import me.eldodebug.soar.management.event.impl.EventBlockHighlightRender;
import me.eldodebug.soar.management.event.impl.EventFovUpdate;
import me.eldodebug.soar.management.event.impl.EventFireOverlay;
import me.eldodebug.soar.management.event.impl.EventJump;
import me.eldodebug.soar.management.event.impl.EventKey;
import me.eldodebug.soar.management.event.impl.EventLeaveServer;
import me.eldodebug.soar.management.event.impl.EventLoadWorld;
import me.eldodebug.soar.management.event.impl.EventPlaySound;
import me.eldodebug.soar.management.event.impl.EventPreRenderTick;
import me.eldodebug.soar.management.event.impl.EventLivingUpdate;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.event.impl.EventRenderCrosshair;
import me.eldodebug.soar.management.event.impl.EventRendererLivingEntity;
import me.eldodebug.soar.management.event.impl.EventRenderPlayer;
import me.eldodebug.soar.management.event.impl.EventRenderPlayerStats;
import me.eldodebug.soar.management.event.impl.EventRenderPumpkinOverlay;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.event.impl.EventClickMouse;
import me.eldodebug.soar.management.event.impl.EventScrollMouse;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.event.impl.EventWaterOverlay;
import me.eldodebug.soar.management.mods.impl.BossHealthMod;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import me.eldodebug.soar.management.mods.impl.ChatTranslateMod;
import me.eldodebug.soar.management.mods.impl.HitDelayFixMod;
import me.eldodebug.soar.management.mods.impl.ModernHotbarMod;
import me.eldodebug.soar.management.mods.impl.SoundSubtitlesMod;
import me.eldodebug.soar.utils.Sound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Adapts Forge events to Glide's existing internal event bus while the client
 * is migrated subsystem by subsystem.
 */
public final class ForgeEventBridge {

    private boolean modernHotbarRendered;
	private boolean blockHighlightHandled;
	private boolean guiCloseMaintenanceAvailable = true;
	private boolean guiClickEffectsAvailable = true;
	private boolean guiClickSoundAvailable = true;
	private boolean chatTranslateGuiAvailable = true;
	private boolean characterInputFixAvailable = true;
	private boolean hitDelayFixAvailable = true;
	private boolean soundSubtitlesAvailable = true;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Glide glide = Glide.getInstance();
            if (!glide.isStarted()) {
                glide.start();
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft.currentScreen != null
						&& minecraft.currentScreen.getClass() == GuiMainMenu.class) {
                    minecraft.displayGuiScreen(glide.getMainMenu());
                }
            }
        } else if (Glide.getInstance().isStarted()) {
            new EventTick().call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START
                && Glide.getInstance().isStarted()
                && event.player == minecraft.thePlayer
                && event.player.worldObj != null
                && event.player.worldObj.isRemote) {
            new EventUpdate().call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (Glide.getInstance().isStarted()
                && event.entityPlayer == minecraft.thePlayer
                && event.entityPlayer.worldObj != null
                && event.entityPlayer.worldObj.isRemote
                && event.target.canAttackWithItem()) {
            new EventAttackEntity(event.target).call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingJump(LivingJumpEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (Glide.getInstance().isStarted()
                && event.entityLiving == minecraft.thePlayer
                && event.entityLiving.worldObj != null
                && event.entityLiving.worldObj.isRemote) {
            new EventJump().call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (Glide.getInstance().isStarted()
                && event.entityLiving.worldObj != null
                && event.entityLiving.worldObj.isRemote) {
            new EventLivingUpdate(event.entityLiving).call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFovUpdate(FOVUpdateEvent event) {
        if (!Glide.getInstance().isStarted()
                || !(event.entity instanceof AbstractClientPlayer)) {
            return;
        }

        EventFovUpdate glideFov = new EventFovUpdate(
                (AbstractClientPlayer) event.entity, event.newfov);
        glideFov.call();
        event.newfov = glideFov.getFov();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        try {
            if (!Glide.getInstance().isStarted()) {
                return;
            }

            new EventRender3D(event.partialTicks).call();

            Minecraft minecraft = Minecraft.getMinecraft();
            if (!blockHighlightHandled
                    && minecraft.getRenderViewEntity() != null
                    && minecraft.getRenderViewEntity().isInsideOfMaterial(Material.water)
                    && minecraft.objectMouseOver != null
                    && minecraft.objectMouseOver.typeOfHit
                            == MovingObjectPosition.MovingObjectType.BLOCK) {
                new EventBlockHighlightRender(
                        minecraft.objectMouseOver, event.partialTicks).call();
            }
        } finally {
            blockHighlightHandled = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        blockHighlightHandled = true;
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        EventBlockHighlightRender glideHighlight =
                new EventBlockHighlightRender(event.target, event.partialTicks);
        glideHighlight.call();
        if (glideHighlight.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        EventRenderPlayer glideRender = new EventRenderPlayer(
                event.entityPlayer, event.x, event.y, event.z,
                event.partialRenderTick);
        glideRender.call();
        if (glideRender.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        EventRendererLivingEntity glideRender = new EventRendererLivingEntity(
                (RendererLivingEntity<EntityLivingBase>) event.renderer,
                event.entity, event.x, event.y, event.z);
        glideRender.call();
        if (glideRender.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderBlockOverlay(RenderBlockOverlayEvent event) {
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.WATER) {
            EventWaterOverlay water = new EventWaterOverlay();
            water.call();
            if (water.isCancelled()) {
                event.setCanceled(true);
            }
        } else if (event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            EventFireOverlay fire = new EventFireOverlay();
            fire.call();
            if (fire.isCancelled()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMouseInput(MouseEvent event) {
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        boolean canceled = false;
        if (event.button >= 0) {
            EventClickMouse click = new EventClickMouse(event.button);
            click.call();
            canceled = click.isCancelled();
        }

        if (event.dwheel != 0) {
            EventScrollMouse scroll = new EventScrollMouse(event.dwheel);
            scroll.call();
            canceled |= scroll.isCancelled();
        }

        if (canceled) {
            event.setCanceled(true);
			return;
		}

		if (hitDelayFixAvailable && event.button == 0 && event.buttonstate) {
			try {
				Minecraft minecraft = Minecraft.getMinecraft();
				HitDelayFixMod hitDelayFix = HitDelayFixMod.getInstance();
				if (minecraft.currentScreen == null && hitDelayFix != null
						&& hitDelayFix.isToggled()) {
					((IMixinMinecraft) minecraft).glide$setLeftClickCounter(0);
				}
			} catch (Throwable throwable) {
				hitDelayFixAvailable = false;
				disableFeature("hit delay fix", throwable);
			}
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!Glide.getInstance().isStarted() || minecraft.currentScreen != null
                || !Keyboard.getEventKeyState()) {
            return;
        }

        int keyCode = Keyboard.getEventKey() == 0
                ? Keyboard.getEventCharacter() + 256
                : Keyboard.getEventKey();
        new EventKey(keyCode).call();
    }

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onGuiKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		if (!characterInputFixAvailable || Keyboard.getEventKeyState()
				|| Keyboard.getEventKey() != 0
				|| Keyboard.getEventCharacter() < ' ') {
			return;
		}

		try {
			((IMixinGuiScreen) event.gui).glide$invokeKeyTyped(
					Keyboard.getEventCharacter(), Keyboard.getEventKey());
		} catch (Throwable throwable) {
			characterInputFixAvailable = false;
			disableFeature("character-only GUI input", throwable);
		}
	}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        Glide glide = Glide.getInstance();
		Minecraft minecraft = Minecraft.getMinecraft();
		GuiScreen closingScreen = minecraft.currentScreen;

		if (guiCloseMaintenanceAvailable && closingScreen != null
				&& closingScreen != event.gui) {
			try {
				if (closingScreen instanceof GuiOptions) {
					minecraft.gameSettings.saveOptions();
				}
				if (closingScreen instanceof GuiLanguage && minecraft.ingameGUI != null) {
					minecraft.ingameGUI.getChatGUI().refreshChat();
				}
			} catch (Throwable throwable) {
				guiCloseMaintenanceAvailable = false;
				disableFeature("GUI close maintenance", throwable);
			}
		}

        if (glide.isStarted() && event.gui != null
				&& event.gui.getClass() == GuiIngameMenu.class) {
			event.gui = new GuiGameMenu();
		} else if (glide.isStarted() && event.gui != null
				&& event.gui.getClass() == GuiMainMenu.class) {
			event.gui = glide.getMainMenu();
        }
    }

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
		if (!Glide.getInstance().isStarted() || !chatTranslateGuiAvailable
				|| !(event.gui instanceof GuiChat)
				|| Mouse.getEventButton() < 0 || !Mouse.getEventButtonState()) {
			return;
		}

		try {
			ChatTranslateMod chatTranslate = ChatTranslateMod.getInstance();
			if (chatTranslate != null && chatTranslate.isToggled()) {
				Minecraft minecraft = Minecraft.getMinecraft();
				int mouseX = Mouse.getEventX() * event.gui.width / minecraft.displayWidth;
				int mouseY = event.gui.height
						- Mouse.getEventY() * event.gui.height / minecraft.displayHeight - 1;
				GuiChatHook.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
			}
		} catch (Throwable throwable) {
			chatTranslateGuiAvailable = false;
			disableFeature("chat translation controls", throwable);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Post event) {
		Glide glide = Glide.getInstance();
		if (!glide.isStarted() || Mouse.getEventButton() < 0
				|| !Mouse.getEventButtonState()) {
			return;
		}

		if (guiClickEffectsAvailable) {
			try {
				if (InternalSettingsMod.getInstance().getClickEffectsSetting().isToggled()) {
					Minecraft minecraft = Minecraft.getMinecraft();
					int mouseX = Mouse.getEventX() * event.gui.width / minecraft.displayWidth;
					int mouseY = event.gui.height
							- Mouse.getEventY() * event.gui.height / minecraft.displayHeight - 1;
					glide.getClickEffects().addClickEffect(mouseX, mouseY);
				}
			} catch (Throwable throwable) {
				guiClickEffectsAvailable = false;
				disableFeature("GUI click effects", throwable);
			}
		}

		if (guiClickSoundAvailable) {
			try {
				Sound.play("soar/audio/click.wav", true);
			} catch (Throwable throwable) {
				guiClickSoundAvailable = false;
				disableFeature("GUI click sound", throwable);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
		Glide glide = Glide.getInstance();
		if (!glide.isStarted()) {
			return;
		}

		if (chatTranslateGuiAvailable && event.gui instanceof GuiChat) {
			try {
				ChatTranslateMod chatTranslate = ChatTranslateMod.getInstance();
				if (chatTranslate != null && chatTranslate.isToggled()) {
					GuiChatHook.drawScreen(
							event.mouseX, event.mouseY, event.renderPartialTicks);
				}
			} catch (Throwable throwable) {
				chatTranslateGuiAvailable = false;
				disableFeature("chat translation controls", throwable);
			}
		}

		if (guiClickEffectsAvailable) {
			try {
				if (InternalSettingsMod.getInstance().getClickEffectsSetting().isToggled()) {
					glide.getClickEffects().drawClickEffects();
				}
			} catch (Throwable throwable) {
				guiClickEffectsAvailable = false;
				disableFeature("GUI click effects", throwable);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlaySound(PlaySoundEvent event) {
		if (!Glide.getInstance().isStarted() || event.result == null) {
			return;
		}

		ISound original = event.result;
		EventPlaySound glideSound = new EventPlaySound(
				event.name,
				original.getVolume(),
				original.getPitch(),
				original.getVolume(),
				original.getPitch());
		glideSound.call();

		if (Float.compare(glideSound.getVolume(), original.getVolume()) != 0
				|| Float.compare(glideSound.getPitch(), original.getPitch()) != 0) {
			event.result = new ModifiedSound(
					original, glideSound.getVolume(), glideSound.getPitch());
		}

		if (!soundSubtitlesAvailable) {
			return;
		}

		try {
			SoundSubtitlesMod subtitles = SoundSubtitlesMod.getInstance();
			if (subtitles != null && subtitles.isToggled()) {
				subtitles.soundPlay(event.result);
			}
		} catch (Throwable throwable) {
			soundSubtitlesAvailable = false;
			disableFeature("sound subtitles", throwable);
		}
	}

	private static final class ModifiedSound implements ISound {

		private final ISound delegate;
		private final float volume;
		private final float pitch;

		private ModifiedSound(ISound delegate, float volume, float pitch) {
			this.delegate = delegate;
			this.volume = volume;
			this.pitch = pitch;
		}

		@Override
		public ResourceLocation getSoundLocation() {
			return delegate.getSoundLocation();
		}

		@Override
		public boolean canRepeat() {
			return delegate.canRepeat();
		}

		@Override
		public int getRepeatDelay() {
			return delegate.getRepeatDelay();
		}

		@Override
		public float getVolume() {
			return volume;
		}

		@Override
		public float getPitch() {
			return pitch;
		}

		@Override
		public float getXPosF() {
			return delegate.getXPosF();
		}

		@Override
		public float getYPosF() {
			return delegate.getYPosF();
		}

		@Override
		public float getZPosF() {
			return delegate.getZPosF();
		}

		@Override
		public AttenuationType getAttenuationType() {
			return delegate.getAttenuationType();
		}
	}

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (Glide.getInstance().isStarted() && event.world.isRemote) {
            new EventLoadWorld().call();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (Glide.getInstance().isStarted() && event.world.isRemote) {
            new EventLeaveServer().call();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            new EventPreRenderTick().call();
        } else {
            new EventRenderTick().call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            modernHotbarRendered = false;
        }

        if (!Glide.getInstance().isStarted()) {
            return;
        }

        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            EventRenderCrosshair crosshair = new EventRenderCrosshair();
            crosshair.call();
            if (crosshair.isCancelled()) {
                cancelReplacement(event);
            }
            return;
        }

        if (event.type == RenderGameOverlayEvent.ElementType.HELMET) {
            EventRenderPumpkinOverlay helmet = new EventRenderPumpkinOverlay();
            helmet.call();
            if (helmet.isCancelled()) {
                cancelReplacement(event);
            }
            return;
        }

        BossHealthMod bossHealth = BossHealthMod.getInstance();
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH
                && bossHealth != null && bossHealth.isToggled()) {
            cancelReplacement(event);
            return;
        }

        ModernHotbarMod modernHotbar = ModernHotbarMod.getInstance();
        if (modernHotbar == null || !modernHotbar.isToggled()) {
            return;
        }

        if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            modernHotbarRendered = modernHotbar.tryRenderForgeHotbar(event.partialTicks);
            if (modernHotbarRendered) {
                cancelReplacement(event);
            }
        } else if (event.type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                && modernHotbarRendered
                && !modernHotbar.shouldRenderVanillaExperience()) {
            cancelReplacement(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlayElement(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.FOOD
                || !Glide.getInstance().isStarted()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getRenderViewEntity() instanceof EntityPlayer
                && minecraft.getRenderViewEntity().ridingEntity == null) {
            new EventRenderPlayerStats().call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL
                && Glide.getInstance().isStarted()) {
            IngameOverlayRenderer.render(event.partialTicks);
        }
    }

    private static void cancelReplacement(RenderGameOverlayEvent.Pre event) {
        event.setCanceled(true);
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(event, event.type));
    }

	private static void disableFeature(String feature, Throwable throwable) {
		if (throwable instanceof VirtualMachineError) {
			throw (VirtualMachineError) throwable;
		}
		if (throwable instanceof ThreadDeath) {
			throw (ThreadDeath) throwable;
		}

		GlideLogger.getLogger().error(
				"[GC/ERROR] Disabled failing Forge bridge feature: " + feature,
				throwable);
	}
}
