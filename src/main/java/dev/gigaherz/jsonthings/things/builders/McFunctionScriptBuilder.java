package dev.gigaherz.jsonthings.things.builders;

import javax.annotation.Nullable;

import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.things.scripting.McFunctionScript;
import net.minecraft.resources.ResourceLocation;

public class McFunctionScriptBuilder extends BaseBuilder<McFunctionScript, McFunctionScriptBuilder>{
    public McFunctionScriptBuilder(ThingParser<McFunctionScriptBuilder> ownerParser, ResourceLocation registryName) {
        super(ownerParser, registryName);
    }
    public String function;
    @Override
    protected String getThingTypeDisplayName() {
        return "Minecraft Function Script";
    }
    public void setFunction(String function)
    {
        this.function = function;
    }
    @Nullable
    public String getFunction()
    {
        return getValue(function, McFunctionScriptBuilder::getFunction);
    }
    @Override
    protected McFunctionScript buildInternal() {
        return new McFunctionScript(getFunction());
    }
}
