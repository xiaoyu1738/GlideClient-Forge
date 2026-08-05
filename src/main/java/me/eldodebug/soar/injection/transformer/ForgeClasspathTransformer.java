package me.eldodebug.soar.injection.transformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LogWrapper;

/**
 * Prevents incomplete API stubs bundled by OptiFine from shadowing the
 * authoritative library classes when OptiFine appears first on the classpath.
 */
public final class ForgeClasspathTransformer implements IClassTransformer {

    private static final String FORGE_LIBRARY_PATH = "/net/minecraftforge/forge/";
    private static final String VECMATH_LIBRARY_PATH = "/java3d/vecmath/1.5.2/";
    private static final String VECMATH_MATRIX = "javax.vecmath.Matrix4f";

    private static final Set<String> OPTIFINE_FORGE_STUBS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "net.minecraftforge.client.model.IModelPart",
                    "net.minecraftforge.client.model.IModelState",
                    "net.minecraftforge.client.model.ISmartItemModel",
                    "net.minecraftforge.client.model.ITransformation",
                    "net.minecraftforge.client.model.TRSRTransformation",
                    "net.minecraftforge.client.model.pipeline.IVertexConsumer",
                    "net.minecraftforge.client.model.pipeline.IVertexProducer",
                    "net.minecraftforge.common.property.IUnlistedProperty",
                    "net.minecraftforge.fml.common.registry.RegistryDelegate"
            )));

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!OPTIFINE_FORGE_STUBS.contains(name) && !VECMATH_MATRIX.equals(name)) {
            return basicClass;
        }

        String resourceName = name.replace('.', '/') + ".class";
        String libraryPath = VECMATH_MATRIX.equals(name)
                ? VECMATH_LIBRARY_PATH
                : FORGE_LIBRARY_PATH;

        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.toExternalForm().contains(libraryPath)) {
                    LogWrapper.info("Glide replaced OptiFine API stub %s from %s", name, resource);
                    return read(resource);
                }
            }
        } catch (IOException e) {
            LogWrapper.log(org.apache.logging.log4j.Level.ERROR, e,
                    "Glide could not load the authoritative implementation of %s", name);
        }

        LogWrapper.warning("Glide did not find the authoritative implementation of %s", name);
        return basicClass;
    }

    private static byte[] read(URL resource) throws IOException {
        try (InputStream input = resource.openStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
}
