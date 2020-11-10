package net.arcanamod.systems.spell.casts;

import net.arcanamod.ArcanaVariables;
import net.arcanamod.aspects.Aspect;
import net.arcanamod.aspects.Aspects;
import net.arcanamod.effects.ArcanaEffects;
import net.arcanamod.systems.spell.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LifeCast extends Cast {
	
	public ICast build(SpellData data, CompoundNBT compound) {
		this.data = data;
		isBuilt = true;
		return this;
	}

	@Override
	public ResourceLocation getId() {
		return ArcanaVariables.arcLoc("life");
	}
	
	public ICast build(CompoundNBT compound){
		return null;
	}
	
	@Override
	public Aspect getSpellAspect() {
		return Aspects.LIFE;
	}

	@Override
	public SpellData getSpellData() {
		return data;
	}

	@Override
	public SpellCosts getSpellCosts() {
		return new SpellCosts(0,0,1,1,0,0,0);
	}

	@Override
	public int getComplexity() {
		if (!isBuilt) return -2;
		return  8
				+ SpellValues.getOrDefault(data.firstModifier,0)
				+ SpellValues.getOrDefault(data.secondModifier,0)
				+ SpellValues.getOrDefault(data.sinModifier,0)
				+ SpellValues.getOrDefault(data.primaryCast.getSecond(),0)/2
				+ SpellValues.getOrDefault(data.plusCast.getSecond(),0)/2;
	}

	@Override
	public int getSpellDuration() {
		return 1;
	}

	public int getWardingDuration() throws SpellNotBuiltError {
		if (!isBuilt) throw new SpellNotBuiltError();
		return SpellValues.getOrDefault(data.firstModifier, 10);
	}

	public int getAmplifier() throws SpellNotBuiltError {
		if (!isBuilt) throw new SpellNotBuiltError();
		return SpellValues.getOrDefault(data.secondModifier, 1);
	}

	@Override
	public ActionResultType useOnEntity(PlayerEntity caster, Entity targetEntity) {
		try {
			if (targetEntity instanceof LivingEntity)
				((LivingEntity)targetEntity).addPotionEffect(new EffectInstance(ArcanaEffects.VICTUS.get(),getWardingDuration(),getAmplifier(),false,true));
		} catch (SpellNotBuiltError spellNotBuiltError) {
			spellNotBuiltError.printStackTrace();
		}
		return ActionResultType.FAIL;
	}

	@Override
	public ActionResultType useOnBlock(PlayerEntity caster, World world, BlockPos blockTarget) {

		return ActionResultType.SUCCESS;
	}

	@Override
	public ActionResultType useOnPlayer(PlayerEntity playerTarget) {
		try {
			playerTarget.addPotionEffect(new EffectInstance(ArcanaEffects.WARDING.get(),getWardingDuration(),getAmplifier(),false,true));
		} catch (SpellNotBuiltError spellNotBuiltError) {
			spellNotBuiltError.printStackTrace();
		}
		return ActionResultType.FAIL;
	}
}