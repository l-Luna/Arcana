package net.arcanamod.blocks.pipes;

import mcp.MethodsReturnNonnullByDefault;
import net.arcanamod.aspects.AspectStack;
import net.arcanamod.aspects.handlers.AspectHandler;
import net.arcanamod.blocks.tiles.ArcanaTiles;
import net.minecraft.block.BlockState;
import net.minecraft.block.SixWayBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TubeTileEntity extends TileEntity implements ITickableTileEntity{
	
	protected static final int MAX_SPECKS = 1000;
	
	List<AspectSpeck> specks = new ArrayList<>();
	
	public TubeTileEntity(){
		this(ArcanaTiles.ASPECT_TUBE_TE.get());
	}
	
	public TubeTileEntity(TileEntityType<?> type){
		super(type);
	}
	
	public void tick(){
		// Move every speck along by (speed / 20f).
		// If there is a connection in the speck's direction, keep moving it until its position exceeds 0.5f.
			// When it does, pass it to pipes or insert it into AspectHandlers.
		// If not, keep moving it until its position exceeds SIZE.
			// Then make it move in the direction of a connection.
			// Prefer down, then random horizontals, then up.
		// If a speck exceeds SIZE perpendicularly to their direction (how?), bring it back to the centre.
		// If there's too many specks (1000?), explode.
		if(specks.size() > MAX_SPECKS){
			specks.clear();
			// also explode or smth
		}
		List<AspectSpeck> toRemove = new ArrayList<>();
		for(AspectSpeck speck : specks){
			Direction dir = speck.direction;
			speck.pos += speck.speed / 20f;
			speck.stuck = false;
			BlockState state = getWorld().getBlockState(pos);
			boolean connected = state.get(SixWayBlock.FACING_TO_PROPERTY_MAP.get(dir));
			if(connected){
				TileEntity target = world.getTileEntity(pos.offset(dir));
				if(target instanceof TubeTileEntity){
					TubeTileEntity tube = (TubeTileEntity)target;
					if(!tube.enabled())
						connected = false;
				}else if(target != null){
					AspectHandler vis = AspectHandler.getFrom(target);
					// add up the available space (capacity - amount) of all holders
					// voiding holders are always considered to have 1 space available - we only check == 0 anyways
					if(vis != null && vis.getHolders().stream().mapToDouble(holder -> holder.voids() ? 1 : holder.getCapacity() - holder.getStack().getAmount()).sum() == 0)
						connected = false;
				}
			}
			float max = connected ? 1 : .5f;
			Optional<Direction> forcedDir = redirect(speck, connected);
			if(forcedDir.isPresent() && speck.pos >= .5f && speck.pos <= max){
				speck.direction = forcedDir.get();
				markDirty();
			}else if(speck.pos > max){
				markDirty();
				// transfer, pass, or bounce
				BlockPos dest = pos.offset(dir);
				TileEntity te = world.getTileEntity(dest);
				if(te instanceof TubeTileEntity && connected){
					TubeTileEntity tube = (TubeTileEntity)te;
					if(tube.enabled()){
						toRemove.add(speck);
						tube.addSpeck(speck);
						speck.pos = speck.pos % 1;
					}
				}else if(AspectHandler.getOptional(te).isPresent() && connected){
					float inserted = AspectHandler.getFrom(te).insert(speck.payload);
					if(inserted >= speck.payload.getAmount())
						toRemove.add(speck);
					else{
						speck.payload = new AspectStack(speck.payload.getAspect(), speck.payload.getAmount() - inserted);
						speck.direction = speck.direction.getOpposite();
						speck.pos = 1 - speck.pos;
						if(speck.payload.getAmount() < 0.5) // remove specks that can't output
							toRemove.add(speck);
					}
				}else if(!forcedDir.isPresent()){ // random bounce
					if(state.get(SixWayBlock.DOWN) && dir != Direction.UP)
						speck.direction = Direction.DOWN;
					else if(state.get(SixWayBlock.NORTH) || state.get(SixWayBlock.SOUTH) || state.get(SixWayBlock.EAST) || state.get(SixWayBlock.WEST)){
						List<Direction> directions = new ArrayList<>();
						if(state.get(SixWayBlock.NORTH)) directions.add(Direction.NORTH);
						if(state.get(SixWayBlock.SOUTH)) directions.add(Direction.SOUTH);
						if(state.get(SixWayBlock.EAST)) directions.add(Direction.EAST);
						if(state.get(SixWayBlock.WEST)) directions.add(Direction.WEST);
						// TODO: don't consider any full targets
						// move connected code to separate method
						// this causes them to sometimes jitter and bounce back on the next tick if they try to enter an adjacent full container
						if(!connected) directions.remove(dir); // if the target is full, don't consider it
						if(directions.size() > 1) directions.remove(dir.getOpposite()); // don't bounce back if possible
						speck.direction = directions.get(getWorld().rand.nextInt(directions.size()));
					}else if(state.get(SixWayBlock.UP))
						speck.direction = Direction.UP;
				}else // forced direction
					if(state.get(SixWayBlock.FACING_TO_PROPERTY_MAP.get(forcedDir.get())))
						speck.direction = forcedDir.get();
				
				if(!toRemove.contains(speck) && speck.direction == dir){
					// We can't output or redirect it
					speck.pos = 0.5f;
					speck.stuck = true;
				}
			}
		}
		specks.removeAll(toRemove);
	}
	
	protected Optional<Direction> redirect(AspectSpeck speck, boolean canPass){
		return Optional.empty();
	}
	
	public void addSpeck(AspectSpeck speck){
		// don't add specks that can't transfer
		if(speck.payload.getAmount() >= 0.5)
			specks.add(speck);
	}
	
	public List<AspectSpeck> getSpecks(){
		return specks;
	}
	
	public boolean enabled(){
		return true;
	}
	
	public CompoundNBT write(CompoundNBT compound){
		CompoundNBT tag = super.write(compound);
		ListNBT specks = new ListNBT();
		for(AspectSpeck speck : this.specks)
			specks.add(speck.toNbt());
		tag.put("specks", specks);
		return tag;
	}
	
	public void read(BlockState state, CompoundNBT nbt){
		super.read(state, nbt);
		ListNBT specksList = nbt.getList("specks", Constants.NBT.TAG_COMPOUND);
		specks.clear();
		for(INBT speckInbt : specksList){
			CompoundNBT speckTag = (CompoundNBT)speckInbt;
			specks.add(AspectSpeck.fromNbt(speckTag));
		}
	}
	
	public CompoundNBT getUpdateTag(){
		return write(new CompoundNBT());
	}
}