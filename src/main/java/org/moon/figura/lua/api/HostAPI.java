package org.moon.figura.lua.api;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.LuaError;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.avatar.AvatarManager;
import org.moon.figura.config.Configs;
import org.moon.figura.ducks.GuiMessageAccessor;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.api.entity.EntityAPI;
import org.moon.figura.lua.api.world.ItemStackAPI;
import org.moon.figura.lua.docs.LuaFieldDoc;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaMethodOverload;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.vector.FiguraVec2;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.mixin.LivingEntityAccessor;
import org.moon.figura.mixin.gui.ChatComponentAccessor;
import org.moon.figura.mixin.gui.ChatScreenAccessor;
import org.moon.figura.model.rendering.texture.FiguraTexture;
import org.moon.figura.overrides.NoInput;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.LuaUtils;
import org.moon.figura.utils.TextUtils;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "HostAPI",
        value = "host"
)
public class HostAPI {

    private final Avatar owner;
    private final boolean isHost;
    private final Minecraft minecraft;
    private Input defaultInput;
    private boolean hasPlayerMovement = false;

    @LuaWhitelist
    @LuaFieldDoc("host.unlock_cursor")
    public boolean unlockCursor = false;
    public Integer chatColor;

    public HostAPI(Avatar owner) {
        this.owner = owner;
        this.defaultInput = null;
        this.minecraft = Minecraft.getInstance();
        this.isHost = owner.isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_host")
    public boolean isHost() {
        return isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "host.is_cursor_unlocked")
    public boolean isCursorUnlocked() {
        return unlockCursor;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "boolean"
            ),
            value = "host.set_unlock_cursor")
    public HostAPI setUnlockCursor(boolean bool) {
        unlockCursor = bool;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "timesData"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class},
                            argumentNames = {"fadeInTime", "stayTime", "fadeOutTime"}
                    )
            },
            aliases = "titleTimes",
            value = "host.set_title_times"
    )
    public HostAPI setTitleTimes(Object x, Double y, Double z) {
        if (!isHost()) return this;
        FiguraVec3 times = LuaUtils.parseVec3("setTitleTimes", x, y, z);
        this.minecraft.gui.setTimes((int) times.x, (int) times.y, (int) times.z);
        return this;
    }

    @LuaWhitelist
    public HostAPI titleTimes(Object x, Double y, Double z) {
        return setTitleTimes(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.clear_title")
    public HostAPI clearTitle() {
        if (isHost())
            this.minecraft.gui.clear();
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "title",
            value = "host.set_title"
    )
    public HostAPI setTitle(@LuaNotNil String text) {
        if (isHost())
            this.minecraft.gui.setTitle(TextUtils.tryParseJson(text));
        return this;
    }

    @LuaWhitelist
    public HostAPI title(@LuaNotNil String text) {
        return setTitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "subtitle",
            value = "host.set_subtitle"
    )
    public HostAPI setSubtitle(@LuaNotNil String text) {
        if (isHost())
            this.minecraft.gui.setSubtitle(TextUtils.tryParseJson(text));
        return this;
    }

    @LuaWhitelist
    public HostAPI subtitle(@LuaNotNil String text) {
        return setSubtitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "text"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, boolean.class},
                            argumentNames = {"text", "animated"}
                    )
            },
            aliases = "actionbar",
            value = "host.set_actionbar"
    )
    public HostAPI setActionbar(@LuaNotNil String text, boolean animated) {
        if (isHost())
            this.minecraft.gui.setOverlayMessage(TextUtils.tryParseJson(text), animated);
        return this;
    }

    @LuaWhitelist
    public HostAPI actionbar(@LuaNotNil String text, boolean animated) {
        return setActionbar(text, animated);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.send_chat_message"
    )
    public HostAPI sendChatMessage(@LuaNotNil String message) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) connection.sendChat(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "command"
            ),
            value = "host.send_chat_command"
    )
    public HostAPI sendChatCommand(@LuaNotNil String command) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.append_chat_history"
    )
    public HostAPI appendChatHistory(@LuaNotNil String message) {
        if (isHost())
            this.minecraft.gui.getChat().addRecentChat(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Integer.class,
                    argumentNames = "index"
            ),
            value = "host.get_chat_message"
    )
    public Map<String, Object> getChatMessage(int index) {
        if (!isHost())
            return null;

        index--;
        List<GuiMessage> messages = ((ChatComponentAccessor) this.minecraft.gui.getChat()).getAllMessages();
        if (index < 0 || index >= messages.size())
            return null;

        GuiMessage message = messages.get(index);
        Map<String, Object> map = new HashMap<>();

        map.put("addedTime", message.addedTime());
        map.put("message", message.content().getString());
        map.put("json", message.content());
        map.put("backgroundColor", ((GuiMessageAccessor) (Object) message).figura$getColor());

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "index"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, String.class},
                            argumentNames = {"index", "newMessage"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, String.class, FiguraVec3.class},
                            argumentNames = {"index", "newMessage", "backgroundColor"}
                    )
            },
            value = "host.set_chat_message")
    public HostAPI setChatMessage(int index, String newMessage, FiguraVec3 backgroundColor) {
        if (!isHost()) return this;

        index--;
        List<GuiMessage> messages = ((ChatComponentAccessor) this.minecraft.gui.getChat()).getAllMessages();
        if (index < 0 || index >= messages.size())
            return this;

        if (newMessage == null)
            messages.remove(index);
        else {
            GuiMessage old = messages.get(index);
            GuiMessage neww = new GuiMessage(this.minecraft.gui.getGuiTicks(), TextUtils.tryParseJson(newMessage), null, GuiMessageTag.chatModified(old.content().getString()));
            messages.set(index, neww);
            ((GuiMessageAccessor) (Object) neww).figura$setColor(backgroundColor != null ? ColorUtils.rgbToInt(backgroundColor) : ((GuiMessageAccessor) (Object) old).figura$getColor());
        }

        this.minecraft.gui.getChat().rescaleChat();
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "offhand"
                    )
            },
            value = "host.swing_arm"
    )
    public HostAPI swingArm(boolean offhand) {
        if (isHost() && this.minecraft.player != null)
            this.minecraft.player.swing(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "slot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "slot"
                    )
            },
            value = "host.get_slot"
    )
    public ItemStackAPI getSlot(@LuaNotNil Object slot) {
        if (!isHost()) return null;
        Entity e = this.owner.luaRuntime.getUser();
        return ItemStackAPI.verify(e.getSlot(LuaUtils.parseSlot(slot, null)).get());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "slot"),
                    @LuaMethodOverload(argumentTypes = Integer.class, argumentNames = "slot"),
                    @LuaMethodOverload(argumentTypes = {String.class, String.class}, argumentNames = {"slot", "item"}),
                    @LuaMethodOverload(argumentTypes = {Integer.class, ItemStackAPI.class}, argumentNames = {"slot", "item"})
            },
            value = "host.set_slot"
    )
    public HostAPI setSlot(@LuaNotNil Object slot, Object item) {
        if (!isHost() || (slot == null && item == null) || this.minecraft.gameMode == null || this.minecraft.player == null || !this.minecraft.gameMode.getPlayerMode().isCreative())
            return this;

        Inventory inventory = this.minecraft.player.getInventory();

        int index = LuaUtils.parseSlot(slot, inventory);
        ItemStack stack = LuaUtils.parseItemStack("setSlot", item);

        inventory.setItem(index, stack);
        this.minecraft.gameMode.handleCreativeModeItemAdd(stack, index + 36);

        return this;
    }

    @LuaWhitelist
    public HostAPI setBadge(int index, boolean value, boolean pride) {
        if (!isHost()) return this;
        if (!FiguraMod.DEBUG_MODE)
            throw new LuaError("Congrats, you found this debug easter egg!");

        Pair<BitSet, BitSet> badges = AvatarManager.getBadges(owner.owner);
        if (badges == null)
            return this;

        BitSet set = pride ? badges.getFirst() : badges.getSecond();
        set.set(index, value);
        return this;
    }

    @LuaWhitelist
    public HostAPI badge(int index, boolean value, boolean pride) {
        return setBadge(index, value, pride);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_color")
    public Integer getChatColor() {
        return isHost() ? this.chatColor : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "color"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            aliases = "chatColor",
            value = "host.set_chat_color"
    )
    public HostAPI setChatColor(Object x, Double y, Double z) {
        if (isHost()) this.chatColor = x == null ? null : ColorUtils.rgbToInt(LuaUtils.parseVec3("setChatColor", x, y, z));
        return this;
    }

    @LuaWhitelist
    public HostAPI chatColor(Object x, Double y, Double z) {
        return setChatColor(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_text")
    public String getChatText() {
        if (isHost() && this.minecraft.screen instanceof ChatScreen chat)
            return ((ChatScreenAccessor) chat).getInput().getValue();

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "chatText",
            value = "host.set_chat_text"
    )
    public HostAPI setChatText(@LuaNotNil String text) {
        if (isHost() && Configs.CHAT_MESSAGES.value && this.minecraft.screen instanceof ChatScreen chat)
            ((ChatScreenAccessor) chat).getInput().setValue(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI chatText(@LuaNotNil String text) {
        return setChatText(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen")
    public String getScreen() {
        if (!isHost() || this.minecraft.screen == null)
            return null;
        return this.minecraft.screen.getClass().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen_slot_count")
    public Integer getScreenSlotCount() {
        if (isHost() && this.minecraft.screen instanceof AbstractContainerScreen<?> screen)
            return screen.getMenu().slots.size();
        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(overloads = {
            @LuaMethodOverload(argumentTypes = String.class, argumentNames = "slot"),
            @LuaMethodOverload(argumentTypes = Integer.class, argumentNames = "slot")
    }, value = "host.get_screen_slot")
    public ItemStackAPI getScreenSlot(@LuaNotNil Object slot) {
        if (!isHost() || !(this.minecraft.screen instanceof AbstractContainerScreen<?> screen))
            return null;

        NonNullList<Slot> slots = screen.getMenu().slots;
        int index = LuaUtils.parseSlot(slot, null);
        if (index < 0 || index >= slots.size())
            return null;
        return ItemStackAPI.verify(slots.get(index).getItem());
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_open")
    public boolean isChatOpen() {
        return isHost() && this.minecraft.screen instanceof ChatScreen;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_container_open")
    public boolean isContainerOpen() {
        return isHost() && this.minecraft.screen instanceof AbstractContainerScreen;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            value = "host.screenshot")
    public FiguraTexture screenshot(@LuaNotNil String name) {
        if (!isHost())
            return null;

        NativeImage img = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
        return owner.luaRuntime.texture.register(name, img, true);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_avatar_uploaded")
    public boolean isAvatarUploaded() {
        return isHost() && AvatarManager.localUploaded;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_status_effects")
    public List<Map<String, Object>> getStatusEffects() {
        List<Map<String, Object>> list = new ArrayList<>();

        LocalPlayer player = this.minecraft.player;
        if (!isHost() || player == null)
            return list;

        for (MobEffectInstance effect : player.getActiveEffects()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", effect.getEffect().getDescriptionId());
            map.put("amplifier", effect.getAmplifier());
            map.put("duration", effect.getDuration());
            map.put("visible", effect.isVisible());

            list.add(map);
        }

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_clipboard")
    public String getClipboard() {
        return isHost() ? this.minecraft.keyboardHandler.getClipboard() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "clipboard",
            value = "host.set_clipboard")
    public HostAPI setClipboard(@LuaNotNil String text) {
        if (isHost()) this.minecraft.keyboardHandler.setClipboard(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI clipboard(@LuaNotNil String text) {
        return setClipboard(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_attack_charge")
    public float getAttackCharge() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAttackStrengthScale(0f);
        return 0f;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_jumping")
    public boolean isJumping() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return ((LivingEntityAccessor) player).isJumping();
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_flying")
    public boolean isFlying() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAbilities().flying;
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_reach_distance")
    public double getReachDistance() {
        return this.minecraft.gameMode == null ? 0 : this.minecraft.gameMode.getPickRange();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_air")
    public int getAir() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAirSupply();
        return 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_pick_block")
    public Object[] getPickBlock() {
        return isHost() ? LuaUtils.parseBlockHitResult(minecraft.hitResult) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_pick_entity")
    public EntityAPI<?> getPickEntity() {
        return isHost() && minecraft.crosshairPickEntity != null ? EntityAPI.wrap(minecraft.crosshairPickEntity) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "vec"
                    ),
            },
            value = "host.set_velocity"
    )
    public void setVelocity(Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("player_setVelocity", x, y, z);
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null) { player.setDeltaMovement(new Vec3(vec.x, vec.y, vec.z)); }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "pos"
                    ),
            },
            value = "host.set_pos"
    )
    public void setPos(Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("player_setPos", x, y, z);
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null) { player.setPos(new Vec3(vec.x, vec.y, vec.z)); }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class},
                            argumentNames = {"playerMovement"}
                    )
            },
            value = "host.set_player_movement"
    )
    public void setPlayerMovement(Boolean playerMovement) {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null) {
            if (this.defaultInput == null) {
                this.defaultInput = player.input; // set default input
            }

            // sets playermovement class
            if (!playerMovement) {
                player.input = new NoInput();
            } else {
                player.input = this.defaultInput;
            }

            hasPlayerMovement = playerMovement;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class},
                            argumentNames = {"isMainHand"}
                    )
            },
            value = "host.start_using_item"
    )
    public void startUsingItem(Boolean isMainHand) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            player.startUsingItem(isMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
    }

    //host:startUsingItem(false)

    @LuaWhitelist
    @LuaMethodDoc("host.get_player_movement")
    public Boolean getPlayerMovement() {
        return hasPlayerMovement;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_last_death_pos")
    public FiguraVec3 getLastDeathPos() {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            BlockPos deathPos = player.getLastDeathLocation().get().pos();
            return FiguraVec3.fromBlockPos(deathPos);
        }
        return null;
    }


    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec2.class,
                            argumentNames = "vec"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class},
                            argumentNames = {"x", "y"}
                    )
            },
            value = "host.set_rot"
    )
    public void setRot(Object x, Double y) {
        FiguraVec2 vec = LuaUtils.parseVec2("player_setRot", x, y);
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            player.setXRot((float) vec.x);
            player.setYRot((float) vec.y);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Double.class},
                            argumentNames = {"angle"}
                    )
            },
            value = "host.set_body_rot"
    )
    public void setBodyRot(Double angle) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            player.setYBodyRot(angle.floatValue());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Double.class},
                            argumentNames = {"angle"}
                    )
            },
            value = "host.set_body_offset_rot"
    )
    public void setBodyOffsetRot(Double angle) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            player.setYBodyRot( angle.floatValue() + player.getYRot() );
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class},
                            argumentNames = {"hasForce"}
                    )
            },
            value = "host.set_gravity"
    )
    public void setGravity(Boolean hasForce) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            player.setNoGravity(!hasForce);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class},
                            argumentNames = {"hasForce"}
                    )
            },
            value = "host.set_drag"
    )
    public void setDrag(Boolean hasForce) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            player.setDiscardFriction(!hasForce);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_verified")
    public boolean isChatVerified() {
        if (!isHost()) return false;
        ClientPacketListener connection = this.minecraft.getConnection();
        PlayerInfo playerInfo = connection != null ? connection.getPlayerInfo(owner.owner) : null;
        return playerInfo != null && playerInfo.hasVerifiableChat();
    }

    public Object __index(String arg) {
        if ("unlockCursor".equals(arg))
            return unlockCursor;
        return null;
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, Object value) {
        if ("unlockCursor".equals(key))
            unlockCursor = (Boolean) value;
        else throw new LuaError("Cannot assign value on key \"" + key + "\"");
    }

    @Override
    public String toString() {
        return "HostAPI";
    }
}
