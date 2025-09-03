package dev.gigaherz.jsonthings.things.builders;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.things.scripting.McFunctionScript;
import dev.gigaherz.jsonthings.util.parse.JParse;
import dev.gigaherz.jsonthings.util.parse.value.Any;
import net.minecraft.resources.ResourceLocation;

public class McFunctionScriptBuilder extends BaseBuilder<McFunctionScript, McFunctionScriptBuilder> {
    public McFunctionScriptBuilder(ThingParser<McFunctionScriptBuilder> ownerParser, ResourceLocation registryName) {
        super(ownerParser, registryName);
    }

    public String function;
    public Boolean debug = false;
    public String item;
    public String block;
    public String hand;

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    @Override
    protected String getThingTypeDisplayName() {
        return "Minecraft Function Script";
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public void putItem(String item) {
        this.item = item;
    }

    public void putBlock(String block) {
        this.block = block;
    }

    public void putHand(String hand) {
        this.hand = hand;
    }

    @Nullable
    public String getFunction() {
        return getValue(function, McFunctionScriptBuilder::getFunction);
    }

    public void setClientLogic(JsonObject any) {
        JParse.begin(any).ifKey("item", val -> val.string().handle(this::putItem))
                .ifKey("block", val -> val.string().handle(this::putBlock))
                .ifKey("hand", val -> val.string().handle(this::putHand));
    }

    @Override
    protected McFunctionScript buildInternal() {
        return new McFunctionScript(getFunction(), debug, item, block, hand);
    }
}
