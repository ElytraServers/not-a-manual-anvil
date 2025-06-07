package cn.elytra.not_a_manual.anvil.util;

import cn.elytra.not_a_manual.anvil.mixin.ForgeRuleAccessor;
import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeStep;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;

@SuppressWarnings("unused")
public class TFCReflect {

    private static final Field FORGE_RULES_ORDER_FIELD;
    private static final Field FORGE_RULE_ORDER_Y_FIELD;

    private static final Class<?> FORGE_RULE_ORDER_CLASS;

    private static Integer valueOrderAny;

    static {
        try {
            {
                Field f = ForgeRule.class.getDeclaredField("order");
                f.setAccessible(true);
                FORGE_RULES_ORDER_FIELD = f;
            }

            {
                FORGE_RULE_ORDER_CLASS = Class.forName(ForgeRule.class.getCanonicalName() + "$Order");
                Field f = FORGE_RULE_ORDER_CLASS.getDeclaredField("y");
                f.setAccessible(true);
                FORGE_RULE_ORDER_Y_FIELD = f;
            }
        } catch(Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getOrderValue(ForgeRule rule) {
        try {
            Object order = FORGE_RULES_ORDER_FIELD.get(rule);
            return FORGE_RULE_ORDER_Y_FIELD.getInt(order);
        } catch(IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getOrderAnyValueInternal() {
        try {
            for(Object enumConstant : FORGE_RULE_ORDER_CLASS.getEnumConstants()) {
                if(((Enum<?>) enumConstant).name().equals("ANY")) {
                    return FORGE_RULE_ORDER_Y_FIELD.getInt(enumConstant);
                }
            }
            throw new NoSuchElementException("ANY");
        } catch(Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isOrderAny(ForgeRule rule) {
        if(valueOrderAny == null) {
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
}
