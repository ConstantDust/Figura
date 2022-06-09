package org.moon.figura;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import org.moon.figura.avatars.Avatar;
import org.moon.figura.avatars.AvatarManager;
import org.moon.figura.avatars.providers.LocalAvatarLoader;
import org.moon.figura.backend.NetworkManager;
import org.moon.figura.commands.FiguraCommands;
import org.moon.figura.config.Config;
import org.moon.figura.config.ConfigManager;
import org.moon.figura.gui.PaperDoll;
import org.moon.figura.gui.actionwheel.ActionWheel;
import org.moon.figura.lua.FiguraLuaPrinter;
import org.moon.figura.lua.FiguraLuaState;
import org.moon.figura.lua.docs.FiguraDocsManager;
import org.moon.figura.mixin.SkullBlockEntityAccessor;
import org.moon.figura.trust.TrustManager;
import org.moon.figura.utils.LuaUtils;
import org.moon.figura.utils.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

public class FiguraMod implements ClientModInitializer {

    public static final String MOD_ID = "figura";
    public static final String MOD_NAME = "Figura";
    public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion().getFriendlyString();
    public static final boolean DEBUG_MODE = Math.random() + 1 < 0;
    public static final boolean CHEESE_DAY = LocalDate.now().getDayOfMonth() == 1 && LocalDate.now().getMonthValue() == 4;
    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir().normalize();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static boolean DO_OUR_NATIVES_WORK = false;

    public static int ticks = 0;

    @Override
    public void onInitializeClient() {
        //init managers
        ConfigManager.init();
        TrustManager.init();
        FiguraDocsManager.init();
        FiguraCommands.init();
        LuaUtils.setupNativesForLua();

        //register events
        ClientTickEvents.START_CLIENT_TICK.register(FiguraMod::tick);
        WorldRenderEvents.START.register(levelRenderer -> AvatarManager.onWorldRender(levelRenderer.tickDelta()));
        WorldRenderEvents.END.register(levelRenderer -> AvatarManager.afterWorldRender());
        WorldRenderEvents.AFTER_ENTITIES.register(FiguraMod::renderFirstPersonWorldParts);
        HudRenderCallback.EVENT.register(FiguraMod::hudRender);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(FiguraLuaState.SCRIPT_LISTENER);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(LocalAvatarLoader.AVATAR_LISTENER);
    }

    private static void tick(Minecraft client) {
        NetworkManager.tick();
        LocalAvatarLoader.tickWatchedKey();
        AvatarManager.tickLoadedAvatars();
        FiguraLuaPrinter.printChatFromQueue();
        ticks++;
    }

    private static void renderFirstPersonWorldParts(WorldRenderContext context) {
        if (!context.camera().isDetached()) {
            Entity watcher = context.camera().getEntity();
            Avatar avatar = AvatarManager.getAvatar(watcher);
            if (avatar != null) {
                avatar.onFirstPersonWorldRender(watcher, context.consumers(), context.matrixStack(), context.camera(), context.tickDelta());
            }
        }
    }

    private static void hudRender(PoseStack stack, float delta) {
        PaperDoll.render(stack);
        ActionWheel.render(stack);
        //TODO popup menu
    }

    // -- Helper Functions -- //

    //mod root directory
    public static Path getFiguraDirectory() {
        String config = (String) Config.MAIN_DIR.value;
        Path p = config.isBlank() ? GAME_DIR.resolve(MOD_ID) : Path.of(config);
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            LOGGER.error("Failed to create the main " + MOD_NAME + " directory", e);
        }

        return p;
    }

    //mod cache directory
    public static Path getCacheDirectory() {
        Path p = getFiguraDirectory().resolve("cache");
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            LOGGER.error("Failed to create cache directory", e);
        }

        return p;
    }

    //get local player uuid
    public static UUID getLocalPlayerUUID() {
        return Minecraft.getInstance().getUser().getGameProfile().getId();
    }

    /**
     * Sends a chat message right away. Use when you know your message is safe.
     * If your message is unsafe, (generated by a user), use luaSendChatMessage instead.
     * @param message - text to send
     */
    public static void sendChatMessage(Component message) {
        if (Minecraft.getInstance().gui != null)
            Minecraft.getInstance().gui.getChat().addMessage(TextUtils.replaceTabs(message));
        else
            LOGGER.info(message.getString());
    }

    /**
     * Converts a player name to UUID using minecraft internal functions.
     * @param playerName - the player name
     * @return - the player's uuid or null
     */
    public static UUID playerNameToUUID(String playerName) {
        GameProfileCache cache = SkullBlockEntityAccessor.getProfileCache();
        if (cache == null)
            return null;

        var profile = cache.get(playerName);
        return profile.isEmpty() ? null : profile.get().getId();
    }
}
