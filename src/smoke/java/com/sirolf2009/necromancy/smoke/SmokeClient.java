package com.sirolf2009.necromancy.smoke;

import com.mojang.logging.LogUtils;
import com.sirolf2009.necromancy.Necromancy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * Dev-only client check that runs by itself in a hidden window. It creates a flat creative world,
 * reports missing block and item models and names, then plays a script file:
 * a line is a server command, or "wait N", "shot NAME", "hud on|off", "view 0|1|2", "screen NAME|none", or "report TEXT".
 * a line is a server command, or "wait N", "shot NAME", "hud on|off", "view 0|1|2", "report TEXT",
 * "useblock X Y Z" (the player right-clicks the block), "closegui", "inventory", "book" (toggles the
 * open furnace screen's recipe book), "tooltip on|off" (draws the held item's tooltip over the HUD) or "hitboxes on|off".
 */
@Mod(value = Necromancy.MOD_ID, dist = Dist.CLIENT)
public class SmokeClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD = "smoke";

    private final Deque<String> script = new ArrayDeque<>();
    private final List<String> report = new ArrayList<>();
    private Path dir;
    private int state;
    private int wait = 40;
    private boolean tooltip;

    public SmokeClient() {
        if (Boolean.getBoolean("necromancy.smoke")) {
            NeoForge.EVENT_BUS.addListener(this::onTick);
            NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        }
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (tooltip && mc.player != null) {
            event.getGuiGraphics().renderTooltip(mc.font, mc.player.getMainHandItem(), 40, 40);
        }
    }

    private void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            tick(mc);
        } catch (Exception e) {
            LOGGER.error("SMOKE: failed", e);
            report.add("EXCEPTION " + e);
            finish(mc);
        }
    }

    private String burstName;
    private int burstLeft;
    private int burstFrame;

    private void tick(Minecraft mc) throws IOException {
        if (burstLeft > 0) {
            // one frame every other tick (about 10 fps of game time) while a "burst" runs
            if (mc.level != null && mc.level.getGameTime() % 2 == 0) {
                Screenshot.grab(dir.toFile(), String.format("%s_%03d.png", burstName, burstFrame++), mc.getMainRenderTarget(), m -> {
                });
                burstLeft--;
            }
            return;
        }
        if (--wait > 0) return;
        switch (state) {
            case 0 -> {
                if (!(mc.screen instanceof TitleScreen)) {
                    wait = 20;
                    return;
                }
                dir = mc.gameDirectory.toPath().resolve("smoke-out");
                Files.createDirectories(dir);
                mc.options.pauseOnLostFocus = false;
                checkModels(mc);
                Path file = Path.of(System.getProperty("necromancy.smoke.script", "smoke-script.txt"));
                if (Files.exists(file)) {
                    script.addAll(Files.readAllLines(file));
                }
                createWorld(mc);
                state = 1;
                wait = 40;
            }
            case 1 -> {
                if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
                    wait = 20;
                    return;
                }
                mc.options.pauseOnLostFocus = false;
                state = 2;
                wait = 60;
            }
            case 2 -> {
                String line = null;
                while (!script.isEmpty() && (line == null || line.isBlank() || line.startsWith("#"))) {
                    line = script.poll();
                }
                if (line == null || line.isBlank() || line.startsWith("#")) {
                    finish(mc);
                    return;
                }
                run(mc, line.strip());
            }
            default -> {
            }
        }
    }

    private void run(Minecraft mc, String line) {
        String[] parts = line.split(" ", 2);
        switch (parts[0]) {
            case "wait" -> wait = Integer.parseInt(parts[1]);
            case "shot" -> {
                Screenshot.grab(dir.toFile(), parts[1] + ".png", mc.getMainRenderTarget(), m -> {
                });
                wait = 5;
            }
            case "burst" -> {
                // "burst NAME FRAMES": NAME_000.png ... at about 10 fps, for GIFs
                String[] args = parts[1].split(" ");
                burstName = args[0];
                burstLeft = Integer.parseInt(args[1]);
                burstFrame = 0;
                wait = 1;
            }
            case "hud" -> mc.options.hideGui = !parts[1].equals("on");
            case "view" -> mc.options.setCameraType(net.minecraft.client.CameraType.values()[Integer.parseInt(parts[1])]);
            case "report" -> report.add(parts[1]);
            case "screen" -> mc.setScreen(switch (parts[1]) {
                case "pause" -> new net.minecraft.client.gui.screens.PauseScreen(true);
                default -> null;
            });
            case "tooltip" -> tooltip = parts[1].equals("on");
            case "hitboxes" -> mc.getEntityRenderDispatcher().setRenderHitBoxes(parts[1].equals("on"));
            case "closegui" -> mc.player.closeContainer();
            case "inventory" -> mc.setScreen(new InventoryScreen(mc.player));
            case "book" -> {
                if (mc.screen instanceof AbstractFurnaceScreen<?> furnace) {
                    furnace.recipeBookComponent.toggleVisibility();
                    furnace.resize(mc, furnace.width, furnace.height);
                }
            }
            case "guide" -> com.sirolf2009.necromancy.client.NecroClientEvents.openGuide();
            case "raise" -> {
                // the player raises the Minion on the altar at X Y Z (as the Raise button does)
                String[] xyz = parts[1].split(" ");
                BlockPos pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                var server = mc.getSingleplayerServer();
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    if (player.serverLevel().getBlockEntity(pos) instanceof com.sirolf2009.necromancy.blockentity.AltarBlockEntity altar) {
                        report.add("raise " + parts[1] + " -> " + altar.tryRaise(player));
                    }
                });
                wait = 2;
            }
            case "menuslot" -> {
                // puts an item into slot I of the open menu on the server: "menuslot I namespace:item [count]"
                String[] args = parts[1].split(" ");
                int index = Integer.parseInt(args[0]);
                var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(args[1]));
                int count = args.length > 2 ? Integer.parseInt(args[2]) : 1;
                var server = mc.getSingleplayerServer();
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    player.containerMenu.getSlot(index).set(new ItemStack(item, count));
                    player.containerMenu.broadcastChanges();
                });
                wait = 1;
            }
            case "minionmenu" -> {
                // tame the nearest Minion to the player and open its screen the way a sneak + empty-hand use does
                var server = mc.getSingleplayerServer();
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    var minions = player.serverLevel().getEntitiesOfClass(com.sirolf2009.necromancy.entity.MinionEntity.class, player.getBoundingBox().inflate(8));
                    minions.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
                    if (!minions.isEmpty()) {
                        var minion = minions.get(0);
                        minion.tame(player);
                        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                                (id, inventory, p) -> new com.sirolf2009.necromancy.menu.MinionMenu(id, inventory, minion), minion.getDisplayName()),
                                buf -> buf.writeVarInt(minion.getId()));
                    }
                });
                wait = 10;
            }
            case "useitem" -> {
                // the player uses the held item on the top of a block, on the server
                String[] xyz = parts[1].split(" ");
                BlockPos pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                var server = mc.getSingleplayerServer();
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
                    var result = player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                    report.add("useitem " + parts[1] + " -> " + result);
                });
                wait = 2;
            }
            case "useblock" -> {
                String[] xyz = parts[1].split(" ");
                BlockPos pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                var server = mc.getSingleplayerServer();
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    var level = player.serverLevel();
                    level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                });
                wait = 2;
            }
            default -> {
                var server = mc.getSingleplayerServer();
                String command = line.startsWith("/") ? line.substring(1) : line;
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
                });
                wait = 2;
            }
        }
    }

    private void createWorld(Minecraft mc) {
        var settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        var options = new WorldOptions(12345L, false, false);
        Function<net.minecraft.core.RegistryAccess, net.minecraft.world.level.levelgen.WorldDimensions> dimensions =
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, options, dimensions, mc.screen);
    }

    private void checkModels(Minecraft mc) {
        BakedModel missing = mc.getModelManager().getMissingModel();
        int items = 0;
        int states = 0;
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            String ns = entry.getKey().location().getNamespace();
            if (!ns.equals(Necromancy.MOD_ID)) continue;
            items++;
            Item item = entry.getValue();
            ItemStack stack = new ItemStack(item);
            if (mc.getItemRenderer().getModel(stack, null, null, 0) == missing) {
                report.add("MISSING ITEM MODEL " + entry.getKey().location());
            }
            if (!I18n.exists(stack.getDescriptionId())) {
                report.add("MISSING ITEM NAME " + entry.getKey().location() + " " + stack.getDescriptionId());
            }
        }
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            String ns = entry.getKey().location().getNamespace();
            if (!ns.equals(Necromancy.MOD_ID)) continue;
            Block block = entry.getValue();
            if (!I18n.exists(block.getDescriptionId()) && block.asItem() != net.minecraft.world.item.Items.AIR) {
                report.add("MISSING BLOCK NAME " + entry.getKey().location() + " " + block.getDescriptionId());
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                states++;
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                if (mc.getBlockRenderer().getBlockModel(state) == missing) {
                    report.add("MISSING BLOCK MODEL " + state);
                }
            }
        }
        report.add("checked " + items + " items and " + states + " block states");
    }

    private void finish(Minecraft mc) {
        state = 3;
        try {
            Files.write(dir.resolve("report.txt"), report);
        } catch (IOException e) {
            LOGGER.error("SMOKE: report", e);
        }
        LOGGER.info("SMOKE: finished, {} report lines", report.size());
        mc.stop();
    }
}
