package cn.elytra.not_a_manual.anvil.util;

import cn.elytra.not_a_manual.anvil.mixin.ForgeRuleAccessor;
import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import net.dries007.tfc.common.capabilities.VesselLike;
import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeStep;
import net.dries007.tfc.common.items.VesselItem;
import net.minecraftforge.items.ItemStackHandler;

@SuppressWarnings("unused")
public class TFCReflect {

    private static final Field FORGE_RULES_ORDER_FIELD;
    private static final Field FORGE_RULE_ORDER_Y_FIELD;

    private static final Class<?> FORGE_RULE_ORDER_CLASS;

    private static final Class<?> VESSEL_ITEM_VESSEL_CAPABILITY_CLASS;
    private static final Field VESSEL_CAPABILITY_INVENTORY_FIELD;
    private static final Field VESSEL_CAPABILITY_CAPACITY_FIELD;

    private static Integer valueOrderAny;

    static {
        try {
            {
                FORGE_RULES_ORDER_FIELD = ForgeRule.class.getDeclaredField("order");
                FORGE_RULES_ORDER_FIELD.setAccessible(true);
            }

            {
                FORGE_RULE_ORDER_CLASS = Class.forName(ForgeRule.class.getCanonicalName() + "$Order");
                FORGE_RULE_ORDER_Y_FIELD = FORGE_RULE_ORDER_CLASS.getDeclaredField("y");
                FORGE_RULE_ORDER_Y_FIELD.setAccessible(true);
            }

            {
                VESSEL_ITEM_VESSEL_CAPABILITY_CLASS = Class
                    .forName(VesselItem.class.getCanonicalName() + "$VesselCapability");

                VESSEL_CAPABILITY_INVENTORY_FIELD = VESSEL_ITEM_VESSEL_CAPABILITY_CLASS.getDeclaredField("inventory");
                VESSEL_CAPABILITY_INVENTORY_FIELD.setAccessible(true);

                VESSEL_CAPABILITY_CAPACITY_FIELD = VESSEL_ITEM_VESSEL_CAPABILITY_CLASS.getDeclaredField("capacity");
                VESSEL_CAPABILITY_CAPACITY_FIELD.setAccessible(true);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getOrderValue(ForgeRule rule) {
        try {
            Object order = FORGE_RULES_ORDER_FIELD.get(rule);
            return FORGE_RULE_ORDER_Y_FIELD.getInt(order);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getOrderAnyValueInternal() {
        try {
            for (Object enumConstant : FORGE_RULE_ORDER_CLASS.getEnumConstants()) {
                if (((Enum<?>) enumConstant).name()
                    .equals("ANY")) {
                    return FORGE_RULE_ORDER_Y_FIELD.getInt(enumConstant);
                }
            }
            throw new NoSuchElementException("ANY");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isOrderAny(ForgeRule rule) {
        if (valueOrderAny == null) {
            valueOrderAny = getOrderAnyValueInternal();
        }
        return getOrderValue(rule) == valueOrderAny;
    }

    public static ForgeStep getForgeStepFromForgeRule(ForgeRule forgeRule) {
        return ((ForgeRuleAccessor) (Object) forgeRule).getType();
    }

    public static int getStepValueFromForgeRule(ForgeRule forgeRule) {
        return getForgeStepFromForgeRule(forgeRule).step();
    }

    public static ItemStackHandler getVesselItemInventory(VesselLike vesselLike) {
        try {
            return (ItemStackHandler) VESSEL_CAPABILITY_INVENTORY_FIELD.get(vesselLike);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getVesselItemCapacity(VesselLike vesselLike) {
        try {
            return VESSEL_CAPABILITY_CAPACITY_FIELD.getInt(vesselLike);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
