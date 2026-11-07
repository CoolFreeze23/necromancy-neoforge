package com.sirolf2009.necromancy.command;

import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sirolf2009.necromancy.entity.MinionCensus;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.registry.NecroAttachments;
import com.sirolf2009.necromancy.registry.NecroAttachments.NecromancerData;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /minion: standing orders for your Minions, as in the original, plus a few modern helpers.
 * <ul>
 * <li>/minion set aggressive|passive - attack every player who is not a friend, or only enemies</li>
 * <li>/minion friend|enemy|forget &lt;player&gt;</li>
 * <li>/minion list - your orders and how many Minions you have</li>
 * <li>/minion sit|follow - order every loaded Minion of yours near you</li>
 * </ul>
 */
public final class MinionCommand {
    private MinionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minion")
                .then(Commands.literal("set")
                        .then(Commands.literal("aggressive").executes(c -> setAggressive(c, true)))
                        .then(Commands.literal("passive").executes(c -> setAggressive(c, false))))
                .then(Commands.literal("friend").then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(c -> relation(c, "friend"))))
                .then(Commands.literal("enemy").then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(c -> relation(c, "enemy"))))
                .then(Commands.literal("forget").then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(c -> relation(c, "forget"))))
                .then(Commands.literal("list").executes(MinionCommand::list))
                .then(Commands.literal("sit").executes(c -> order(c, true)))
                .then(Commands.literal("follow").executes(c -> order(c, false))));
    }

    private static int setAggressive(CommandContext<CommandSourceStack> context, boolean aggressive) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getData(NecroAttachments.NECROMANCER).setAggressive(aggressive);
        context.getSource().sendSuccess(() -> Component.translatable(aggressive ? "commands.necromancy.minion.aggressive" : "commands.necromancy.minion.passive"), false);
        return 1;
    }

    private static int relation(CommandContext<CommandSourceStack> context, String kind) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NecromancerData data = player.getData(NecroAttachments.NECROMANCER);
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        for (GameProfile profile : profiles) {
            UUID id = profile.getId();
            if (id.equals(player.getUUID())) {
                continue;
            }
            switch (kind) {
                case "friend" -> data.setFriend(id);
                case "enemy" -> data.setEnemy(id);
                default -> data.forget(id);
            }
            String name = profile.getName();
            context.getSource().sendSuccess(() -> Component.translatable("commands.necromancy.minion." + kind, name), false);
        }
        return profiles.size();
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NecromancerData data = player.getData(NecroAttachments.NECROMANCER);
        int count = MinionCensus.get(context.getSource().getServer()).count(player.getUUID());
        context.getSource().sendSuccess(() -> Component.translatable("commands.necromancy.minion.list.header", count,
                Component.translatable(data.aggressive() ? "commands.necromancy.minion.mode.aggressive" : "commands.necromancy.minion.mode.passive")), false);
        var cache = context.getSource().getServer().getProfileCache();
        data.relations().forEach((uuid, friend) -> {
            String name = cache == null ? uuid.toString() : cache.get(uuid).map(GameProfile::getName).orElse(uuid.toString());
            context.getSource().sendSuccess(() -> Component.translatable(friend ? "commands.necromancy.minion.list.friend" : "commands.necromancy.minion.list.enemy", name), false);
        });
        return count;
    }

    private static int order(CommandContext<CommandSourceStack> context, boolean sit) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var minions = player.serverLevel().getEntitiesOfClass(MinionEntity.class, player.getBoundingBox().inflate(48), m -> m.isOwnedBy(player));
        for (MinionEntity minion : minions) {
            minion.setOrderedToSit(sit);
            minion.getNavigation().stop();
            minion.setTarget(null);
        }
        int n = minions.size();
        context.getSource().sendSuccess(() -> Component.translatable(sit ? "commands.necromancy.minion.sit" : "commands.necromancy.minion.follow", n), false);
        return n;
    }
}
