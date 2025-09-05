package dev.gigaherz.jsonthings.things.scripting.client;

import static dev.gigaherz.jsonthings.things.scripting.McFunctionScript.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

@OnlyIn(Dist.CLIENT)
public interface IClientLogic {
    public default Object getResult(FlexEventType<?> event, FlexEventContext context)
    {
        return getDefaultByEventType(event, context);
    }
    public static Map<ResourceLocation,Function<JsonObject,IClientLogic>> CLIENT_LOGICS = new HashMap<>();
    public static IClientLogic DEFAULT = new IClientLogic() { };
    public static void registerClientLogic(ResourceLocation rl, Function<JsonObject,IClientLogic> func)
    {
        CLIENT_LOGICS.put(rl, func);
    }
    public static void setup()
    {
        registerClientLogic(ResourceLocation.fromNamespaceAndPath("jsonthings","basic_compare"), BasicCompareLogic::new);
        registerClientLogic(ResourceLocation.fromNamespaceAndPath("jsonthings","value"), ForceValue::new);
    }
    public static IClientLogic getClientLogic(ResourceLocation rl, JsonObject data)
    {

        if(CLIENT_LOGICS.containsKey(rl))
        return CLIENT_LOGICS.get(rl).apply(data);
        LOGGER.error("No client logic found for {}, fall to default", rl);
        return IClientLogic.DEFAULT;
    }
    public static IClientLogic getClientLogic(String str, JsonObject data)
    {
        return getClientLogic(ResourceLocation.parse(str),data);
    }
}
