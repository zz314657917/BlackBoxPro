package com.blackboxpro.forge.dispatcher

import com.blackboxpro.common.action.ActionCatalog
import com.blackboxpro.common.action.composite.BatchAction
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.movement.*
import com.blackboxpro.forge.action.block.*
import com.blackboxpro.forge.action.entity.*
import com.blackboxpro.forge.action.container.*
import com.blackboxpro.forge.action.player.*
import com.blackboxpro.forge.action.chat.*
import com.blackboxpro.forge.action.client.*
import com.blackboxpro.forge.action.advanced.*
import com.blackboxpro.forge.action.debug.*
import com.blackboxpro.forge.action.composite.*
import com.blackboxpro.forge.action.query.*
import com.blackboxpro.runtime.bindings.LoggerSupplierBinding
import com.blackboxpro.runtime.bindings.SharedSlf4jLoggerSupplier
import com.blackboxpro.common.runtime.dispatcher.RuntimeActionRegistry

object ActionRegistry {

    private val registry = RuntimeActionRegistry(SharedSlf4jLoggerSupplier.getLogger("BlackBoxPro-Registry"))

    fun register(actionId: String, executor: ActionExecutor) {
        registry.register(actionId, executor)
    }

    fun registerExternal(actionId: String, executor: ActionExecutor) {
        registry.registerExternal(actionId, executor)
    }

    fun find(actionId: String): ActionExecutor? = registry.find(actionId)

    fun size(): Int = registry.size()

    fun registerAll() {
        registry.ensureNotInitialized()

        // Bind common actions that need platform-specific dependencies
        val boundBatchAction = BatchAction().also { it.bind(LoggerSupplierBinding) }

        // === 绉诲姩涓庝綅缃?===
        register("player_move", PlayerMoveAction())
        register("player_move_look", PlayerMoveLookAction())
        register("player_look", PlayerLookAction())
        register("player_on_ground", PlayerOnGroundAction())
        register("confirm_teleportation", ConfirmTeleportationAction())
        register("move_vehicle", MoveVehicleAction())
        register("paddle_boat", PaddleBoatAction())
        register("player_input", PlayerInputAction())

        // === 鏂瑰潡浜や簰 ===
        register("dig_start", DigStartAction())
        register("dig_cancel", DigCancelAction())
        register("dig_finish", DigFinishAction())
        register("place_block", PlaceBlockAction())
        register("use_item", UseItemAction())

        // === 瀹炰綋浜や簰涓庢垬鏂?===
        register("attack_entity", AttackEntityAction())
        register("interact_entity", InteractEntityAction())
        register("interact_entity_at", InteractEntityAtAction())
        register("swing_arm", SwingArmAction())
        register("left_click", LeftClickAction())

        // === 瀹瑰櫒/GUI 鎿嶄綔 ===
        register("click_slot", ClickSlotAction())
        register("click_button", ClickButtonAction())
        register("close_container", CloseContainerAction())
        register("set_carried_item", SetCarriedItemAction())
        register("creative_set_slot", CreativeSetSlotAction())
        register("pick_item", PickItemAction())
        register("pick_entity", PickEntityAction())
        register("pick_item_from_block", PickItemFromBlockAction())
        register("pick_item_from_entity", PickItemFromEntityAction())
        register("bundle_selected_slot", BundleSelectedSlotAction())
        register("slot_state_change", SlotStateChangeAction())
        register("hover_slot", HoverSlotAction())

        // === 鐜╁鐘舵€佷笌鍔ㄤ綔 ===
        register("sneak_start", SneakStartAction())
        register("sneak_stop", SneakStopAction())
        register("leave_bed", LeaveBedAction())
        register("sprint_start", SprintStartAction())
        register("sprint_stop", SprintStopAction())
        register("horse_jump_start", HorseJumpStartAction())
        register("horse_jump_stop", HorseJumpStopAction())
        register("open_horse_inventory", OpenHorseInventoryAction())
        register("elytra_start", ElytraStartAction())
        register("drop_item", DropItemAction())
        register("drop_item_stack", DropItemStackAction())
        register("finish_using", FinishUsingAction())
        register("swap_hands", SwapHandsAction())
        register("perform_respawn", PerformRespawnAction())
        register("spectator_teleport", SpectatorTeleportAction())

        // === 鑱婂ぉ涓庡懡浠?===
        register("chat_message", ChatMessageAction())
        register("chat_command", ChatCommandAction())
        register("click_chat_text", ClickChatTextAction())

        // === 瀹㈡埛绔缃笌淇℃伅 ===
        register("client_information", ClientInformationAction())
        register("player_abilities", PlayerAbilitiesAction())
        register("resource_pack_response", ResourcePackResponseAction())
        register("screenshot", ScreenshotAction())
        register("screenshot_tooltip", ScreenshotTooltipAction())
        register("connect_to_server", ConnectToServerAction())
        register("create_world", CreateWorldAction())
        register("join_world", JoinWorldAction())
        register("leave_world", LeaveWorldAction())
        register("close_screen", CloseScreenAction())
        register("open_inventory", OpenInventoryAction())

        // === 杩涢樁浜や簰 ===
        register("edit_book", EditBookAction())
        register("sign_book", SignBookAction())
        register("update_sign", UpdateSignAction())
        register("update_command_block", UpdateCommandBlockAction())
        register("update_command_block_minecart", UpdateCommandBlockMinecartAction())
        register("update_structure_block", UpdateStructureBlockAction())
        register("update_jigsaw_block", UpdateJigsawBlockAction())
        register("select_recipe", SelectRecipeAction())
        register("recipe_book_toggle", RecipeBookToggleAction())
        register("query_entity_nbt", QueryEntityNbtAction())
        register("query_block_nbt", QueryBlockNbtAction())
        register("set_beacon_effect", SetBeaconEffectAction())
        register("rename_item", RenameItemAction())
        register("lock_difficulty", LockDifficultyAction())
        register("select_trade", SelectTradeAction())
        register("advancement_tab", AdvancementTabAction())
        register("recipe_book_seen", RecipeBookSeenAction())

        // === 璋冭瘯涓庣壒娈婃搷浣?===
        register("custom_payload", CustomPayloadAction())
        register("tab_complete", TabCompleteAction())
        register("keep_alive", KeepAliveAction())
        register("pong", PongAction())
        register("debug_sample_subscription", DebugSampleAction())
        register("chunk_batch_received", ChunkBatchReceivedAction())

        // === 澶嶅悎琛屼负 ===
        register("look_at", LookAtAction())
        register("look_at_entity", LookAtEntityAction())
        register("break_block", BreakBlockAction())
        register("place_block_at", PlaceBlockAtAction())
        register("attack", AttackAction())
        register("use", UseAction())
        register("open_container", OpenContainerAction())
        register("container_transfer", ContainerTransferAction())
        register("drop_inventory", DropInventoryAction())
        register("pathfind_to", PathfindToAction())
        register("batch", boundBatchAction)
        register("wait", WaitAction())
        register("respawn", RespawnAction())
        register("craft_recipe", CraftRecipeAction())

        // === 鏌ヨ琛屼负 ===
        register("query_held_item", QueryHeldItemAction())
        register("query_inventory_slot", QueryInventorySlotAction())
        register("query_chat_history", QueryChatHistoryAction())
        register("query_nearby_entities", QueryNearbyEntitiesAction())
        register("query_container_state", QueryContainerStateAction())
        register("query_player_state", QueryPlayerStateAction())
        register("query_container_slots", QueryContainerSlotsAction())
        register("query_active_effects", QueryActiveEffectsAction())
        register("query_block_state", QueryBlockStateAction())
        register("query_world_state", QueryWorldStateAction())
        register("query_tab_list", QueryTabListAction())
        register("query_scoreboard", QueryScoreboardAction())
        register("query_screen_state", QueryScreenStateAction())
        register("query_boss_bar", QueryBossBarAction())
        register("query_tooltip_state", QueryTooltipStateAction())
        register("query_chat_style", QueryChatStyleAction())
        register("query_slot_tooltip", QuerySlotTooltipAction())

        // === 鐜╁鍔ㄤ綔锛堟柊澧烇級===
        register("jump", JumpAction())

        // === 瀵艰埅涓庣瀯鍑?===
        register("look_at_block", LookAtBlockAction())
        register("navigate_to", NavigateToAction())

        registry.freezeAndValidate(ActionCatalog.getActionIds().toSet())
    }
}

