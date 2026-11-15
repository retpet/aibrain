package com.aibrain;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.List;

public class EntityAIBot extends EntityCreature {

    private static final int EPISODE_TICKS = 600; // 30 seconds
    private static final int BLOCKS_PER_TICK = 4;

    private float reward = 0f;
    private int episodeTicks = 0;
    private int attackCooldown = 0;
    private int mineCooldown = 0;
    private int searchCooldown = 0;
    private int idleBuildTimer = 100;
    private BlockPos focusPos = null;

    private List<Structures.Piece> buildQueue = null;
    private int buildIndex = 0;
    private BlockPos buildOrigin = null;
    private int buildType = 0;
    private int buildStyle = 0;

    public EntityAIBot(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        enablePersistence();
        setCustomNameTag("AI Bot");
        setAlwaysRenderNameTag(true);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0D);
    }

    // ------------------------------------------------------------ building

    /** forcedType: 0 house, 1 farm, 2 redstone, -1 = choose by taste. Returns false if already building. */
    public boolean requestBuild(int forcedType) {
        if (buildQueue != null) return false;
        int[] pick = Taste.choose(forcedType);
        buildType = pick[0];
        buildStyle = pick[1];
        buildQueue = Structures.build(buildType, buildStyle);
        buildIndex = 0;
        buildOrigin = new BlockPos(MathHelper.floor(posX) + 3, MathHelper.floor(posY) - 1, MathHelper.floor(posZ) + 3);
        notifyNearby("Building: " + Structures.NAMES[buildType] + " (style " + (buildStyle + 1) + ")");
        return true;
    }

    private void tickBuilding() {
        getNavigator().clearPath();
        for (int n = 0; n < BLOCKS_PER_TICK && buildIndex < buildQueue.size(); n++) {
            Structures.Piece p = buildQueue.get(buildIndex++);
            world.setBlockState(buildOrigin.add(p.dx, p.dy, p.dz), p.state, 3);
        }
        if (ticksExisted % 6 == 0) {
            swingArm(EnumHand.MAIN_HAND);
        }
        if (buildIndex >= buildQueue.size()) {
            Taste.lastType = buildType;
            Taste.lastStyle = buildStyle;
            buildQueue = null;
            idleBuildTimer = 600;
            BrainManager.save();
            notifyNearby("Done! Press J and choose Like or Dislike so I learn your taste.");
        }
    }

    private void notifyNearby(String msg) {
        EntityPlayer p = world.getClosestPlayerToEntity(this, 128.0D);
        if (p != null) {
            p.sendMessage(new TextComponentString(msg));
        }
    }

    // ------------------------------------------------------------ main brain loop

    /** The brain controls the bot here (runs on the server only). */
    @Override
    protected void updateAITasks() {
        super.updateAITasks();

        if (buildQueue != null) {
            tickBuilding();
            return;
        }

        BrainManager.Mode mode = BrainManager.getMode();
        Brain brain = BrainManager.getBrain();
        EntityPlayer player = world.getClosestPlayerToEntity(this, 64.0D);

        if (mode == BrainManager.Mode.CREATIVE && player != null && --idleBuildTimer <= 0) {
            requestBuild(-1);
            return;
        }

        // ---- what are we heading to: a block to mine (survival) or the player ----
        if (mode == BrainManager.Mode.SURVIVAL && !Progress.isDone()) {
            if (focusPos != null && !Progress.isTarget(world.getBlockState(focusPos).getBlock())) {
                focusPos = null;
            }
            if (focusPos == null && --searchCooldown <= 0) {
                searchCooldown = 40;
                focusPos = findTarget();
            }
        } else {
            focusPos = null;
        }

        boolean hasFocus = false;
        double fx = 0;
        double fz = 0;
        if (focusPos != null) {
            fx = focusPos.getX() + 0.5D;
            fz = focusPos.getZ() + 0.5D;
            hasFocus = true;
        } else if (player != null) {
            fx = player.posX;
            fz = player.posZ;
            hasFocus = true;
        }
        double focusDist = hasFocus ? Math.sqrt((fx - posX) * (fx - posX) + (fz - posZ) * (fz - posZ)) : 32.0D;

        EntityMob mob = null;
        double mobDistSq = 1.0E9D;
        for (EntityMob m : world.getEntitiesWithinAABB(EntityMob.class, getEntityBoundingBox().grow(16.0D))) {
            double d = getDistanceSq(m);
            if (d < mobDistSq) {
                mobDistSq = d;
                mob = m;
            }
        }

        // ---- inputs ("eyes") ----
        float[] in = new float[Brain.IN];
        if (hasFocus) {
            in[0] = (float) Math.min(focusDist, 32.0D) / 32f;
            in[1] = relAngle(fx, fz);
        } else {
            in[0] = 1f;
            in[1] = 0f;
        }
        if (mob != null) {
            in[2] = (float) Math.min(Math.sqrt(mobDistSq), 16.0D) / 16f;
            in[3] = relAngle(mob.posX, mob.posZ);
        } else {
            in[2] = 1f;
            in[3] = 0f;
        }
        in[4] = getHealth() / getMaxHealth();

        double rad = Math.toRadians(rotationYaw);
        BlockPos ahead = new BlockPos(posX - Math.sin(rad), posY, posZ + Math.cos(rad));
        in[5] = world.getBlockState(ahead).getMaterial().blocksMovement() ? 1f : 0f;

        float danger = 0f;
        BlockPos below = ahead.down();
        Material mat = world.getBlockState(below).getMaterial();
        if (mat == Material.LAVA || mat == Material.FIRE) {
            danger = 1f;
        } else if (!mat.blocksMovement() && !world.getBlockState(below.down()).getMaterial().blocksMovement()) {
            danger = 0.7f;
        }
        in[6] = danger;
        in[7] = onGround ? 1f : 0f;

        // ---- think ----
        float[] out = brain.think(in);

        // ---- actions ----
        rotationYaw += out[0] * 25f;
        rotationYawHead = rotationYaw;

        float speed = (out[1] + 1f) * 0.5f;
        if (speed > 0.05f) {
            double r = Math.toRadians(rotationYaw);
            getMoveHelper().setMoveTo(posX - Math.sin(r) * 2.0D, posY, posZ + Math.cos(r) * 2.0D, speed);
        }

        if (out[2] > 0.3f && onGround) {
            jump();
        }

        if (attackCooldown > 0) attackCooldown--;
        if (out[3] > 0.3f && attackCooldown <= 0 && mob != null && mobDistSq < 9.0D) {
            attackCooldown = 12;
            swingArm(EnumHand.MAIN_HAND);
            float dmg = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            mob.attackEntityFrom(DamageSource.causeMobDamage(this), dmg);
            reward += 0.5f;
            if (mob.getHealth() <= 0f) reward += 5f;
        }

        // ---- mining (survival instinct) ----
        if (mineCooldown > 0) mineCooldown--;
        if (focusPos != null && mineCooldown <= 0) {
            double dx = focusPos.getX() + 0.5D - posX;
            double dz = focusPos.getZ() + 0.5D - posZ;
            double dy = focusPos.getY() - posY;
            if (dx * dx + dz * dz < 9.0D && Math.abs(dy) < 5.0D) {
                mineCooldown = 15;
                swingArm(EnumHand.MAIN_HAND);
                if (world.destroyBlock(focusPos, true)) {
                    reward += 2f;
                    if (Progress.onMined()) {
                        notifyNearby("Stage complete! Next: " + Progress.describe());
                        BrainManager.save();
                    }
                }
                focusPos = null;
            }
        }

        // ---- reward ----
        if (hasFocus) {
            float d = (float) Math.min(focusDist, 20.0D);
            reward += (1f - d / 20f) * 0.01f;
        } else {
            reward -= 0.005f;
        }

        if (++episodeTicks >= EPISODE_TICKS) {
            finishEpisode();
        }
    }

    /** Nearest block that is useful for the current survival stage. */
    private BlockPos findTarget() {
        BlockPos me = new BlockPos(this);
        BlockPos best = null;
        double bestD = 1.0E9D;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -5; dy <= 6; dy++) {
                    BlockPos p = me.add(dx, dy, dz);
                    Block b = world.getBlockState(p).getBlock();
                    if (Progress.isTarget(b)) {
                        double d = dx * dx + dz * dz + dy * dy;
                        if (d < bestD) {
                            bestD = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    private float relAngle(double tx, double tz) {
        double dx = tx - posX;
        double dz = tz - posZ;
        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        return MathHelper.wrapDegrees(targetYaw - rotationYaw) / 180f;
    }

    private void finishEpisode() {
        BrainManager.getBrain().endEpisode(reward);
        BrainManager.save();
        reward = 0f;
        episodeTicks = 0;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (BrainManager.getMode() == BrainManager.Mode.CREATIVE && source != DamageSource.OUT_OF_WORLD) {
            return false; // invulnerable in creative mode
        }
        boolean hit = super.attackEntityFrom(source, amount);
        if (hit) reward -= amount * 0.5f;
        return hit;
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (!world.isRemote) {
            reward -= 20f;
            finishEpisode();
        }
        super.onDeath(cause);
    }
}
