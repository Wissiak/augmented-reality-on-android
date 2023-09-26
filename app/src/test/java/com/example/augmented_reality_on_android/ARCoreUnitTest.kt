package com.example.augmented_reality_on_android

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencv.core.CvType
import org.opencv.core.Mat
import kotlin.math.sqrt

class ARCoreUnitTest {
    private lateinit var arCore: ARCore
    private lateinit var H_c_b: D2Array<Double>

    @BeforeEach
    fun setUp() {
        System.loadLibrary("opencv_java480")
        val reference_image = Mat(100, 100, CvType.CV_32FC2);
        this.arCore = ARCore(reference_image)

        this.H_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-1.0, 2.0, 3.0),
                doubleArrayOf(4.0, -5.0, 6.0),
                doubleArrayOf(7.0, 8.0, 9.0),
            )
        )
    }

    @Test
    fun test_focalLength() {
        val res = arCore.focalLength(H_c_b)
        val exp = sqrt(-(-1.0 * 2 + 4 * -5) / (7 * 8)) // 0.6267831705280087
        assertEquals(exp, res)
    }

    @Test
    fun test_rigidBodyMotion() {
        val res: RecoveryFromHomography = arCore.rigidBodyMotion(H_c_b, 10.0)
        val expR_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-0.014260997240803912, 0.02494355114064532, 0.11916617118751906),
                doubleArrayOf(0.05704398896321565, -0.0623588778516133, 0.03912919053918536),
                doubleArrayOf(0.9982698068562739, 0.9977420456258128, -0.0005335798709888912)
            )
        )

        assertEquals(expR_c_b, res.R_c_b)
    }
}