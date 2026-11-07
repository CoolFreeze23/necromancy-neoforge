package com.sirolf2009.necromancy.smoke.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.neoforged.fml.loading.FMLConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Dev-only: the client's window stays invisible and unfocused for its whole life, and gives up after 15 minutes. */
@Mixin(Window.class)
public abstract class HiddenWindowMixin {
    @Shadow private boolean fullscreen;
    @Shadow private boolean actuallyFullscreen;
    @Shadow public abstract long getWindow();
    @Unique private final long nekoma$started = System.nanoTime();
    @Unique private boolean nekoma$stopping;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private long nekoma$hiddenContext(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor, Operation<Long> original) {
        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            throw new IllegalStateException("The hidden window needs the early window off before startup");
        }
        fullscreen = actuallyFullscreen = false;
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        long window = original.call(width, height, title, (LongSupplier) () -> 0L);
        if (window == 0L) throw new IllegalStateException("Hidden GL context creation failed");
        return window;
    }

    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true)
    private void nekoma$stayWindowed(CallbackInfo callback) {
        callback.cancel();
    }

    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void nekoma$assertHidden(CallbackInfo callback) {
        if (GLFW.glfwGetWindowAttrib(getWindow(), GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE) {
            GLFW.glfwHideWindow(getWindow());
        }
        if (!nekoma$stopping && System.nanoTime() - nekoma$started > 900_000_000_000L) {
            nekoma$stopping = true;
            com.mojang.logging.LogUtils.getLogger().error("SMOKE: timed out");
            net.minecraft.client.Minecraft.getInstance().stop();
        }
    }
}
