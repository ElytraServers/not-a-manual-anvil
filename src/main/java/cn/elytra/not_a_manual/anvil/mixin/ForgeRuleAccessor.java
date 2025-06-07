package cn.elytra.not_a_manual.anvil.mixin;

import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ForgeRule.class, remap = false)
public interface ForgeRuleAccessor {

    @Accessor("type")
    ForgeStep getType();

}
