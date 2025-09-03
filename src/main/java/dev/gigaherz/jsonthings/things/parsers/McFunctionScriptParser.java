package dev.gigaherz.jsonthings.things.parsers;

import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.logging.log4j.core.script.Script;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.builders.McFunctionScriptBuilder;
import dev.gigaherz.jsonthings.things.scripting.ScriptParser;
import dev.gigaherz.jsonthings.things.scripting.ThingScript;
import dev.gigaherz.jsonthings.util.parse.JParse;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;

public class McFunctionScriptParser extends ThingParser<McFunctionScriptBuilder> {

    public McFunctionScriptParser() {
        super(GSON, "mcfunction_script");
    }
    @Override
    public McFunctionScriptBuilder processThing(ResourceLocation key, JsonObject data,
            Consumer<McFunctionScriptBuilder> builderModification) {
        final McFunctionScriptBuilder builder = new McFunctionScriptBuilder(this, key);
        JParse.begin(data)
                .key("function", val -> val.string().handle(builder::setFunction))
                .ifKey("debug", val -> val.bool().handle(builder::setDebug))
                .ifKey("client_logic", val -> val.obj().raw(obj -> builder.setClientLogic(obj)));
        builderModification.accept(builder);
        ScriptParser.instance().putScript(key, builder.build());
        return builder;
    }
}
