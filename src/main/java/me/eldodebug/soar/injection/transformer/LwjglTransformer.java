package me.eldodebug.soar.injection.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
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

        if (name == null
                || name.startsWith("me.eldodebug.soar.internal.asm.")
                || name.startsWith("me.eldodebug.soar.injection.transformer.")) {
            return basicClass;
        }

        if (REGISTRY_DELEGATE.equals(transformedName)) {
            return normalizeRegistryDelegate(basicClass);
        }

        basicClass = bridgeNanoVgClassLoaderBoundary(name, basicClass);

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

        // lwjgl-soar bundles the LWJGL 3 system package alongside Minecraft's
        // sealed LWJGL 2 root package. The loader only uses Version.getVersion
        // for a temporary extraction directory and the root-package class is
        // intentionally omitted from the Forge jar to avoid package sealing.
        if (name.equals("org.lwjgl.system.SharedLibraryLoader")
                || name.equals("org.lwjgl.system.Library")) {
            ClassNode node = new ClassNode();
            new ClassReader(basicClass).accept(node, ClassReader.EXPAND_FRAMES);
            boolean changed = false;
            for (MethodNode method : node.methods) {
                for (org.objectweb.asm.tree.AbstractInsnNode instruction = method.instructions.getFirst();
                        instruction != null; instruction = instruction.getNext()) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode invoke = (MethodInsnNode) instruction;
                        if (invoke.getOpcode() == Opcodes.INVOKESTATIC
                                && "org/lwjgl/Version".equals(invoke.owner)
                                && "getVersion".equals(invoke.name)
                                && "()Ljava/lang/String;".equals(invoke.desc)) {
                            method.instructions.set(instruction, new LdcInsnNode("3.3.1"));
                            changed = true;
                        }
                    }
                }
            }
            if (changed) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                return writer.toByteArray();
            }
        }
        return basicClass;
    }

    private byte[] bridgeNanoVgClassLoaderBoundary(String name, byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        if ((node.access & Opcodes.ACC_INTERFACE) != 0
                && name != null && !name.startsWith("me.eldodebug.soar.")) {
            for (MethodNode method : node.methods) {
                LwjglClassLoadingBridge.registerDescriptor(method.desc);
            }
        }

        boolean changed = false;
        for (MethodNode method : node.methods) {
            if ("loadClass".equals(method.name)
                    && "(Ljava/lang/String;Z)Ljava/lang/Class;".equals(method.desc)
                    && (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                LabelNode useLocalLoader = new LabelNode();
                InsnList bridge = new InsnList();
                bridge.add(new LdcInsnNode(LwjglClassLoadingBridge.PROPERTY_PREFIX));
                bridge.add(new VarInsnNode(Opcodes.ALOAD, 1));
                bridge.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/String", "concat",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
                bridge.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/System", "getProperty",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
                bridge.add(new JumpInsnNode(Opcodes.IFNULL, useLocalLoader));
                bridge.add(new VarInsnNode(Opcodes.ALOAD, 0));
                bridge.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/ClassLoader", "getParent",
                        "()Ljava/lang/ClassLoader;", false));
                bridge.add(new VarInsnNode(Opcodes.ALOAD, 1));
                bridge.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/ClassLoader", "loadClass",
                        "(Ljava/lang/String;)Ljava/lang/Class;", false));
                bridge.add(new InsnNode(Opcodes.ARETURN));
                bridge.add(useLocalLoader);
                bridge.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(bridge);
                changed = true;
            }
        }

        if (!changed) {
            return basicClass;
        }

        // Computing frames can recursively load a class which is already being
        // defined by this child loader. Preserve the existing frames and add
        // the single new branch frame above; only the maximum stack changes.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
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
