package com.example.augmented_reality_on_android

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import kotlin.math.sqrt
import kotlin.test.assertNotNull

class ARCoreUnitTest {
    private lateinit var arCore: ARCore
    private lateinit var H_c_b: D2Array<Double>
    private lateinit var x_d: D2Array<Double>
    private lateinit var x_u: D2Array<Double>
    private lateinit var shape: Size

    @BeforeEach
    fun setUp() {
        System.loadLibrary("opencv_java480")
        val reference_image = Mat(100, 100, CvType.CV_32FC2);
        arCore = ARCore(reference_image)

        H_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-1.0, 2.0, 3.0),
                doubleArrayOf(4.0, -5.0, 6.0),
                doubleArrayOf(7.0, 8.0, 9.0),
            )
        )
        x_d = mk.ndarray(
            arrayOf(
                doubleArrayOf(200.23314, 252.60797),
                doubleArrayOf(109.43447, 490.69876),
                doubleArrayOf(440.8565, 630.7588),
                doubleArrayOf(555.4785, 320.25253),
            )
        )
        x_u = mk.ndarray(
            arrayOf(
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(205.0, 0.0),
                doubleArrayOf(205.0, 285.0),
                doubleArrayOf(0.0, 285.0),
            )
        )
        shape = Size(539.0, 959.0)
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

    @Test
    fun test_recoverRigidBodyMotionAndFocalLengths() {
        val cH_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-0.43699279658975915, 1.0158929392626697, -69.26686096191406),
                doubleArrayOf(1.1610037751270545, 0.3657492297298278, -226.89202880859375),
                doubleArrayOf(-3.7031925044988056e-05, -0.000806291569845789, 1.0),
            )
        )
        val res = arCore.recoverRigidBodyMotionAndFocalLengths(cH_c_b)

        val expR_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-0.35149255313798733, 0.8171274348751087, -0.45689795388353843),
                doubleArrayOf(0.935914197685627, 0.2948396070971149, -0.1927024147676617),
                doubleArrayOf(-0.022750816666840444, -0.49535074569565735, -0.8683950938828178),
            )
        )

        val expt_c_cb = mk.ndarray(
            arrayOf(
                doubleArrayOf(-55.71438705021373),
                doubleArrayOf(-182.9033424808808),
                doubleArrayOf(614.3568458620966),
            )
        )

        val expfx = 763.7985891324039
        val expfy = 762.1111198920303

        assertEquals(expR_c_b, res.R_c_b)
        assertEquals(expt_c_cb, res.t_c_cb)
        assertEquals(expfx, res.fx)
        assertEquals(expfy, res.fy)
    }

    @Test
    fun test_findPoseTransformationParams() {
        val res: RecoveryFromHomography? = arCore.findPoseTransformationParams(shape, x_d, x_u)

        val expR_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-0.36346958963518994, 0.8103524968812156, -0.45958534377067123),
                doubleArrayOf(0.9267872755629462, 0.2644149733373461, -0.26673970032528355),
                doubleArrayOf(-0.09463193575658364, -0.5228896180585249, -0.8471311846824975)
            )
        )

        val expt_c_cb = mk.ndarray(
            arrayOf(
                doubleArrayOf(-53.227998757614074),
                doubleArrayOf(-174.28181170485738),
                doubleArrayOf(779.0564081797978),
            )
        )

        val expfx = 1013.8046219476499
        val expfy = 1014.2291281419842

        assertNotNull(res)
        assertEquals(expR_c_b, res.R_c_b)
        assertEquals(expt_c_cb, res.t_c_cb)
        assertEquals(expfx, res.fx)
        assertEquals(expfy, res.fy)
    }

    @Test
    fun test_homographyFrom4PointCorrespondences() {
        val x_d_center = Size(shape.width / 2, shape.height / 2)
        val x_ds_center: D2Array<Double> = mk.zeros(4, 2)
        for (i in 0..3) {
            x_ds_center[i] = mk.ndarray(
                doubleArrayOf(
                    x_d[i, 0] - x_d_center.width,
                    x_d[i, 1] - x_d_center.height
                )
            )
        }

        val res = arCore.homographyFrom4PointCorrespondences(x_ds_center, x_u)

        val cH_c_b = mk.ndarray(
            arrayOf(
                doubleArrayOf(-0.4369927800171011, 1.0158928755270795, -69.26686000000001),
                doubleArrayOf(1.16100377429334, 0.36574923601165304, -226.89203),
                doubleArrayOf(-3.703209208324552e-05, -8.062916332568412e-4, 1.0),
            )
        )

        assertEquals(cH_c_b, res)
    }
}