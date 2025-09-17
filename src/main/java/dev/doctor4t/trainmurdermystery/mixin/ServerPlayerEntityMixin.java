package dev.doctor4t.trainmurdermystery.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @WrapOperation(method = "trySleep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V"))
    public void tmm$disableSleepMessage(ServerPlayerEntity instance, Text message, boolean overlay, Operation<Void> original) {

    }
}
