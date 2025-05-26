package com.teamofpowersim.powersim.screen.crusher;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamofpowersim.powersim.PowerSim;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ElectricCrusherScreen extends AbstractContainerScreen<ElectricCrusherMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/crusher/electric_crusher_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/icons/icon_power.png");

    public ElectricCrusherScreen(ElectricCrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        // Gets rid of labels
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        pGuiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(pGuiGraphics, x, y);
        renderProgressPower(pGuiGraphics, x, y);
    }

    private void renderProgressPower(GuiGraphics pGuiGraphics, int x, int y) {
        if (menu.getPowerDisplayStatus() == 1.0f) {
            pGuiGraphics.blit(POWER_TEXTURE, x + 57, y + 37, 0, 0, 14, 14, 14, 14);
        }
    }

    private void renderProgressArrow(GuiGraphics pGuiGraphics, int x, int y) {
        if(menu.isCrushing()) { // isCrushing now also checks if powered
            int arrowWidth = Mth.ceil(menu.getCrushingProgress() * 24.0F);
            if (arrowWidth > 0) { // Only blit if there's some width
                pGuiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
