package com.teamofelectrorealism.electrorealism.screen.arc_furnace;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ArcFurnaceScreen extends AbstractContainerScreen<ArcFurnaceMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/arc_furnace/arc_furnace_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/crusher/electric_crusher_power.png");

    private static final ResourceLocation[] HEAT_TEXTURES = new ResourceLocation[8];

    static {
        for (int i = 0; i < 8; i++) {
            HEAT_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    ElectroRealism.MODID,
                    "textures/gui/arc_furnace/heat-" + i + ".png"
            );
        }
    }

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory playerInventory, Component title) {
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
        renderHeatAnimation(pGuiGraphics, x, y);
    }

    private void renderProgressPower(GuiGraphics pGuiGraphics, int x, int y) {
        int powerHeight = Mth.ceil(menu.getSmeltingProgress() * 13.0F) + 1; // Scale to max 14 pixels
        if (powerHeight > 0) {
            pGuiGraphics.blit(POWER_TEXTURE, x + 36, y + 36 + 14 - powerHeight, 0, 14 - powerHeight, 14, powerHeight, 14, 14);
        }
    }

    private void renderProgressArrow(GuiGraphics pGuiGraphics, int x, int y) {
        if(menu.isSmelting()) {
            int arrowWidth = Mth.ceil(menu.getHeatProgress() * 24.0F);
            pGuiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
        }
    }

    private void renderHeatAnimation(GuiGraphics pGuiGraphics, int x, int y) {
        int heatFrame = (int)((System.currentTimeMillis() / 1000) % 8); // Cycle every 1000ms
        RenderSystem.setShaderTexture(0, HEAT_TEXTURES[heatFrame]);

        pGuiGraphics.blit(
                HEAT_TEXTURES[heatFrame],
                x + 57, y + 18, // Adjust coordinates as needed
                0, 0,
                13, 13,
                13, 13
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
