package com.zarazeknemos;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ZarazekNemosScreen extends Screen {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 100;

    private ButtonWidget onButton;
    private ButtonWidget offButton;

    public ZarazekNemosScreen() {
        super(Text.literal("ZarazekNemos"));
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        onButton = ButtonWidget.builder(Text.literal("Włącz"), b -> {
            ZarazekNemosClient.enabled = true;
            refreshButtons();
        }).dimensions(left + 10, top + 70, 85, 20).build();

        offButton = ButtonWidget.builder(Text.literal("Wyłącz"), b -> {
            ZarazekNemosClient.enabled = false;
            refreshButtons();
        }).dimensions(left + 105, top + 70, 85, 20).build();

        this.addDrawableChild(onButton);
        this.addDrawableChild(offButton);
        refreshButtons();
    }

    private void refreshButtons() {
        onButton.active = !ZarazekNemosClient.enabled;
        offButton.active = ZarazekNemosClient.enabled;
    }

    // Tlo i panel rysujemy tutaj, zeby przyciski byly na wierzchu
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int right = left + PANEL_W;
        int bottom = top + PANEL_H;

        // ramka i panel w stylu vanilla
        context.fill(left, top, right, bottom, 0xFF000000);
        context.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFFC6C6C6);
        context.fill(left + 1, top + 1, right - 1, top + 2, 0xFFFFFFFF);
        context.fill(left + 1, top + 1, left + 2, bottom - 1, 0xFFFFFFFF);
        context.fill(left + 1, bottom - 2, right - 1, bottom - 1, 0xFF555555);
        context.fill(right - 2, top + 1, right - 1, bottom - 1, 0xFF555555);

        int centerX = this.width / 2;

        // tytul
        Text title = Text.literal("ZarazekNemos");
        context.drawText(this.textRenderer, title,
                centerX - this.textRenderer.getWidth(title) / 2, top + 12, 0xFF404040, false);

        // status
        Text status = Text.literal("Mod: ").append(
                ZarazekNemosClient.enabled
                        ? Text.literal("Włączony").formatted(Formatting.DARK_GREEN)
                        : Text.literal("Wyłączony").formatted(Formatting.DARK_RED));
        context.drawText(this.textRenderer, status,
                centerX - this.textRenderer.getWidth(status) / 2, top + 40, 0xFF404040, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
