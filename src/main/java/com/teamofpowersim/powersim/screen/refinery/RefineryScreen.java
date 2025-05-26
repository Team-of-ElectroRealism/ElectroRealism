package com.teamofpowersim.powersim.screen.refinery;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.screen.AbstractModScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class RefineryScreen extends AbstractModScreen<RefineryMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/crusher/electric_crusher_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/icons/icon_power.png");

    public RefineryScreen(RefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return GUI_TEXTURE;
    }

    @Override
    protected void renderProgressWidgets(GuiGraphics guiGraphics, int x, int y, float partialTick, int mouseX, int mouseY) {
        renderProgressArrow(guiGraphics, x, y);
        renderProgressPower(guiGraphics, x, y); // This will use the updated logic
    }

    private void renderProgressPower(GuiGraphics guiGraphics, int x, int y) {
        // Use getPowerDisplayStatus() which returns 1.0f if powered, 0.0f otherwise
        if (menu.getPowerDisplayStatus() == 1.0f) {
            // Render the full power icon (14 pixels high)
            // Adjust x + 57, y + 37 to match your refinery GUI layout for the power icon
            guiGraphics.blit(POWER_TEXTURE, x + 57, y + 37, 0, 0, 14, 14, 14, 14);
        }
        // If not powered, do nothing.
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isRefining()) { // isRefining now also checks if powered
            int arrowWidth = Mth.ceil(menu.getRefiningProgress() * 24.0F);
            if (arrowWidth > 0) {
                // Adjust x + 79, y + 34 to match your refinery GUI layout for the arrow
                guiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
            }
        }
    }
}
