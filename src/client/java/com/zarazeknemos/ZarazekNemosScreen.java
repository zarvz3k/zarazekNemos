package com.zarazeknemos;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.IntConsumer;

public class ZarazekNemosScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 150;
    private static final int COL_W = 65;
    private static final int MAX_DELAY = 4;

    private static final String[] LABELS = {
            "Zmiana slota na wodę",
            "Wylanie wody",
            "Podniesienie wody",
            "Zmiana slota na web"
    };

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
        }).dimensions(left + 10, top + 58, 125, 20).build();

        offButton = ButtonWidget.builder(Text.literal("Wyłącz"), b -> {
            ZarazekNemosClient.enabled = false;
            refreshButtons();
        }).dimensions(left + 145, top + 58, 125, 20).build();

        this.addDrawableChild(onButton);
        this.addDrawableChild(offButton);
        refreshButtons();

        addDelayField(0, left, top, ZarazekNemosClient.DELAY_AFTER_SWITCH_TO_WATER,
                v -> ZarazekNemosClient.DELAY_AFTER_SWITCH_TO_WATER = v);
        addDelayField(1, left, top, ZarazekNemosClient.DELAY_AFTER_PLACE_WATER,
                v -> ZarazekNemosClient.DELAY_AFTER_PLACE_WATER = v);
        addDelayField(2, left, top, ZarazekNemosClient.DELAY_AFTER_PICKUP,
                v -> ZarazekNemosClient.DELAY_AFTER_PICKUP = v);
        addDelayField(3, left, top, ZarazekNemosClient.DELAY_AFTER_SWITCH_TO_WEB,
                v -> ZarazekNemosClient.DELAY_AFTER_SWITCH_TO_WEB = v);
    }

    private void addDelayField(int index, int left, int top, int current, IntConsumer setter) {
        int centerX = left + 10 + index * COL_W + COL_W / 2;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer,
                centerX - 15, top + 114, 30, 18, Text.literal(LABELS[index]));
        field.setMaxLength(1);
        field.setTextPredicate(s -> s.isEmpty() || s.matches("[0-" + MAX_DELAY + "]"));
        field.setText(String.valueOf(current));
        field.setChangedListener(s -> {
            int v = 0;
            if (!s.isEmpty()) {
                try {
                    v = Math.min(MAX_DELAY, Math.max(0, Integer.parseInt(s)));
                } catch (NumberFormatException ignored) {
                }
            }
            setter.accept(v);
        });
        this.addDrawableChild(field);
    }

    private void refreshButtons() {
        onButton.active = !ZarazekNemosClient.enabled;
        offButton.active = ZarazekNemosClient.enabled;
    }

    // Tlo i panel rysujemy tutaj, zeby widgety byly na wierzchu
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

        // podpisy nad polami delayu (zawijane do 2 linii)
        for (int i = 0; i < LABELS.length; i++) {
            int colCenter = left + 10 + i * COL_W + COL_W / 2;
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(LABELS[i]), COL_W - 4);
            int y = top + 90;
            for (OrderedText line : lines) {
                context.drawText(this.textRenderer, line,
                        colCenter - this.textRenderer.getWidth(line) / 2, y, 0xFF404040, false);
                y += 10;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
