package com.zarazeknemos;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ZarazekNemosClient implements ClientModInitializer {

    private enum State { IDLE, PLACE_WATER, PICKUP_WATER, SWITCH_TO_WEB, PLACE_WEB }

    // domyslnie wylaczony
    public static boolean enabled = false;
    private static boolean openConfig = false;

    private static State state = State.IDLE;
    private static int delay = 0;
    private static int webSlot = -1;

    // opoznienia w tickach po kazdym kroku
    public static int DELAY_AFTER_SWITCH_TO_WATER = 0;
    public static int DELAY_AFTER_PLACE_WATER = 0;
    public static int DELAY_AFTER_PICKUP = 0;
    public static int DELAY_AFTER_SWITCH_TO_WEB = 0;

    @Override
    public void onInitializeClient() {

        // komenda /zn config
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("zn")
                        .then(ClientCommandManager.literal("config")
                                .executes(ctx -> {
                                    openConfig = true; // ekran otwieramy w nastepnym ticku
                                    return 1;
                                }))));

        // otwieranie menu (po zamknieciu czatu)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfig) {
                openConfig = false;
                client.setScreen(new ZarazekNemosScreen());
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!enabled) return ActionResult.PASS;
            if (!world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.getMainHandStack().isOf(Items.COBWEB)) return ActionResult.PASS;

            BlockPos clicked = hitResult.getBlockPos();

            // klik prosto w dzwignie/guzik -> mod nic nie robi, dzwignia dziala normalnie
            if (isLeverOrButton(world.getBlockState(clicked))) return ActionResult.PASS;

            boolean found = isLeverOrButton(world.getBlockState(clicked.offset(hitResult.getSide())));

            if (!found) {
                // sprawdz kazda kratke, przez ktora przechodzi promien od oczu do trafionego bloku
                Vec3d eye = player.getEyePos();
                Vec3d look = player.getRotationVec(1.0f);
                double dist = eye.distanceTo(hitResult.getPos());
                for (double t = 0; t <= dist + 0.05; t += 0.02) {
                    BlockPos bp = BlockPos.ofFloored(eye.add(look.multiply(t)));
                    if (isLeverOrButton(world.getBlockState(bp))) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return ActionResult.PASS;

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

        // wykonuj kolejne kroki w tym samym ticku, dopoki delay = 0
        int guard = 0;
        while (state != State.IDLE && guard++ < 6) {
            if (delay > 0) {
                delay--;
                return;
            }
            step(client, player);
        }
    }

    private void step(MinecraftClient client, ClientPlayerEntity player) {
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

    private static boolean isLeverOrButton(BlockState s) {
        return s.isOf(Blocks.LEVER) || s.isIn(BlockTags.BUTTONS);
    }

    private static int findWaterInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
        }
        return -1;
    }
}
