package com.sirolf2009.necromancy.api;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A creature whose body parts can be sewn and raised on the Summoning Altar. This is the 1.21.1 form of sirolf2009's
 * NecroEntityBase: everything the server needs lives here, the look of each part is registered on the client
 * ({@code com.sirolf2009.necromancy.client.model.NecroMobModels}).
 */
public class NecroMob {
    private final ResourceLocation id;
    private final Map<PartType, Supplier<? extends Item>> parts = new EnumMap<>(PartType.class);
    private final Map<PartType, Boolean> sewableParts = new EnumMap<>(PartType.class);
    private final java.util.Set<PartType> creatureParts = java.util.EnumSet.noneOf(PartType.class);
    /** Set by the item registry: the generic creature part item. */
    public static Supplier<? extends Item> CREATURE_PART_ITEM = () -> net.minecraft.world.item.Items.AIR;
    /** Set by the item registry: builds a creature part stack for a mob and type. */
    public static java.util.function.BiFunction<ResourceLocation, PartType, net.minecraft.world.item.ItemStack> CREATURE_PART_STACK =
            (mob, type) -> net.minecraft.world.item.ItemStack.EMPTY;
    private final Map<BodyPartLocation, PartStats> stats = new EnumMap<>(BodyPartLocation.class);
    private final Map<BodyPartLocation, PartStats> classicStats = new EnumMap<>(BodyPartLocation.class);
    private final Map<PartType, List<PartTrait>> traits = new EnumMap<>(PartType.class);
    private final Map<PartType, List<ExtraAttribute>> extraAttributes = new EnumMap<>(PartType.class);
    private Supplier<Ingredient> essence = () -> Ingredient.EMPTY;
    private final Map<PartType, Supplier<Ingredient>> essenceOverrides = new EnumMap<>(PartType.class);
    private boolean saddleable;
    private Mount mount = Mount.PIG;
    @Nullable
    private String nameKey;
    private float riderOffset;
    private Supplier<SoundEvent> ambientSound;
    private Supplier<SoundEvent> hurtSound;
    private Supplier<SoundEvent> deathSound;
    private int weight = 1;
    private float legHeight = 0.75F;
    private float torsoHeight = 0.75F;
    private float torsoWidth = 0.6F;

    public NecroMob(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    /** Translation key of the mob's display name, used in tooltips and the altar screen. */
    public String translationKey() {
        return nameKey != null ? nameKey : "necromob." + id.getNamespace() + "." + id.getPath();
    }

    // --- building ---

    /** A sewn part: it gets the default Sewing Machine recipe. */
    public NecroMob part(PartType type, Supplier<? extends Item> item) {
        parts.put(type, item);
        sewableParts.put(type, true);
        return this;
    }

    /**
     * A sewn part that has no item of its own: it is the generic {@code necromancy:creature_part} item carrying this mob
     * and part type (creatures of other mods, which can have hundreds of parts).
     */
    public NecroMob creaturePart(PartType type) {
        parts.put(type, CREATURE_PART_ITEM);
        sewableParts.put(type, true);
        creatureParts.add(type);
        return this;
    }

    /** A part that is found, not sewn (a vanilla mob head, Isaac's Head): no sewing recipe. */
    public NecroMob foundPart(PartType type, Supplier<? extends Item> item) {
        parts.put(type, item);
        sewableParts.put(type, false);
        return this;
    }

    public NecroMob stats(BodyPartLocation location, PartStats partStats) {
        stats.put(location, partStats);
        return this;
    }

    /** Stats for both arms at once. */
    public NecroMob armStats(PartStats partStats) {
        stats.put(BodyPartLocation.ARM_LEFT, partStats);
        stats.put(BodyPartLocation.ARM_RIGHT, partStats);
        return this;
    }

    /** The 1.7.1 numbers for a part whose stats the balance pass changed (used when balance.classicStats is on). */
    public NecroMob classicStats(BodyPartLocation location, PartStats partStats) {
        classicStats.put(location, partStats);
        if (location == BodyPartLocation.ARM_LEFT) {
            classicStats.put(BodyPartLocation.ARM_RIGHT, partStats);
        }
        return this;
    }

    /** A special ability of one part type (both arms share an arm trait). */
    public NecroMob trait(PartType type, PartTrait trait) {
        traits.computeIfAbsent(type, t -> new ArrayList<>()).add(trait);
        return this;
    }

    /** A modern attribute a part adds on top of the five classic stats (armour, scale, gravity, ...). */
    public NecroMob attribute(PartType type, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        extraAttributes.computeIfAbsent(type, t -> new ArrayList<>()).add(new ExtraAttribute(attribute, amount, operation));
        return this;
    }

    public NecroMob attribute(PartType type, Holder<Attribute> attribute, double amount) {
        return attribute(type, attribute, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** The mob-specific ingredient in the default sewing recipes (the original's initDefaultRecipes). */
    public NecroMob essence(Supplier<Ingredient> ingredient) {
        this.essence = ingredient;
        return this;
    }

    public NecroMob essence(PartType type, Supplier<Ingredient> ingredient) {
        essenceOverrides.put(type, ingredient);
        return this;
    }

    public NecroMob saddleable(float riderOffset) {
        this.saddleable = true;
        this.riderOffset = riderOffset;
        return this;
    }

    /** Name the mob with another translation key (a creature of another mod: its own entity name). */
    public NecroMob name(String translationKey) {
        this.nameKey = translationKey;
        return this;
    }

    /** A saddleable torso that rides differently from the pig: seats, water, direct steering, a jump bar. */
    public NecroMob mount(float riderOffset, Mount mount) {
        saddleable(riderOffset);
        this.mount = mount;
        return this;
    }

    public NecroMob sounds(Supplier<SoundEvent> ambient, Supplier<SoundEvent> hurt, Supplier<SoundEvent> death) {
        this.ambientSound = ambient;
        this.hurtSound = hurt;
        this.deathSound = death;
        return this;
    }

    /** How heavy each part of this mob is for a soul to animate (1 light to 4 very heavy). */
    public NecroMob weight(int weight) {
        this.weight = weight;
        return this;
    }

    /** Rough size of this mob's legs and torso in blocks, for the Minion's hitbox (biped defaults: 0.75, 0.75, 0.6). */
    public NecroMob shape(float legHeight, float torsoHeight, float torsoWidth) {
        this.legHeight = legHeight;
        this.torsoHeight = torsoHeight;
        this.torsoWidth = torsoWidth;
        return this;
    }

    // --- reading ---

    public boolean hasPart(PartType type) {
        return parts.containsKey(type);
    }

    public boolean isSewable(PartType type) {
        return sewableParts.getOrDefault(type, false);
    }

    /** Is this part the generic creature part item (rather than an item of its own)? */
    public boolean isCreaturePart(PartType type) {
        return creatureParts.contains(type);
    }

    /** One of this part, ready to drop or sell: its own item, or a creature part stack. */
    public net.minecraft.world.item.ItemStack partStack(PartType type) {
        if (creatureParts.contains(type)) {
            return CREATURE_PART_STACK.apply(id, type);
        }
        return partItem(type).map(net.minecraft.world.item.ItemStack::new).orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    public Optional<Item> partItem(PartType type) {
        Supplier<? extends Item> supplier = parts.get(type);
        return supplier == null ? Optional.empty() : Optional.of(supplier.get());
    }

    public Map<PartType, Supplier<? extends Item>> parts() {
        return parts;
    }

    public PartStats stats(BodyPartLocation location) {
        if (NecroBalance.classic()) {
            PartStats classic = classicStats.get(location);
            if (classic != null) {
                return classic;
            }
        }
        return stats.getOrDefault(location, PartStats.NONE);
    }

    public List<PartTrait> traits(PartType type) {
        return traits.getOrDefault(type, List.of());
    }

    public List<ExtraAttribute> extraAttributes(PartType type) {
        return extraAttributes.getOrDefault(type, List.of());
    }

    public Ingredient essence(PartType type) {
        Supplier<Ingredient> override = essenceOverrides.get(type);
        return override != null ? override.get() : essence.get();
    }

    public boolean isSaddleable() {
        return saddleable;
    }

    public Mount mount() {
        return mount;
    }

    public float riderOffset() {
        return riderOffset;
    }

    public int weight() {
        return weight;
    }

    @Nullable
    public SoundEvent ambientSound() {
        return ambientSound == null ? null : ambientSound.get();
    }

    @Nullable
    public SoundEvent hurtSound() {
        return hurtSound == null ? null : hurtSound.get();
    }

    @Nullable
    public SoundEvent deathSound() {
        return deathSound == null ? null : deathSound.get();
    }

    public float legHeight() {
        return legHeight;
    }

    public float torsoHeight() {
        return torsoHeight;
    }

    public float torsoWidth() {
        return torsoWidth;
    }

    /**
     * How a saddled torso rides. {@code direct}: steered with the movement keys like a horse (otherwise with a Brain on a
     * Stick, like a pig); {@code water}: full speed only in water; {@code jump}: strength of the horse jump bar (0 for none).
     */
    public record Mount(int seats, boolean water, boolean direct, float jump, float speedBonus) {
        public static final Mount PIG = new Mount(1, false, false, 0F, 0F);
    }

    public record ExtraAttribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    }
}
