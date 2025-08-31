package dev.gigaherz.jsonthings.things.scripting;

import java.util.ArrayList;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import net.minecraft.client.Minecraft;
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
    public static final Logger LOGGER = LogUtils.getLogger();

    public McFunctionScript(String string) {
        this.function = string;
    }

    public Object getDefaultByEventType(FlexEventType event, FlexEventContext context) {
        if (event == FlexEventType.USE_BLOCK_WITH_ITEM)
            return ItemInteractionResult.CONSUME;
        if (event == FlexEventType.USE_BLOCK_WITHOUT_ITEM || event == FlexEventType.BEFORE_DESTROY)
            return InteractionResult.CONSUME;
        if (event == FlexEventType.USE_ITEM_ON_AIR || event == FlexEventType.USE_ITEM_ON_BLOCK
                || event == FlexEventType.BEGIN_USING_ITEM || event == FlexEventType.END_USING)
            return new InteractionResultHolder<ItemStack>(InteractionResult.CONSUME,
                    context.get(FlexEventContext.STACK));
        if (event == FlexEventType.UPDATE)
            return context.get(FlexEventContext.STACK);
        return null;
    }

    public Object getResultByEventType(FlexEventType event, FlexEventContext context, Object o) {
        if (event == FlexEventType.USE_BLOCK_WITH_ITEM) {
            if (o == null)
                return ItemInteractionResult.CONSUME;
            if (o instanceof Integer r)
                return ItemInteractionResult.values()[r];
        }
        if (event == FlexEventType.USE_BLOCK_WITHOUT_ITEM || event == FlexEventType.BEFORE_DESTROY) {
            return ((ItemInteractionResult) getResultByEventType(FlexEventType.USE_BLOCK_WITH_ITEM, context, o))
                    .result();
        }
        if (event == FlexEventType.USE_ITEM_ON_AIR || event == FlexEventType.USE_ITEM_ON_BLOCK
                || event == FlexEventType.BEGIN_USING_ITEM || event == FlexEventType.END_USING)
            return new InteractionResultHolder<ItemStack>(
                    (InteractionResult) getResultByEventType(FlexEventType.USE_BLOCK_WITHOUT_ITEM, context, o),
                    context.get(FlexEventContext.STACK));
        if (event == FlexEventType.UPDATE)
        {
            Player player = ((Player) context.get(FlexEventContext.USER));
            InteractionHand hand = context.get(FlexEventContext.HAND);
            if(hand == InteractionHand.OFF_HAND)
            {
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
            String hand = context.get(FlexEventContext.HAND) == InteractionHand.OFF_HAND ? "weapon.offhand"
                    : "weapon.mainhand";
            MinecraftServer server = level.getServer();
            if (server != null) {
                ArrayList<Component> resultComponents = new ArrayList<>();
                server.getCommands().performPrefixedCommand(new CommandSourceStack(new CommandSource() {
                    @Override
                    public void sendSystemMessage(Component message) {
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
                        "function " + function + " " + String.format("{X:%f,Y:%f,Z:%f,Hand:%s,Name:%s}", pos.x, pos.y,
                                pos.z, hand, player.getName().getString()));
                Object result = null;
                ComponentContents message = resultComponents.getLast().getContents();
                // LOGGER.info(message.toString());
                try {
                    if (message instanceof TranslatableContents tcontents) {
                        Object[] args = tcontents.getArgs();
                        result = (int) args[args.length - 1];
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing function result: {} Check your function returning please.", message);
                }
                return getResultByEventType(event, context, result);
            }
        }
        return getDefaultByEventType(event, context);
    }
}
