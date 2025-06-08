package cn.elytra.not_a_manual.anvil;

import cn.elytra.not_a_manual.anvil.mixin.ScreenAccessor;
import cn.elytra.not_a_manual.anvil.util.TFCReflect;
import com.mojang.datafixers.util.Either;
import net.dries007.tfc.client.screen.AnvilScreen;
import net.dries007.tfc.common.capabilities.VesselLike;
import net.dries007.tfc.common.items.VesselItem;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Alloy;
import net.dries007.tfc.util.Metal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod(NotAManualAnvil.MOD_ID)
public class NotAManualAnvil {

    public static final String MOD_ID = "not_a_manual_anvil";

    public static final Logger LOG = LoggerFactory.getLogger(NotAManualAnvil.class);

    public NotAManualAnvil() {
        LOG.info("Not A Manual Anvil!");
    }

    /**
     * The button clicked callback.
     *
     * @param screen the anvil screen instance
     */
    public static void onAutoAnvilButtonClicked(AnvilScreen screen) {
        AutoAnvilExecutor aae = new AutoAnvilExecutor(screen);
        try {
            aae.workout();
        } catch(Exception e) {
            trySendMessageToLocalPlayer(Component.translatable("not_a_manual_anvil.auto_anvil.fail", e.getMessage()));
        }
    }

    /**
     * Send a message to the local player if present.
     *
     * @param message the message to send
     */
    public static void trySendMessageToLocalPlayer(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player != null) {
            player.sendSystemMessage(message);
        }
    }

    @Mod.EventBusSubscriber(modid = NotAManualAnvil.MOD_ID, value = Dist.CLIENT)
    public static class ClientEventHandler {

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init event) {
            if(event.getScreen() instanceof AnvilScreen anvilScreen) {
                Button button = Button.builder(Component.translatable("not_a_manual_anvil.auto_anvil_button"),
                                (button1) -> NotAManualAnvil.onAutoAnvilButtonClicked(anvilScreen))
                        .pos(anvilScreen.getGuiLeft() + 10, anvilScreen.getGuiTop() + 10)
                        .size(16, 16)
                        .build();
                ((ScreenAccessor) anvilScreen).invokeAddRenderableWidget(button);
            }
        }

        @SubscribeEvent
        public static void onTooltip(RenderTooltipEvent.GatherComponents event) {
            ItemStack itemStack = event.getItemStack();
            List<Either<FormattedText, TooltipComponent>> tooltips = event.getTooltipElements();

            if(itemStack.getItem() instanceof VesselItem) {
                VesselLike vesselLike = VesselLike.get(itemStack);
                if(vesselLike == null) {
                    return;
                }

                if(vesselLike.mode() == VesselLike.Mode.INVENTORY) {
                    ItemStackHandler inventory = TFCReflect.getVesselItemInventory(vesselLike);

                    int capacity = TFCReflect.getVesselItemCapacity(vesselLike);
                    Alloy alloy = new Alloy(capacity);

                    ItemStackInventory inventoryForRecipes = new ItemStackInventory();
                    float maxTemperature = 0.0F;
                    for(int i = 0; i < 4; i++) {
                        ItemStack toMelt = inventory.getStackInSlot(i);
                        inventoryForRecipes.setStack(toMelt.copy());
                        HeatingRecipe recipe = HeatingRecipe.getRecipe(toMelt);
                        if(recipe != null) {
                            FluidStack melted = recipe.assembleFluid(inventoryForRecipes);
                            if(!melted.isEmpty()) {
                                melted.setAmount(melted.getAmount() * toMelt.getCount());
                            }
                            if(recipe.getTemperature() > maxTemperature) {
                                maxTemperature = recipe.getTemperature();
                            }
                            Metal metal = Metal.get(melted.getFluid());
                            if(metal != null) {
                                alloy.add(metal, melted.getAmount(), false);
                            }
                        }
                    }

                    MutableComponent heatTooltip = TFCConfig.CLIENT.heatTooltipStyle.get().formatColored(maxTemperature);

                    tooltips.add(Either.left(Component.translatable("not_a_manual_anvil.alloy_predict.title").withStyle(ChatFormatting.DARK_GREEN)));
                    tooltips.add(Either.left(Component.translatable("tfc.tooltip.item_melts_into", alloy.getAmount(), alloy.getResult().getDisplayName(), heatTooltip)));
                }
            }
        }

    }

}
