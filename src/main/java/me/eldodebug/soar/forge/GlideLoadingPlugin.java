package me.eldodebug.soar.forge;

import java.util.Map;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * Early Forge entry point used to install Glide's transformers before
 * Minecraft classes are loaded. The regular mod lifecycle is owned by
 * {@link GlideForgeMod}.
 */
@IFMLLoadingPlugin.Name("Glide Client")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"me.eldodebug.soar.forge.GlideLoadingPlugin"})
public final class GlideLoadingPlugin implements IFMLLoadingPlugin {

    private static final String FORGE_CLASSPATH_TRANSFORMER =
            "me.eldodebug.soar.injection.transformer.ForgeClasspathTransformer";

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {FORGE_CLASSPATH_TRANSFORMER};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        ForgeEnvironment.detect(Launch.classLoader);
        ForgeMixinBootstrap.initialize(Launch.classLoader);
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
