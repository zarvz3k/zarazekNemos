package com.zarazeknemos;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class ZarazekNemosClient implements ClientModInitializer {

    private enum State { IDLE, PLACE_WATER, PICKUP_WATER, SWITCH_TO_WEB, PLACE_WEB }

    private static State state = State.IDLE;
    private static int delay = 0;
    private static int webSlot = -1;

    // opoznienia w tickach po kazdym kroku
    private static final int DELAY_AFTER_SWITCH_TO_WATER = 1;
    private static final int DELAY_AFTER_PLACE_WATER = 1;
    private static final int DELAY_AFTER_PICKUP = 1;
    private static final int DELAY_AFTER_SWITCH_TO_WEB = 1;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.getMainHandStack().isOf(Items.COBWEB)) return ActionResult.PASS;

            BlockState target = world.getBlockState(hitResult.getBlockPos());
            if (!(target.isOf(Blocks.LEVER) || target.isIn(BlockTags.BUTTONS))) return ActionResult.PASS;

            if (state != State.IDLE) return ActionResult.FAIL;

            int water = findWaterInHotbar(player);
            if (water == -1) return ActionResult.PASS;

            webSlot = player.getInventory().selectedSlot;
            player.getInventory().selectedSlot = water;
            state = State.PLACE_WATER;
            delay = DELAY_AFTER_SWITCH_TO_WATER;

            return ActionResult.FAIL; // nie przelaczaj dzwigni
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (state == State.IDLE) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            reset();
            return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        switch (state) {
            case PLACE_WATER -> {
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                state = State.PICKUP_WATER;
                delay = DELAY_AFTER_PLACE_WATER;
            }
            case PICKUP_WATER -> {
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                state = State.SWITCH_TO_WEB;
                delay = DELAY_AFTER_PICKUP;
            }
            case SWITCH_TO_WEB -> {
                player.getInventory().selectedSlot = webSlot;
                state = State.PLACE_WEB;
                delay = DELAY_AFTER_SWITCH_TO_WEB;
            }
            case PLACE_WEB -> {
                HitResult hit = client.crosshairTarget;
                if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, bhr);
                    player.swingHand(Hand.MAIN_HAND);
                }
                reset();
            }
            default -> reset();
        }
    }

    private static void reset() {
        state = State.IDLE;
        delay = 0;
    }

    private static int findWaterInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
        }
        return -1;
    }
}
