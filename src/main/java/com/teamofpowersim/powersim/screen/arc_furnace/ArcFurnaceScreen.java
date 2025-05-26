package com.teamofpowersim.powersim.screen.arc_furnace;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.screen.AbstractModScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ArcFurnaceScreen extends AbstractModScreen<ArcFurnaceMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/arc_furnace/arc_furnace_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/icons/icon_power.png");

    private static final ResourceLocation[] HEAT_TEXTURES = new ResourceLocation[8];

    static {
        for (int i = 0; i < 8; i++) {
            HEAT_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    PowerSim.MODID,
                    "textures/gui/arc_furnace/heat-" + i + ".png"
            );
        }
    }

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return GUI_TEXTURE;
    }

    @Override
    protected void renderProgressWidgets(GuiGraphics guiGraphics, int x, int y, float partialTick, int mouseX, int mouseY) {
        renderProgressArrow(guiGraphics, x, y);
        renderProgressPower(guiGraphics, x, y);
        renderHeatAnimation(guiGraphics, x, y);
    }

    private void renderProgressPower(GuiGraphics pGuiGraphics, int x, int y) {
        int powerHeight = Mth.ceil(menu.getPowerProgress() * 13.0F) + 1; // Scale to max 14 pixels
        if (powerHeight > 0) {
            pGuiGraphics.blit(POWER_TEXTURE, x + 57, y + 54 + 14 - powerHeight, 0, 14 - powerHeight, 14, powerHeight, 14, 14);
        }
    }

    private void renderProgressArrow(GuiGraphics pGuiGraphics, int x, int y) {
        if(menu.isSmelting()) {
            int arrowWidth = Mth.ceil(menu.getSmeltingProgress() * 24.0F);
            pGuiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
        }
    }

    private void renderHeatAnimation(GuiGraphics pGuiGraphics, int x, int y) {
        int heatFrame = (int)(menu.getHeatProgress());
        RenderSystem.setShaderTexture(0, HEAT_TEXTURES[heatFrame]);

        pGuiGraphics.blit(
                HEAT_TEXTURES[heatFrame],
                x + 57, y + 18,
                0, 0,
                13, 13,
                13, 13
        );
    }
}
