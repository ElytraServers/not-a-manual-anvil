package cn.elytra.not_a_manual.anvil;

import cn.elytra.not_a_manual.anvil.util.Result;
import cn.elytra.not_a_manual.anvil.util.TFCReflect;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import net.dries007.tfc.client.screen.AnvilScreen;
import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeStep;
import net.dries007.tfc.common.capabilities.forge.Forging;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.heat.IHeat;
import net.dries007.tfc.common.container.AnvilContainer;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.ScreenButtonPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class AutoAnvilExecutor {

    // limit to 15 steps to prevent infinite loops
    private static final int LOOP_LIMIT = 15;

    /**
     * The memoization map for selected forge steps.
     * The key is the offset (destination - current), and the value is the selected forge step.
     *
     * @see #getMemoizedSelectedForgeStep(int, int)
     */
    private static final Int2ObjectMap<ForgeStep> SELECTED_STEP_MEMOIZE_MAP = new Int2ObjectArrayMap<>();

    protected final AnvilScreen anvilScreen;

    public AutoAnvilExecutor(AnvilScreen anvilScreen) {
        this.anvilScreen = anvilScreen;
    }

    protected AnvilContainer getAnvilContainer() {
        return anvilScreen.getMenu();
    }

    @Nullable
    protected Forging getForging() {
        return getAnvilContainer().getBlockEntity()
            .getMainInputForging();
    }

    @Nullable
    protected ForgeRule[] getForgeRules(@NotNull Forging forging) {
        AnvilRecipe recipe = forging.getRecipe(null);
        if (recipe == null) {
            return null;
        }
        return recipe.getRules();
    }

    /**
     * Send the forge step packet to the server.
     *
     * @param step the forge step to send
     */
    public void doForgeStep(ForgeStep step) {
        // from AnvilStepButton#<init>
        NotAManualAnvil.LOG.debug("Sending the forge step packet: {}", step.name());
        PacketHandler.send(PacketDistributor.SERVER.noArg(), new ScreenButtonPacket(step.ordinal(), null));
    }

    /**
     * Select the best forge step to reach the destination.
     *
     * @param current     the current work value
     * @param destination the destination work value
     * @return the selected forge step
     * @see #getMemoizedSelectedForgeStep(int, int)
     */
    protected ForgeStep getSelectedForgeStep(int current, int destination) {
        Optional<Pair<ForgeStep, Integer>> min = Arrays.stream(ForgeStep.values())
            .map(fs -> Pair.of(fs, fs.step()))
            .map(pair -> Pair.of(pair.left(), Math.abs(current + pair.right() - destination)))
            .min(Comparator.comparingInt(Pair::right));
        return min.orElseThrow()
            .left();
    }

    /**
     * Select the best forge step to reach the destination, using memoization to avoid recalculating
     *
     * @param current     the the current work value
     * @param destination the destination work value
     * @return the selected forge step
     */
    protected ForgeStep getMemoizedSelectedForgeStep(int current, int destination) {
        int offset = destination - current;
        return SELECTED_STEP_MEMOIZE_MAP.computeIfAbsent(offset, ignored -> getSelectedForgeStep(current, destination));
    }

    /**
     * Check if the item has enough heat to work.
     *
     * @param stack the item to check
     * @return {@code true} if the item has enough heat to work.
     */
    public static boolean isEnoughHeatToWork(ItemStack stack) {
        IHeat heat = HeatCapability.get(stack);
        return heat != null && heat.canWork();
    }

    /**
     * Check if the working item has enough heat to work.
     *
     * @return {@code true} if the working item has enough heat to work.
     */
    protected boolean isEnoughHeatToWork() {
        ItemStack stack = getAnvilContainer().getItems()
            .get(0);
        return isEnoughHeatToWork(stack);
    }

    /**
     * Move the forging current work value to the destination.
     * It will simulate the player, sending packets to the server to perform the forge steps.
     * If the destination is not reachable within {@link #LOOP_LIMIT}(15) iterations, it will fail.
     *
     * @param destination the destination work value
     * @return the result of the operation, containing an error message if failed
     */
    public Result<Component> moveTo(@Range(from = 0, to = 150) int destination) {
        Forging forging = getForging();
        if (forging == null) {
            return Result.fail(Component.translatable("not_a_manual_anvil.auto_anvil.fail.no_forging"));
        }

        int current = forging.getWork();
        for (int i = 0; i < LOOP_LIMIT && current != destination; i++) {
            if (!isEnoughHeatToWork()) {
                return Result.fail(Component.translatable("not_a_manual_anvil.auto_anvil.fail.not_hot_enough"));
            }

            ForgeStep step = getMemoizedSelectedForgeStep(current, destination);
            doForgeStep(step);
            current += step.step();
        }
        if (current != destination) {
            return Result.fail(
                Component
                    .translatable("not_a_manual_anvil.auto_anvil.fail.reached_max_iteration", destination, LOOP_LIMIT));
        }

        return Result.ok();
    }

    /**
     * Try to workout the forging.
     */
    public void workout() {
        Forging forging = getForging();
        if (forging == null) {
            NotAManualAnvil.LOG.error("No forging found!");
            return;
        }

        Result<Component> result = internalWorkoutStepsBeforeRules(forging)
            .then(() -> internalWorkoutFinalSteps(forging));
        if (!result.success()) {
            NotAManualAnvil.trySendMessageToLocalPlayer(result.errorMessage());
        }
    }

    /**
     * Try to workout the forging before applying the last 2-3 rules.
     */
    @SuppressWarnings("unused") // for test purposes
    public void workoutBeforeRules() {
        Forging forging = getForging();
        if (forging == null) {
            NotAManualAnvil
                .trySendMessageToLocalPlayer(Component.translatable("not_a_manual_anvil.auto_anvil.fail.no_forging"));
            return;
        }

        Result<Component> result = internalWorkoutStepsBeforeRules(forging);
        if (!result.success()) {
            NotAManualAnvil.LOG.error("Failed to workout before rules: {}", result.errorMessage());
            NotAManualAnvil.trySendMessageToLocalPlayer(result.errorMessage());
        }
    }

    /**
     * Move the work value to the destination with a offset of the last operations of the forge rules.
     *
     * @return the result of the operation, containing an error message if failed
     */
    protected Result<Component> internalWorkoutStepsBeforeRules(@NotNull Forging forging) {
        int finalTarget = forging.getWorkTarget();
        // the target that the last operations have not been applied yet as they need to be applied by the rules
        ForgeRule[] rules = getForgeRules(forging);
        if (rules == null) {
            return moveTo(finalTarget);
        }
        int targetBeforeLastOperations = finalTarget - Arrays.stream(rules)
            .mapToInt(TFCReflect::getStepValueFromForgeRule)
            .sum();
        return moveTo(targetBeforeLastOperations);
    }

    /**
     * Move the work value to the final destination, calculating and applying the last steps of the forge rules.
     *
     * @return the result of the operation, containing an error message if failed
     */
    protected Result<Component> internalWorkoutFinalSteps(@NotNull Forging forging) {
        AnvilRecipe recipe = forging.getRecipe(null);
        if (recipe == null) {
            return Result.fail(Component.translatable("not_a_manual_anvil.auto_anvil.fail.no_forging_recipe"));
        }
        for (ForgeStep step : getOrderedForgeRuleSteps(recipe.getRules())) {
            doForgeStep(step);
        }
        return Result.ok();
    }

    /**
     * Get the last forge steps to apply from the forge rules.
     *
     * @param rules the forge rules of the recipe
     * @return the last forge steps to apply
     */
    protected static ForgeStep[] getOrderedForgeRuleSteps(ForgeRule[] rules) {
        return Arrays.stream(rules)
            .sorted(Comparator.comparingInt(AutoAnvilExecutor::getForgeRulePriority))
            .map(TFCReflect::getForgeStepFromForgeRule)
            .toArray(ForgeStep[]::new);
    }

    @SuppressWarnings("unused")
    public static final int ANY = 88, LAST = 0, NOT_LAST = 66, SECOND_LAST = 22, THIRD_LAST = 44;

    /**
     * The priority of the forge rule.
     * It is used to sort the last forge steps to apply.
     */
    protected static int getForgeRulePriority(ForgeRule rule) {
        int y = TFCReflect.getOrderValue(rule);
        return switch (y) {
            case LAST -> 10;
            case THIRD_LAST -> -10;
            default -> 0;
        };
    }

}
