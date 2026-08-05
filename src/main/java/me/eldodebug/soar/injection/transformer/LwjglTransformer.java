package me.eldodebug.soar.injection.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class LwjglTransformer implements IClassTransformer {

    private static final String REGISTRY_DELEGATE =
            "net.minecraftforge.fml.common.registry.RegistryDelegate";

	private static final String NAME_DESCRIPTOR = "()Ljava/lang/String;";
	private static final String RESOURCE_NAME_DESCRIPTOR =
			"()Lnet/minecraft/util/ResourceLocation;";
	
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        if (REGISTRY_DELEGATE.equals(transformedName)) {
            return normalizeRegistryDelegate(basicClass);
        }

        if (name.equals("org.lwjgl.nanovg.NanoVGGLConfig")) {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);

            for (MethodNode method : node.methods) {
                if (method.name.equals("configGL")) {
                    InsnList list = new InsnList();

                    list.add(new VarInsnNode(Opcodes.LLOAD, 0));
                    list.add(new TypeInsnNode(Opcodes.NEW, "me/eldodebug/soar/injection/transformer/Lwjgl2FunctionProvider"));
                    list.add(new InsnNode(Opcodes.DUP));
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "me/eldodebug/soar/injection/transformer/Lwjgl2FunctionProvider",
                        "<init>",
                        "()V",
                        false
                    ));
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "org/lwjgl/nanovg/NanoVGGLConfig",
                        "config",
                        "(JLorg/lwjgl/system/FunctionProvider;)V",
                        false
                    ));
                    list.add(new InsnNode(Opcodes.RETURN));

                    method.instructions.clear();
                    method.instructions.insert(list);
                }
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(cw);
            return cw.toByteArray();
        }
        return basicClass;
    }

    /**
     * Some OptiFine 1.8.9 distributions contain an obsolete Forge interface
     * which shadows the real Forge class when OptiFine appears first on the
     * launch classpath. Restore the Forge 2318 interface contract.
     */
    private byte[] normalizeRegistryDelegate(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        boolean hasName = false;
        boolean hasResourceName = false;

        for (int i = node.methods.size() - 1; i >= 0; i--) {
            MethodNode method = node.methods.get(i);

            if ("name".equals(method.name)) {
                if (NAME_DESCRIPTOR.equals(method.desc)) {
                    hasName = true;
                } else {
                    node.methods.remove(i);
                }
            } else if ("getResourceName".equals(method.name)) {
                if (RESOURCE_NAME_DESCRIPTOR.equals(method.desc)) {
                    hasResourceName = true;
                } else {
                    node.methods.remove(i);
                }
            }
        }

        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT;
        if (!hasName) {
            node.methods.add(new MethodNode(access, "name", NAME_DESCRIPTOR, null, null));
        }
        if (!hasResourceName) {
            node.methods.add(new MethodNode(
                    access, "getResourceName", RESOURCE_NAME_DESCRIPTOR, null, null));
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
