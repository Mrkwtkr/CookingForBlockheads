package net.blay09.mods.cookingforblockheads.tile;

import com.google.common.collect.Lists;
import net.blay09.mods.cookingforblockheads.api.capability.CapabilityKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.capability.IKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.network.VanillaPacketHandler;
import net.blay09.mods.cookingforblockheads.registry.CookingRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.List;

public class TileMilkJar extends TileEntity {

	protected static final int MILK_CAPACITY = 8000;

	private static class MilkJarItemProvider implements IKitchenItemProvider {
		private final List<ItemStack> itemStacks = Lists.newArrayList();
		private final TileMilkJar tileMilkJar;
		private int milkUsed;

		public MilkJarItemProvider(TileMilkJar tileMilkJar) {
			this.tileMilkJar = tileMilkJar;
			itemStacks.add(new ItemStack(Items.MILK_BUCKET));
			itemStacks.addAll(CookingRegistry.getMilkItems());
		}

		@Override
		public void resetSimulation() {
			milkUsed = 0;
		}

		@Override
		public ItemStack useItemStack(int slot, int amount, boolean simulate, List<IKitchenItemProvider> inventories) {
			if(tileMilkJar.getMilkAmount() - milkUsed > amount * 1000) {
				if(getStackInSlot(slot).getItem() == Items.MILK_BUCKET) {
					if(!CookingRegistry.consumeItemStack(new ItemStack(Items.BUCKET), inventories, simulate)) {
						return null;
					}
				}
				if(simulate) {
					milkUsed += amount * 1000;
				} else {
					tileMilkJar.drain(amount * 1000);
				}
				return ItemHandlerHelper.copyStackWithSize(getStackInSlot(slot), amount);
			}
			return null;
		}

		@Override
		public ItemStack returnItemStack(ItemStack itemStack) {
			for (ItemStack providedStack : itemStacks) {
				if (ItemHandlerHelper.canItemStacksStackRelaxed(itemStack, providedStack)) {
					tileMilkJar.fill(1000);
					break;
				}
			}
			return null;
		}

		@Override
		public int getSlots() {
			return itemStacks.size();
		}

		@Override
		@Nonnull
		public ItemStack getStackInSlot(int slot) {
			return itemStacks.get(slot);
		}
	}

	private final MilkJarItemProvider itemProvider = new MilkJarItemProvider(this);
	protected float milkAmount;

	public void fill(int amount) {
		milkAmount = Math.min(MILK_CAPACITY, milkAmount + amount);
		VanillaPacketHandler.sendTileEntityUpdate(this);
	}

	public void drain(int amount) {
		milkAmount = Math.max(0, milkAmount - amount);
		VanillaPacketHandler.sendTileEntityUpdate(this);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setFloat("MilkAmount", milkAmount);
		return tagCompound;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		milkAmount = tagCompound.getFloat("MilkAmount");
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, new NBTTagCompound());
	}

	public float getMilkAmount() {
		return milkAmount;
	}

	public float getMilkCapacity() {
		return MILK_CAPACITY;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityKitchenItemProvider.CAPABILITY
				|| super.hasCapability(capability, facing);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityKitchenItemProvider.CAPABILITY) {
			return (T) itemProvider;
		}
		return super.getCapability(capability, facing);
	}
}
