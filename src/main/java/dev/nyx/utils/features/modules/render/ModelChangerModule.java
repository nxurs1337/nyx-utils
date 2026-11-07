package dev.nyx.utils.features.modules.render;

import dev.nyx.utils.features.modules.Module;
import dev.nyx.utils.features.modules.render.model.GlbModel;
import dev.nyx.utils.features.modules.render.model.ModelLibrary;
import dev.nyx.utils.features.settings.Setting;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws you, other players and/or mobs as a different mob. Purely visual and client side:
 * every target gets a look-alike dummy that copies its position, rotation and walk animation.
 */
public class ModelChangerModule extends Module {
    public enum Model {
        PIG(EntityType.PIG), COW(EntityType.COW), SHEEP(EntityType.SHEEP), CHICKEN(EntityType.CHICKEN),
        RABBIT(EntityType.RABBIT), FOX(EntityType.FOX), WOLF(EntityType.WOLF), CAT(EntityType.CAT),
        PANDA(EntityType.PANDA), POLAR_BEAR(EntityType.POLAR_BEAR), GOAT(EntityType.GOAT), CAMEL(EntityType.CAMEL),
        SNIFFER(EntityType.SNIFFER), ARMADILLO(EntityType.ARMADILLO), AXOLOTL(EntityType.AXOLOTL), FROG(EntityType.FROG),
        TURTLE(EntityType.TURTLE), PARROT(EntityType.PARROT), BEE(EntityType.BEE), ALLAY(EntityType.ALLAY),
        VILLAGER(EntityType.VILLAGER), WITCH(EntityType.WITCH), IRON_GOLEM(EntityType.IRON_GOLEM),
        SNOW_GOLEM(EntityType.SNOW_GOLEM), COPPER_GOLEM(EntityType.COPPER_GOLEM), CREEPER(EntityType.CREEPER),
        ZOMBIE(EntityType.ZOMBIE), SKELETON(EntityType.SKELETON), SPIDER(EntityType.SPIDER), ENDERMAN(EntityType.ENDERMAN),
        PIGLIN(EntityType.PIGLIN), BLAZE(EntityType.BLAZE), SLIME(EntityType.SLIME), WARDEN(EntityType.WARDEN),
        HAPPY_GHAST(EntityType.HAPPY_GHAST), GHAST(EntityType.GHAST),
        /** A .glb model from the model library, chosen with the Custom setting. */
        CUSTOM(null);

        public final EntityType<?> type;

        Model(EntityType<?> type) {
            this.type = type;
        }
    }

    private static ModelChangerModule INSTANCE;

    public final Setting<Model> model = mode("Model", Model.PIG)
            .withPicker(() -> mc.setScreen(new ModelPickerScreen(mc.screen, this)));
    public final Setting<String> custom = str("Custom", "tung_tung_tung_sahur");
    public final Setting<Integer> customYaw = num("CustomYaw", 0, 0, 359);
    public final Setting<Float> customSize = num("CustomSize", 1.0f, 0.3f, 3.0f);
    public final Setting<Boolean> self = bool("Self", true);
    public final Setting<Boolean> players = bool("Players", true);
    public final Setting<Boolean> hostile = bool("Hostile", false);
    public final Setting<Boolean> passive = bool("Passive", false);
    public final Setting<Boolean> neutral = bool("Neutral", false);
    public final Setting<Boolean> names = bool("PlayerNames", true);

    private final Map<Entity, Entity> dummies = new WeakHashMap<>();
    /** Render states (one per entity per frame) that should be drawn with the custom model. */
    private static final Map<EntityRenderState, GlbModel> CUSTOM_STATES = new WeakHashMap<>();
    private Level dummyLevel;
    private Model dummyModel;

    public ModelChangerModule() {
        super("ModelChanger", "Shows you, players or mobs as another mob (only on your screen)", Category.RENDER);
        INSTANCE = this;
        custom.setVisibility(v -> model.getValue() == Model.CUSTOM);
        customYaw.setVisibility(v -> model.getValue() == Model.CUSTOM);
        customSize.setVisibility(v -> model.getValue() == Model.CUSTOM);
    }

    public static ModelChangerModule getInstance() {
        return INSTANCE;
    }

    /** Picks a mob model (from the picker). */
    public void select(Model choice) {
        model.setValue(choice);
        if (!isEnabled()) enable();
    }

    /** Picks a custom .glb model (from the picker). */
    public void selectCustom(String name) {
        custom.setValue(name);
        model.setValue(Model.CUSTOM);
        if (!isEnabled()) enable();
    }

    /** Called right after an entity's render state was extracted: remembers which states get a custom model. */
    public static void tag(Entity entity, EntityRenderState state) {
        ModelChangerModule module = INSTANCE;
        GlbModel glb = null;
        if (module != null && module.isEnabled() && module.model.getValue() == Model.CUSTOM
                && entity instanceof LivingEntity living && module.targets(living)) {
            glb = ModelLibrary.get(module.custom.getValue());
        }
        if (glb != null) CUSTOM_STATES.put(state, glb);
        else CUSTOM_STATES.remove(state);
    }

    public static GlbModel customModel(EntityRenderState state) {
        return CUSTOM_STATES.get(state);
    }

    @Override
    public void onDisable() {
        dummies.clear();
    }

    @Override
    public String getDisplayInfo() {
        return model.getValue() == Model.CUSTOM ? custom.getValue() : model.getValue().name();
    }

    /** Called for every entity about to be drawn: returns the entity itself or its look-alike. */
    public static Entity substitute(Entity entity) {
        ModelChangerModule module = INSTANCE;
        if (module == null || !module.isEnabled() || !(entity instanceof LivingEntity living)) return entity;
        if (!module.targets(living)) return entity;
        Model model = module.model.getValue();
        if (model == Model.CUSTOM) return entity; // drawn in place by CustomModelRenderer
        if (entity.getType() == model.type) return entity;
        Entity dummy = module.dummyFor(entity, model);
        if (dummy == null) return entity;
        module.copy(entity, dummy);
        return dummy;
    }

    private boolean targets(LivingEntity entity) {
        if (entity instanceof ArmorStand) return false;
        if (entity == mc.player) return self.getValue();
        if (entity instanceof Player) return players.getValue();
        if (entity instanceof Enemy) return hostile.getValue();
        if (entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AgeableWaterCreature
                || entity instanceof AmbientCreature) return passive.getValue();
        return neutral.getValue();
    }

    private Entity dummyFor(Entity entity, Model model) {
        if (dummyLevel != entity.level() || dummyModel != model) {
            dummies.clear();
            dummyLevel = entity.level();
            dummyModel = model;
        }
        Entity dummy = dummies.get(entity);
        if (dummy == null) {
            dummy = model.type.create(entity.level(), EntitySpawnReason.LOAD);
            if (dummy == null) return null;
            dummies.put(entity, dummy);
        }
        return dummy;
    }

    private void copy(Entity from, Entity to) {
        to.setPos(from.getX(), from.getY(), from.getZ());
        to.xo = from.xo;
        to.yo = from.yo;
        to.zo = from.zo;
        to.xOld = from.xOld;
        to.yOld = from.yOld;
        to.zOld = from.zOld;
        to.setYRot(from.getYRot());
        to.yRotO = from.yRotO;
        to.setXRot(from.getXRot());
        to.xRotO = from.xRotO;
        to.tickCount = from.tickCount;
        to.setOnGround(from.onGround());
        to.setShiftKeyDown(from.isShiftKeyDown());
        to.setInvisible(from.isInvisible());

        if (from instanceof LivingEntity a && to instanceof LivingEntity b) {
            b.yBodyRot = a.yBodyRot;
            b.yBodyRotO = a.yBodyRotO;
            b.yHeadRot = a.yHeadRot;
            b.yHeadRotO = a.yHeadRotO;
            b.hurtTime = a.hurtTime;
            b.swinging = a.swinging;
            b.attackAnim = a.attackAnim;
            b.oAttackAnim = a.oAttackAnim;
            b.walkAnimation.speedOld = a.walkAnimation.speedOld;
            b.walkAnimation.speed = a.walkAnimation.speed;
            b.walkAnimation.position = a.walkAnimation.position;
        }

        // keep other players recognisable
        boolean showName = names.getValue() && from instanceof Player && from != mc.player;
        to.setCustomName(showName ? from.getDisplayName() : null);
        to.setCustomNameVisible(showName);
    }
}
