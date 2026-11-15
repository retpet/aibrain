package com.aibrain;

import net.minecraft.block.material.Material;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityAIBot extends EntityCreature {

    private static final int EPISODE_TICKS = 600; // 30 seconds

    private float reward = 0f;
    private int episodeTicks = 0;
    private int attackCooldown = 0;

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

    /** The brain controls the bot here (runs on the server only). */
    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        Brain brain = BrainManager.getBrain();

        EntityPlayer player = world.getClosestPlayerToEntity(this, 64.0D);

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
        if (player != null) {
            in[0] = (float) Math.min(Math.sqrt(getDistanceSq(player)), 32.0D) / 32f;
            in[1] = relAngle(player);
        } else {
            in[0] = 1f;
            in[1] = 0f;
        }
        if (mob != null) {
            in[2] = (float) Math.min(Math.sqrt(mobDistSq), 16.0D) / 16f;
            in[3] = relAngle(mob);
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
            swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
            float dmg = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            mob.attackEntityFrom(DamageSource.causeMobDamage(this), dmg);
            reward += 0.5f;
            if (mob.getHealth() <= 0f) reward += 5f;
        }
        // ---- reward ----
        if (player != null) {
            float d = (float) Math.min(Math.sqrt(getDistanceSq(player)), 20.0D);
            reward += (1f - d / 20f) * 0.01f;
        } else {
            reward -= 0.005f;
        }

        if (++episodeTicks >= EPISODE_TICKS) {
            finishEpisode();
        }
    }

    private float relAngle(Entity target) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
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
