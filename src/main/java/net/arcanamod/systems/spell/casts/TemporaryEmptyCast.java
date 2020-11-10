package net.arcanamod.systems.spell.casts;

import net.arcanamod.aspects.Aspect;
import net.arcanamod.aspects.Aspects;
import net.arcanamod.systems.spell.SpellCosts;
import net.arcanamod.systems.spell.SpellData;
import net.arcanamod.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;

import static net.arcanamod.util.Pair.of;

// Temporary class
public class TemporaryEmptyCast implements ICast {
	
	public ICast build(CompoundNBT compound) {
		return this;
	}
	
	@Override
	public Aspect getSpellAspect() {
		return Aspects.EMPTY;
	}

	@Override
	public SpellData getSpellData() {
		return new SpellData(Aspects.EMPTY,Aspects.EMPTY,Aspects.EMPTY, of(Aspects.EMPTY,Aspects.EMPTY),of(Aspects.EMPTY,Aspects.EMPTY));
	}

	@Override
	public SpellCosts getSpellCosts() {
		return new SpellCosts(0,0,0,0,0,0,0);
	}

	@Override
	public int getComplexity() {
		return 0;
	}

	@Override
	public int getSpellDuration() {
		return 0;
	}
	
	public void use(PlayerEntity player, Object sender, Pair<Aspect, Aspect> cast, Action action){
	
	}
	
	public ActionResultType use(PlayerEntity player, Object sender, Action action) {
		return null;
	}
}