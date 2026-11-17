package com.gridinv;

import com.mrcrayfish.backpacked.common.CostModel;
import com.mrcrayfish.backpacked.common.PaymentItem;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.inventory.container.UnlockableController;
import net.minecraft.world.Container;

import java.util.List;

/**
 * Read-only view of which backpack slots are unlocked. Unlocking is still done
 * in Backpacked's own backpack screen (its keybind keeps working).
 */
public class FixedController extends UnlockableController {
    public FixedController(UnlockableSlots slots) { super(slots); }

    @Override public CostModel getCostModel() { return null; }
    @Override public PaymentItem getPaymentItem() { return null; }
    @Override public List<Container> getPaymentContainers() { return List.of(); }
    @Override public boolean allowsUnlockToken() { return false; }
}
