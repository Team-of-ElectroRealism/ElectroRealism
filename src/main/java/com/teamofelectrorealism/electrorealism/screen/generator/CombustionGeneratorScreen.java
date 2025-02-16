package com.teamofelectrorealism.electrorealism.screen.generator;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/generator/combustion_generator_gui.png");
    private static final ResourceLocation FIRE_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/lit_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/icons/icon_power.png");

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        renderProgressFire(pGuiGraphics, x, y);
        renderProgressPower(pGuiGraphics, x, y);
    }

    private void renderProgressPower(GuiGraphics pGuiGraphics, int x, int y) {
        int powerHeight = Mth.ceil(menu.getPowerProgress() * 13.0F) + 1; // Scale to max 14 pixels
        if (powerHeight > 0) {
            pGuiGraphics.blit(POWER_TEXTURE, x + 101, y + 43 + 14 - powerHeight, 0, 14 - powerHeight, 14, powerHeight, 14, 14);
        }
    }

    private void renderProgressFire(GuiGraphics guiGraphics, int x, int y) {
        int fireHeight = Mth.ceil(menu.getLitProgress() * 13.0F) + 1; // Scale to max 14 pixels
        if (fireHeight > 0) {
            guiGraphics.blit(FIRE_TEXTURE, x + 81, y + 25 + 14 - fireHeight, 0, 14 - fireHeight, 14, fireHeight, 14, 14);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
