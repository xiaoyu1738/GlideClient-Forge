package me.eldodebug.soar.injection.mixin.mixins.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.eldodebug.soar.hooks.RenderEntityItemHook;
import me.eldodebug.soar.management.mods.impl.Items2DMod;
import me.eldodebug.soar.management.mods.impl.ItemPhysicsMod;
import me.eldodebug.soar.management.mods.impl.UHCOverlayMod;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem {

	@Shadow
    private RenderItem itemRenderer;
	
	@Shadow
    public abstract int func_177078_a(ItemStack stack);
	
	@Inject(method = "func_177077_a", at = @At("HEAD"), cancellable = true)
	private void glide$transformDroppedItem(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_, CallbackInfoReturnable<Integer> cir) {
		if (ItemPhysicsMod.getInstance().isToggled()
				|| Items2DMod.getInstance().isToggled()
				|| UHCOverlayMod.getInstance().isToggled()) {
			cir.setReturnValue(RenderEntityItemHook.func_177077_a(itemIn, p_177077_2_, p_177077_4_, p_177077_6_, p_177077_8_, p_177077_9_, func_177078_a(itemIn.getEntityItem())));
		}
	}
	
    @Redirect(method = {"doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V"))
    public void stopItemModelRender(RenderItem instance, ItemStack stack, IBakedModel model) {
        if (Items2DMod.getInstance().isToggled() && !model.isGui3d()) {
            RenderEntityItemHook.oldItemRender(instance, model, stack);
        } else {
            this.itemRenderer.renderItem(stack, model);
        }
    }
}
