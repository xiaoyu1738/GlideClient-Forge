package me.eldodebug.soar.injection.mixin.mixins.shader;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.eldodebug.soar.injection.interfaces.IMixinShaderGroup;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;

@Mixin(ShaderGroup.class)
public class MixinShaderGroup implements IMixinShaderGroup {

	@Shadow
	private List<Shader> listShaders;
	
	@Override
	public List<Shader> glide$getListShaders() {
		return listShaders;
	}
}
