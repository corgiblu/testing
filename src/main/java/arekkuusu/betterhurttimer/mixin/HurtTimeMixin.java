package arekkuusu.betterhurttimer.mixin;

import arekkuusu.betterhurttimer.BHT;
import arekkuusu.betterhurttimer.BHTConfig;
import arekkuusu.betterhurttimer.api.BHTAPI;
import arekkuusu.betterhurttimer.api.capability.Hurt;
import arekkuusu.betterhurttimer.api.capability.HurtProvider;
import arekkuusu.betterhurttimer.api.capability.data.AttackInfo;
import arekkuusu.betterhurttimer.common.Events;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HurtTimeMixin extends Entity {

    @Shadow
    public int hurtTime;
    @Shadow
    public float hurtDir;
    public float preAttackedAtYaw;
    public int preHurtTime;
    public DamageSource preDamageSource;

    public HurtTimeMixin(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    public void attackEntityFromBefore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (this.hurtTime > 0) {
            this.preHurtTime = this.hurtTime;
        } else {
            this.preHurtTime = 0;
        }
        if (this.hurtDir > 0) {
            this.preAttackedAtYaw = this.hurtDir;
        } else {
            this.preAttackedAtYaw = 0;
        }
        //noinspection ConstantConditions
        BHT.getProxy().setPreHurtTime((LivingEntity) ((Object) this));
        this.preDamageSource = source;
    }

    @Redirect(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", ordinal = 0))
    public int attackResistantOverride(LivingEntity target, DamageSource source) {
        if (Events.isAttack(this.preDamageSource)) {
            Entity attacker = this.preDamageSource.getEntity();
            LazyOptional<Hurt> optional = HurtProvider.hurt(attacker);
            if (optional.isPresent()) {
                Hurt capability = optional.orElseThrow(UnsupportedOperationException::new);
                final AttackInfo attackInfo = capability.getMeleeMap().computeIfAbsent(target, BHTAPI.INFO_FUNCTION);
                if (attackInfo.override) {
                    attackInfo.override = false;
                    return target.invulnerableDuration;
                }
            }
        }
        return target.invulnerableTime;
    }

    @Inject(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;hurtTime:I", shift = At.Shift.AFTER))
    public void hurtResistantTime(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        this.invulnerableTime = BHTConfig.Runtime.DamageFrames.hurtResistantTime;
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V", ordinal = 2))
    public void turnOffSound(Level world, Entity entity, byte b) {
        if (b == 2 || b == 33 || b == 36 || b == 37) {
            if (this.preHurtTime == 0) {
                world.broadcastEntityEvent(entity, b);
            }
        }
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    public void playHurtSound(LivingEntity that, DamageSource source) {
        if (this.preHurtTime == 0) {
            this.playHurtSound(source);
        }
    }

    @Inject(method = "hurt", at = @At("TAIL"))
    public void attackEntityFromAfter(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (this.preHurtTime > 0) {
            this.hurtTime = this.preHurtTime;
        }
        if (this.preAttackedAtYaw > 0) {
            this.hurtDir = this.preAttackedAtYaw;
        }
    }

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    public void playHurtSound(DamageSource source, CallbackInfo info) {
        if (this.preHurtTime > 0) {
            info.cancel();
        }
    }

    @Shadow
    protected abstract void playHurtSound(DamageSource source);
}
