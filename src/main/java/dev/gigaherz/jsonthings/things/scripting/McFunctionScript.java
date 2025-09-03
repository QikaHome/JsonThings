package dev.gigaherz.jsonthings.things.scripting;

import java.util.ArrayList;

import org.jline.utils.Log;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import ca.weblite.objc.Client;
import net.minecraft.world.entity.Entity;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.references.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class McFunctionScript extends ThingScript {
    public final String function;
    public final Boolean debug;

    public class ClientLogic {
        public ClientLogic(String item, String block, String hand) {
            this.item = item;
            this.block = block;
            this.hand = hand;
        }

        public String item = null;
        public String block = null;
        public String hand = null;

        public void putItemRequirement(String item) {
            this.item = item;
        }

        public void putBlockRequirement(String block) {
            this.block = block;
        }

        public void putHandRequirement(String hand) {
            this.hand = hand;
        }

        public Object getResult(FlexEventType event, FlexEventContext context) {
            if(item==null && block==null && hand==null)
                return getDefaultByEventType(event, context);
            Entity user = context.get(FlexEventContext.USER);
            Level level = user.level();
            if (!level.isClientSide)
                return getDefaultByEventType(event, context); // Dont call me plz
            InteractionHand hand = context.get(FlexEventContext.HAND);
            if (item != null) {
                ItemStack stack1 = context.get(FlexEventContext.STACK);
                Player player = user instanceof Player p ? p : null;
                ItemStack stack2 = player != null
                        ? hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem()
                        : null;
                Item ano = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item));
                LOGGER.debug("clientLogic item: {}", ano);
                boolean a = stack1 == null;
                boolean b = stack2 == null;
                LOGGER.debug("clientLogic item: {}, stack1: {}, stack2: {}", ano, stack1, stack2);
                if ((a || !stack1.is(ano)) && (b || !stack2.is(ano)))
                    return getDefaultByEventType(event, context);
            }
            if (hand != null) {
                if (hand == null)
                    return getDefaultByEventType(event, context);
                if (!hand.toString().equals(this.hand))
                    return getDefaultByEventType(event, context);
            }
            if (block != null) {
                BlockPos pos = context.get(FlexEventContext.BLOCK_POS);
                if (pos == null)
                    return getDefaultByEventType(event, context);
                Block one = level.getBlockState(pos).getBlock();
                Block ano = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(block));
                if (!one.equals(ano))
                    return getDefaultByEventType(event, context);
            }
            return getResultByEventType(event, context, 1);
        }
    }

    public final ClientLogic clientLogic;

    public static final Logger LOGGER = LogUtils.getLogger();

    public McFunctionScript(String string, Boolean debug, String item, String block, String hand) {
        this.function = string;
        this.debug = debug;
        this.clientLogic = new ClientLogic(item, block, hand);
    }

    public InteractionResult getResult(int i) {
        switch (i) {
            case 0:
                return InteractionResult.PASS;
            case 1:
                return InteractionResult.SUCCESS;
            case 2:
                return InteractionResult.FAIL;
            case 3:
                return InteractionResult.CONSUME;
            default:
                return InteractionResult.PASS;
        }
    }

    public ItemInteractionResult getItemResult(int i) {
        switch (i) {
            case 0:
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case 1:
                return ItemInteractionResult.SUCCESS;
            case 2:
                return ItemInteractionResult.FAIL;
            case 3:
                return ItemInteractionResult.CONSUME;
            default:
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    public Object getDefaultByEventType(FlexEventType event, FlexEventContext context) {
        if (event == FlexEventType.USE_BLOCK_WITH_ITEM)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (event == FlexEventType.USE_BLOCK_WITHOUT_ITEM || event == FlexEventType.BEFORE_DESTROY)
            return InteractionResult.PASS;
        if (event == FlexEventType.USE_ITEM_ON_AIR || event == FlexEventType.USE_ITEM_ON_BLOCK
                || event == FlexEventType.BEGIN_USING_ITEM || event == FlexEventType.END_USING)
            return new InteractionResultHolder<ItemStack>(InteractionResult.PASS,
                    context.get(FlexEventContext.STACK));
        if (event == FlexEventType.UPDATE)
            return context.get(FlexEventContext.STACK);
        return null;
    }

    public Object getResultByEventType(FlexEventType event, FlexEventContext context, Object o) {
        if (event == FlexEventType.USE_BLOCK_WITH_ITEM) {
            if (o instanceof Integer r)
                return getItemResult(r);
        }
        if (event == FlexEventType.USE_BLOCK_WITHOUT_ITEM || event == FlexEventType.BEFORE_DESTROY) {
            if (o instanceof Integer r)
                return getResult(r);
        }
        if (event == FlexEventType.USE_ITEM_ON_AIR || event == FlexEventType.USE_ITEM_ON_BLOCK
                || event == FlexEventType.BEGIN_USING_ITEM || event == FlexEventType.END_USING)
            return new InteractionResultHolder<ItemStack>(
                    (InteractionResult) getResultByEventType(FlexEventType.USE_BLOCK_WITHOUT_ITEM, context, o),
                    context.get(FlexEventContext.STACK));
        if (event == FlexEventType.UPDATE) {
            Player user = ((Player) context.get(FlexEventContext.USER));
            InteractionHand hand = context.get(FlexEventContext.HAND);
            if (hand == InteractionHand.OFF_HAND) {
                return user.getOffhandItem();
            }
            return user.getMainHandItem();
        }

        return getDefaultByEventType(event, context);
    }

    @Override
    public Object apply(FlexEventType event, FlexEventContext context) {
        Entity user;
        if (context.get(FlexEventContext.USER) instanceof Entity p)
            user = p;
        else
            return getDefaultByEventType(event, context);
        LOGGER.debug("Executing mcfunction script: {}, client: {}", function,
                user.level().isClientSide() ? "true" : "false");
        if (user.level() instanceof ServerLevel level) {
            BlockPos bpos = context.get(FlexEventContext.BLOCK_POS);
            Vec3 pos;
            if (bpos != null) {
                pos = new Vec3(bpos.getX(), bpos.getY(), bpos.getZ());
            } else {
                pos = new Vec3(user.getX(), user.getY(), user.getZ());
            }
            LOGGER.debug(event.toString());
            String hand = event != FlexEventType.USE_BLOCK_WITHOUT_ITEM
                    && context.get(FlexEventContext.HAND) == InteractionHand.OFF_HAND ? "weapon.offhand"
                            : "weapon.mainhand";
            LOGGER.debug(hand);
            MinecraftServer server = level.getServer();
            if (server != null) {
                ArrayList<Component> resultComponents = new ArrayList<>();
                server.getCommands().performPrefixedCommand(new CommandSourceStack(new CommandSource() {
                    @Override
                    public void sendSystemMessage(Component message) {
                        if (debug)
                            server.getPlayerList().broadcastSystemMessage(message, true);
                        // first is start info and last is the result
                        resultComponents.add(message);
                    }

                    @Override
                    public boolean acceptsSuccess() {
                        return true;
                    }

                    @Override
                    public boolean acceptsFailure() {
                        return true;
                    }

                    @Override
                    public boolean shouldInformAdmins() {
                        return false;
                    }
                }, pos, Vec2.ZERO, level, 4, "", Component.literal(""),
                        server, user),
                        "function " + function + " "
                                + String.format("{X:%f,Y:%f,Z:%f,Hand:%s,Name:%s}", pos.x, pos.y,
                                        pos.z, hand, user.getName().getString())); // I've tryed to pass Item Name,
                                                                                   // but something may went wrong
                                                                                   // with translation
                try {
                    Object result = null;
                    for (Component component : resultComponents) {
                        LOGGER.debug("Function {} ends with messages {}", function, component);
                    }
                    ComponentContents message = resultComponents.getLast().getContents();
                    if (message instanceof TranslatableContents tcontents) {
                        Object[] args = tcontents.getArgs();
                        result = (int) args[args.length - 1];
                    }

                    result = getResultByEventType(event, context, result);
                    LOGGER.debug(result.toString());
                    return result;
                } catch (Exception e) {
                    LOGGER.error("Error processing function result: {} Check your function returning please.");
                }
            }
        }
        Object result = clientLogic.getResult(event, context);
        LOGGER.debug("clientLogic result: {}", result.toString());
        return result;
    }
}
