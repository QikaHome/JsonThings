package dev.gigaherz.jsonthings;

import com.google.common.eventbus.Subscribe;
import com.mojang.logging.LogUtils;
import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.client.BlockColorHandler;
import dev.gigaherz.jsonthings.things.parsers.*;
import dev.gigaherz.jsonthings.things.scripting.ScriptParser;
import dev.gigaherz.jsonthings.util.CustomPackType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.NamedRenderTypeManager;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = JsonThings.MODID, bus = EventBusSubscriber.Bus.MOD)
@Mod(JsonThings.MODID)
public class JsonThings
{
    public static final String MODID = "jsonthings";
    public static final Logger LOGGER = LogUtils.getLogger();

    static
    {
        ThingRegistries.staticInit();
    }

    public static BlockParser blockParser;
    public static ItemParser itemParser;
    public static FluidParser fluidParser;
    public static FoodPropertiesParser foodPropertiesParser;
    public static ConsumableParser consumableParser;
    public static ShapeParser shapeParser;
    public static ToolMaterialParser toolMaterialParser;
    public static FluidTypeParser fluidTypeParser;
    public static ArmorMaterialParser armorMaterialParser;
    public static CreativeModeTabParser creativeModeTabParser;
    public static MobEffectInstanceParser mobEffectInstanceParser;
    public static BlockSetTypeParser blockSetTypeParser;
    public static SoundEventParser soundEventParser;
    public static SoundTypeParser soundTypeParser;

    public JsonThings(IEventBus bus)
    {
        var manager = ThingResourceManager.instance();
        //if (ModList.get().isLoaded("rhino"))
        {
            ScriptParser.enable(manager);
        }
        blockParser = manager.registerParser(new BlockParser(bus));
        itemParser = manager.registerParser(new ItemParser(bus));
        fluidParser = manager.registerParser(new FluidParser(bus));
        foodPropertiesParser = manager.registerParser(new FoodPropertiesParser());
        consumableParser = manager.registerParser(new ConsumableParser());
        shapeParser = manager.registerParser(new ShapeParser());
        toolMaterialParser = manager.registerParser(new ToolMaterialParser());
        fluidTypeParser = manager.registerParser(new FluidTypeParser(bus));
        armorMaterialParser = manager.registerParser(new ArmorMaterialParser(bus));
        creativeModeTabParser = manager.registerParser(new CreativeModeTabParser(bus));
        mobEffectInstanceParser = manager.registerParser(new MobEffectInstanceParser());
        blockSetTypeParser = manager.registerParser(new BlockSetTypeParser());
        soundEventParser = manager.registerParser(new SoundEventParser(bus));
        soundTypeParser = manager.registerParser(new SoundTypeParser());
    }

    private static CompletableFuture<ThingResourceManager> loaderFuture;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void construct(FMLConstructModEvent event)
    {
        event.enqueueWork(() -> {
            ThingResourceManager instance = ThingResourceManager.instance();

            ResourcePackLoader.populatePackRepository(instance.getRepository(), CustomPackType.THINGS, false);

            loaderFuture = instance.beginLoading();
        });
    }

    @SubscribeEvent
    public static void finishLoading(NewRegistryEvent event)
    {
        ThingResourceManager.instance().waitForLoading(loaderFuture);
        loaderFuture = null;
    }

    @SubscribeEvent
    public static void packFinder(AddPackFindersEvent event)
    {
        switch (event.getPackType())
        {
            case CLIENT_RESOURCES, SERVER_DATA -> event.addRepositorySource(ThingResourceManager.instance().getWrappedPackFinder(event.getPackType()));
            default -> {}
        }
    }

    @SubscribeEvent
    public static void validateThings(FMLCommonSetupEvent event)
    {
        ThingResourceManager.instance().validateAll();
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = JsonThings.MODID)
    public static class ClientHandlers
    {

        @SubscribeEvent
        public static void constructMod(FMLConstructModEvent event)
        {
            if (DatagenModLoader.isRunningDataGen()) return;

            event.enqueueWork(BlockColorHandler::init);

            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (mc, returnTo) -> {
                var thingPackManager = ThingResourceManager.instance();
                return new PackSelectionScreen(thingPackManager.getRepository(),
                        rpl -> {
                                Minecraft.getInstance().setScreen(returnTo);
                                thingPackManager.onConfigScreenSave();
                        }, thingPackManager.getThingPacksLocation(),
                        Component.literal("Thing Packs"));
            });
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event)
        {
            final ResourceLocation solid = ResourceLocation.withDefaultNamespace("solid");
            JsonThings.blockParser.getBuilders().forEach(thing -> {
                if (thing.isInErrorState()) return;
                ResourceLocation layer = thing.getDefaultRenderLayer();
                if (!layer.equals(solid))
                {
                    //noinspection deprecation
                    ItemBlockRenderTypes.setRenderLayer(thing.get().self(), NamedRenderTypeManager.get(layer).block());
                }
            });
            JsonThings.fluidParser.getBuilders().forEach(thing -> {
                if (thing.isInErrorState()) return;
                ResourceLocation layer = thing.getDefaultRenderLayer();
                for (var fluid : thing.getAllSiblings())
                {
                    if (!layer.equals(solid))
                    {
                        ItemBlockRenderTypes.setRenderLayer(fluid, NamedRenderTypeManager.get(layer).block());
                    }
                }
            });
        }

        @SubscribeEvent
        public static void blockColorHandlers(RegisterColorHandlersEvent.Block event)
        {
            JsonThings.blockParser.getBuilders().forEach(thing -> {
                String handlerName = thing.getColorHandler();
                if (handlerName != null)
                {
                    BlockColor bc = BlockColorHandler.get(handlerName);
                    event.register(bc, thing.get().self());
                }
            });
        }

        @SubscribeEvent
        public static void clientProperties(RegisterClientExtensionsEvent event)
        {
            JsonThings.fluidTypeParser.getBuilders().forEach(thing -> {
                var color = Objects.requireNonNullElse(thing.getColor(), 0xFFFFFFFF);
                var stillTexture = thing.getStillTexture();
                var flowingTexture = thing.getFlowingTexture();
                var sideTexture = thing.getSideTexture();

                event.registerFluidType(new IClientFluidTypeExtensions()
                {
                    @Override
                    public int getTintColor()
                    {
                        return color;
                    }

                    @Override
                    public ResourceLocation getStillTexture()
                    {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture()
                    {
                        return flowingTexture;
                    }

                    @Override
                    public @Nullable ResourceLocation getOverlayTexture()
                    {
                        return sideTexture;
                    }
                }, thing.get());
            });
        }
    }
}
