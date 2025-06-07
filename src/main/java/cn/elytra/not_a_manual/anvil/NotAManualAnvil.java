package cn.elytra.not_a_manual.anvil;

import cn.elytra.not_a_manual.anvil.mixin.ScreenAccessor;
import net.dries007.tfc.client.screen.AnvilScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    }

}
