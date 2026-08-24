package me.eldodebug.soar.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MathUtilsTest {

    @Test
    public void finiteValuesAreAccepted() {
        assertTrue(MathUtils.isFinite(0.0F));
        assertTrue(MathUtils.isFinite(-Float.MAX_VALUE));
    }

    @Test
    public void nonFiniteValuesAreRejected() {
        assertFalse(MathUtils.isFinite(Float.NaN));
        assertFalse(MathUtils.isFinite(Float.POSITIVE_INFINITY));
        assertFalse(MathUtils.isFinite(Float.NEGATIVE_INFINITY));
    }
}
