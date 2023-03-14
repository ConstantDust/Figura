package org.moon.figura.model.rendertasks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.LuaError;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.api.world.ItemStackAPI;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaMethodOverload;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.model.PartCustomization;
import org.moon.figura.utils.LuaUtils;

import java.util.Random;

@LuaWhitelist
@LuaTypeDoc(
        name = "ItemTask",
        value = "item_task"
)
public class ItemTask extends RenderTask {

    private ItemStack item;
    private ItemTransforms.TransformType renderType = ItemTransforms.TransformType.NONE;
    private boolean left = false;
    private int cachedComplexity;

    public ItemTask(String name) {
        super(name);
    }

    @Override
    public boolean render(PartCustomization.Stack stack, MultiBufferSource buffer, int light, int overlay) {
        if (!enabled || item == null || item.isEmpty())
            return false;

        this.pushOntoStack(stack);
        PoseStack poseStack = stack.peek().copyIntoGlobalPoseStack();
        poseStack.scale(-16, 16, -16);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                null, item, renderType, left,
                poseStack, buffer, null,
                this.light != null ? this.light : light, this.overlay != null ? this.overlay : overlay, 0);

        stack.pop();
        return true;
    }

    @Override
    public int getComplexity() {
        return cachedComplexity;
    }


    // -- lua -- //


    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "item"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = ItemStackAPI.class,
                            argumentNames = "item"
                    )
            },
            aliases = "item",
            value = "item_task.set_item"
    )
    public ItemTask setItem(Object item) {
        this.item = LuaUtils.parseItemStack("item", item);
        Minecraft client = Minecraft.getInstance();
        Random random = client.level != null ? client.level.random : new Random();
        cachedComplexity = client.getItemRenderer().getModel(this.item, null, null, 0).getQuads(null, null, random).size();
        return this;
    }

    @LuaWhitelist
    public ItemTask item(Object item) {
        return setItem(item);
    }

    @LuaWhitelist
    @LuaMethodDoc("item_task.get_render_type")
    public String getRenderType() {
        return this.renderType.name();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                        argumentTypes = String.class,
                        argumentNames = "renderType"
            ),
            aliases = "renderType",
            value = "item_task.set_render_type"
    )
    public ItemTask setRenderType(@LuaNotNil String type) {
        try {
            this.renderType = ItemTransforms.TransformType.valueOf(type.toUpperCase());
            this.left = this.renderType == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND || this.renderType == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND;
            return this;
        } catch (Exception ignored) {
            throw new LuaError("Illegal RenderType: \"" + type + "\".");
        }
    }

    @LuaWhitelist
    public ItemTask renderType(@LuaNotNil String type) {
        return setRenderType(type);
    }

    @Override
    public String toString() {
        return name + " (Item Render Task)";
    }
}
