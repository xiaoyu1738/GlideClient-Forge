package me.eldodebug.soar.injection.mixin.mixins.client;

import eu.shoroa.contrib.render.ShBlur;
import me.eldodebug.soar.utils.MacOSUtils;
import net.minecraft.util.Util;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiSplashScreen;
import me.eldodebug.soar.injection.interfaces.IMixinEntityLivingBase;
import me.eldodebug.soar.injection.interfaces.IMixinMinecraft;
import me.eldodebug.soar.management.event.impl.EventToggleFullscreen;
import me.eldodebug.soar.management.mods.impl.FPSLimiterMod;
import me.eldodebug.soar.management.mods.impl.FPSSpooferMod;
import me.eldodebug.soar.management.mods.impl.AnimationsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IMixinMinecraft {

	@Shadow
    private Timer timer;
	
    @Shadow
    public int displayWidth;
    
    @Shadow
    public int displayHeight;
    
    @Shadow
    private Session session;
    
    @Shadow
    public PlayerControllerMP playerController;
    
    @Shadow
    public MovingObjectPosition objectMouseOver;
    
    @Shadow
    public EffectRenderer effectRenderer;
    
    @Shadow
    public EntityPlayerSP thePlayer;
    
    @Shadow
    private int leftClickCounter;
    
    @Shadow
    public WorldClient theWorld;
    
    @Shadow
    public GameSettings gameSettings;
    
    @Shadow
    public GuiScreen currentScreen;
    
    @Shadow
    private boolean fullscreen;
    
    @Shadow
    @Final
    private DefaultResourcePack mcDefaultResourcePack;
    
    @Shadow
    public abstract void clickMouse();
    
    @Shadow
    public abstract void rightClickMouse();
    
	@Shadow
    public abstract void displayGuiScreen(GuiScreen guiScreenIn);
	
	@Shadow
	public abstract void updateDisplay();
	
	@Shadow 
	public EntityRenderer entityRenderer;
	
	@Shadow
    private static int debugFPS;
	
	@Shadow
	private Entity renderViewEntity;
	
    @Shadow 
    private boolean enableGLErrorChecking;

	@Shadow protected abstract void resize(int width, int height);

    @Inject(method = "run", at = @At("HEAD"))
    public void preRun(CallbackInfo callbackInfo) {
    	
        if (displayWidth < 1100) {
        	displayWidth = 1100;
        }
            
        if (displayHeight < 630) {
        	displayHeight = 630;
        }
    }
    
    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    public void preShutdown(CallbackInfo ci) {
    	Glide.getInstance().stop();
    }

    @Inject(method = "startGame", at = @At("TAIL"))
    private void disableGlErrorChecking(CallbackInfo ci) {
        this.enableGLErrorChecking = false;
    }
    
	@Inject(method = "setIngameFocus", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MouseHelper;grabMouseCursor()V"))
	public void fixKeyBinding(CallbackInfo callback) {
		for(KeyBinding keyBinding : gameSettings.keyBindings) {
			try {
				KeyBinding.setKeyBindState(keyBinding.getKeyCode(), keyBinding.getKeyCode() < 256 && Keyboard.isKeyDown(keyBinding.getKeyCode()));
			}
			catch (Exception e) {}
		}
	}
	
    @Inject(method = "sendClickBlockToController", at = @At("HEAD"))
    public void preSendClickBlockToController(boolean leftClick, CallbackInfo ci) {
    	
    	AnimationsMod mod = AnimationsMod.getInstance();
    	
        if (mod.isToggled() && mod.getBlockHitSetting().isToggled() && mod.getPushingSetting().isToggled() && gameSettings.keyBindUseItem.isKeyDown()) {
            if (leftClickCounter <= 0 && leftClick && objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                if (!theWorld.isAirBlock(objectMouseOver.getBlockPos()) && thePlayer.isAllowEdit()) {
                	
                    if (mod.getPushingParticleSetting().isToggled()) {
                        effectRenderer.addBlockHitEffects(objectMouseOver.getBlockPos(), objectMouseOver.sideHit);
                    }
                    
                    if (!thePlayer.isSwingInProgress || thePlayer.swingProgressInt >= ((IMixinEntityLivingBase) thePlayer).glide$getArmSwingAnimation() / 2 || thePlayer.swingProgressInt < 0) {
                        thePlayer.swingProgressInt = -1;
                        thePlayer.isSwingInProgress = true;
                    }
                }
            } else {
                playerController.resetBlockRemoving();
            }
        }
    }
    
	@Inject(method = "createDisplay", at = @At("RETURN"))
	private void setGlideWindowTitle(CallbackInfo ci) {
		Display.setTitle("Glide Client v" + Glide.getInstance().getVersion() + " ("
				+ Glide.getInstance().getVersionIdentifier() + ") for " + Display.getTitle());
	}
	
	@Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    public void getLimitFramerate(CallbackInfoReturnable<Integer> cir) {
		
		FPSLimiterMod limiter = FPSLimiterMod.getInstance();
		
		if(limiter != null && limiter.isToggled()) {
			
			if(this.currentScreen == null && limiter.getLimitMaxFpsSetting().isToggled()) {
				cir.setReturnValue(limiter.getMaxFpsSetting().getValueInt());
			} else if(this.currentScreen != null && limiter.getLimitGuiFps().isToggled()) {
				cir.setReturnValue(limiter.getGuiFpsSetting().getValueInt());
			}
		}
	}
	
    @Inject(method = "isFramerateLimitBelowMax", at = @At("HEAD"), cancellable = true)
    public void isFramerateLimitBelowMax(CallbackInfoReturnable<Boolean> cir) {
    	
		FPSLimiterMod limiter = FPSLimiterMod.getInstance();
		
		if(limiter != null && limiter.isToggled() && limiter.getLimitMaxFpsSetting().isToggled()) {
			cir.setReturnValue(true);
	    }
    }
    
    @Inject(method = "drawSplashScreen", at = @At("HEAD"), cancellable = true)
    public void overrideSplash(TextureManager textureManagerInstance, CallbackInfo ci) {
    	new GuiSplashScreen().draw();
    	ci.cancel();
    }
    
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void clearLoadedMaps(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        if (worldClientIn != this.theWorld) {
            this.entityRenderer.getMapItemRenderer().clearLoadedMaps();
        }
    }

	/**
	 * Mixin setGameIcon
	 * @reason change the game icon to a custom one
	 */
	@Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
	private void setGameIcon(CallbackInfo c) {
		if(Util.getOSType() == Util.EnumOS.OSX) {
			MacOSUtils.setDockIcon("/assets/minecraft/soar/osx.png");
			c.cancel();
		}
	}
    
    @ModifyArg(method = "launchIntegratedServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", ordinal = 1))
    private GuiScreen displayWorkingScreen(GuiScreen original) {
        return new GuiScreenWorking();
    }
    
    @Inject(method = "toggleFullscreen", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setFullscreen(Z)V", remap = false))
    private void resolveScreenState(CallbackInfo ci) {
        if (!this.fullscreen && SystemUtils.IS_OS_WINDOWS) {
            Display.setResizable(false);
            Display.setResizable(true);
        }
    }
    
	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void handleToggle(CallbackInfo ci) {
		
		EventToggleFullscreen event = new EventToggleFullscreen(!fullscreen);
		event.call();
		
		if(event.isCancelled()) {
			ci.cancel();
			gameSettings.fullScreen = fullscreen;
		}else if(!event.isApplyState()) {
			ci.cancel();
			gameSettings.fullScreen = (fullscreen = !fullscreen);
		}
	}
    
	@Inject(method = "getDebugFPS", at = @At("HEAD"), cancellable = true)
	private static void glide$getDebugFPS(CallbackInfoReturnable<Integer> cir) {
		if(FPSSpooferMod.getInstance().isToggled()) {
			cir.setReturnValue(debugFPS * FPSSpooferMod.getInstance().getMultiplierSetting().getValueInt());
		}
	}
	
	@Override
	public Timer glide$getTimer() {
		return timer;
	}

	@Override
	public void glide$setSession(Session session) {
		this.session = session;
	}

	@Override
	public void glide$callClickMouse() {
		clickMouse();
	}
	
	@Override
	public void glide$callRightClickMouse() {
		rightClickMouse();
	}

	@Override
	public void glide$setLeftClickCounter(int value) {
		leftClickCounter = value;
	}
	
    @Override
    public DefaultResourcePack glide$getMcDefaultResourcePack() {
    	return this.mcDefaultResourcePack;
    }
    
    @Override
    public Entity glide$getRenderViewEntity() {
    	return renderViewEntity;
    }
    
	@Override
	@Accessor("running")
	public abstract boolean glide$isRunning();
	
	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/SkinManager;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Ljava/io/File;Lcom/mojang/authlib/minecraft/MinecraftSessionService;)V"))
	public void splashSkinManager(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/chunk/storage/AnvilSaveConverter;<init>(Ljava/io/File;)V"))
	public void splashSaveLoader(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/audio/SoundHandler;<init>(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/settings/GameSettings;)V"))
	public void splashSoundHandler(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/audio/MusicTicker;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashMusicTicker(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/FontRenderer;<init>(Lnet/minecraft/client/settings/GameSettings;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureManager;Z)V"))
	public void splashFontRenderer(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/MouseHelper;<init>()V"))
	public void splashMouseHelper(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/texture/TextureMap;<init>(Ljava/lang/String;)V"))
	public void splashTextureMap(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/ModelManager;<init>(Lnet/minecraft/client/renderer/texture/TextureMap;)V"))
	public void splashModelManager(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/RenderItem;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/resources/model/ModelManager;)V"))
	public void splashRenderItem(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/RenderManager;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/renderer/entity/RenderItem;)V"))
	public void splashRenderManager(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemRenderer;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashItemRenderer(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/EntityRenderer;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/resources/IResourceManager;)V"))
	public void splashEntityRenderer(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;<init>(Lnet/minecraft/client/renderer/BlockModelShapes;Lnet/minecraft/client/settings/GameSettings;)V"))
	public void splashBlockRenderDispatcher(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/RenderGlobal;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashRenderGlobal(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/achievement/GuiAchievement;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashGuiAchivement(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/EffectRenderer;<init>(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
	public void splashEffectRenderer(CallbackInfo callback) {
		updateDisplay();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiIngame;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashGuiIngame(CallbackInfo callback) {
		ShBlur.getInstance().init();
		updateDisplay();
	}

	@Override
	public void glide$resizeWindow(int width, int height) {
		resize(width, height);
	}

	@Inject(method = "resize", at = @At("TAIL"))
	public void inject$resize(int width, int height, CallbackInfo ci) {
		ShBlur.getInstance().resize();
	}
}
