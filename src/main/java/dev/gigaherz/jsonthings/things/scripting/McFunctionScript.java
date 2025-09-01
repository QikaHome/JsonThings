package dev.gigaherz.jsonthings.things.scripting;

import java.util.ArrayList;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class McFunctionScript extends ThingScript {
    public final String function;
    public final Boolean debug;
    public static final Logger LOGGER = LogUtils.getLogger();

    public McFunctionScript(String string, Boolean debug) {
        this.function = string;
        this.debug = debug;
    }
    public InteractionResult getResult(int i)
    {
        switch(i)
        {
            case 0: return InteractionResult.PASS;
            case 1: return InteractionResult.SUCCESS;
            case 2: return InteractionResult.FAIL;
            case 3: return InteractionResult.CONSUME;
            default: return InteractionResult.PASS;
        }
    }

        public ItemInteractionResult getItemResult(int i)
    {
        switch(i)
        {
            case 0: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case 1: return ItemInteractionResult.SUCCESS;
            case 2: return ItemInteractionResult.FAIL;
            case 3: return ItemInteractionResult.CONSUME;
            default: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
            Player player = ((Player) context.get(FlexEventContext.USER));
            InteractionHand hand = context.get(FlexEventContext.HAND);
            if (hand == InteractionHand.OFF_HAND) {
                return player.getOffhandItem();
            }
            return player.getMainHandItem();
        }

        return getDefaultByEventType(event, context);
    }

    @Override
    public Object apply(FlexEventType event, FlexEventContext context) {
        LOGGER.debug("Executing mcfunction script: {}", function);
        Player player;
        if (context.get(FlexEventContext.USER) instanceof Player p)
            player = p;
        else
            return null;
        if (player.level() instanceof ServerLevel level) {
            BlockPos bpos = context.get(FlexEventContext.BLOCK_POS);
            Vec3 pos;
            if (bpos != null) {
                pos = new Vec3(bpos.getX(), bpos.getY(), bpos.getZ());
            } else {
                pos = new Vec3(player.getX(), player.getY(), player.getZ());
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
                        server, player),
                        "function " + function + " "
                                + String.format("{X:%f,Y:%f,Z:%f,Hand:%s,Name:%s}", pos.x, pos.y,
                                        pos.z, hand, player.getName().getString())); // I've tryed to pass Item Name,
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
        return getDefaultByEventType(event, context);
    }
}
