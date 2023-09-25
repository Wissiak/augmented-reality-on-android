package com.example.augmented_reality_on_android

import android.graphics.Bitmap
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.linalg.inv
import org.jetbrains.kotlinx.multik.api.linalg.norm
import org.jetbrains.kotlinx.multik.api.linalg.solve
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.append
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import org.opencv.core.*
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.SIFT
import kotlin.math.pow
import kotlin.math.sqrt

class Utils {
    companion object {
        fun cross(
            vectorA: MultiArray<Double, D1>,
            vectorB: D1Array<Double>
        ): D1Array<Double> {
            val crossProduct = mk.ndarray(
                doubleArrayOf(
                    vectorA[1] * vectorB[2] - vectorA[2] * vectorB[1],
                    vectorA[2] * vectorB[0] - vectorA[0] * vectorB[2],
                    vectorA[0] * vectorB[1] - vectorA[1] * vectorB[0]
                )
            )

            return crossProduct
        }
    }
}

class RecoveryFromHomography {
    private var R_c_b: Mat
    private var t_c_b: Mat
    private var fx: Int
    private var fy: Int

    constructor(H_c_b: D2Array<Double>) {
        R_c_b = Mat(3, 3, CvType.CV_8UC4)
        t_c_b = Mat(3, 1, CvType.CV_8UC4)
        fx = 1
        fy = 1
    }

}

//https://answers.opencv.org/question/5597/cvtype-meaning/
class ARCore {
    private var sift: SIFT
    private var matcher: BFMatcher

    init {
        sift = SIFT.create(3000)
        sift.contrastThreshold = 0.001
        sift.edgeThreshold = 20.0
        sift.sigma = 1.5
        sift.nOctaveLayers = 4

        matcher = BFMatcher.create(Core.NORM_L2, true)
    }

    fun recoverRigidBodyMotionAndFocalLengths(H_c_b: D2Array<Double>): RecoveryFromHomography {
        val Ma = mk.ndarray(
            arrayOf(
                doubleArrayOf(H_c_b[0,0].pow(2), H_c_b[1][0].pow(2), H_c_b[2][0].pow(2)),
                doubleArrayOf(H_c_b[0,1].pow(2), H_c_b[1][1].pow(2), H_c_b[2][1].pow(2)),
                doubleArrayOf(H_c_b[0,0]*H_c_b[0][1], H_c_b[1][0]*H_c_b[1][1], H_c_b[2][0]*H_c_b[2][1])
            )
        )
        val y = mk.ndarray(arrayOf(doubleArrayOf(1.0), doubleArrayOf(1.0), doubleArrayOf(1.0)))

        val diags = mk.linalg.inv(Ma).dot((y))

        diags[0] * diags[2]

        //lambdaInv = mk

        return RecoveryFromHomography(H_c_b)
    }

    fun homographyFrom4PointCorrespondences(x_d: D2Array<Double>, x_u: D2Array<Double>): D2Array<Double> {
        val A = mk.zeros<Double>(8, 8)
        val y = mk.zeros<Double>(8)

        for (n in 0..3) {
            A[2*n] = mk.ndarray(doubleArrayOf(x_u[n,0], x_u[n,1], 1.0, 0.0, 0.0, 0.0, -x_u[n,0]*x_d[n,0], -x_u[n , 1]*x_d[n, 0]))
            A[2*n+1] = mk.ndarray(doubleArrayOf(0.0, 0.0, 0.0, x_u[n, 0], x_u[n, 1], 1.0, -x_u[n, 0]*x_d[n, 1], -x_u[n, 1]*x_d[n, 1]))
            y[2*n] = x_d[n, 0]
            y[2*n+1] = x_d[n, 1]
        }
        val theta = mk.linalg.solve(A, y)
        val H_d_u = theta.append(1.0).reshape(3, 3)
        return H_d_u
    }

    fun findPoseTransformationParamsLeastSquares(x_d: D2Array<Double>, x_u: D2Array<Double>) {
        //TODO: implement
    }

    fun drawARObject(video_frame: Bitmap, K_c: D2Array<Double>, R_c_b: D2Array<Double>, t_c_cb: D2Array<Double>) {
        //TODO: implement
    }

    fun focalLength(H_c_b: D2Array<Double>): Double {
        val h00 = H_c_b[0, 0]
        val h01 = H_c_b[0, 1]
        val h10 = H_c_b[1, 0]
        val h11 = H_c_b[1, 1]
        val h20 = H_c_b[2, 0]
        val h21 = H_c_b[2, 1]

        val fsquare = - (h00 * h01 + h10 * h11) / (h20 * h21)
        return sqrt(fsquare)
    }

    fun rigidBodyMotion(H_c_b: D2Array<Double>, f: Int) {
        val K_c = mk.ndarray(
            arrayOf(
                intArrayOf(f, 0, 0),
                intArrayOf(0, f, 0),
                intArrayOf(0, 0, 1)
            )
        )
        var V = mk.linalg.inv(K_c) * H_c_b

        V /= mk.linalg.norm(V[0, 0..2].reshape(3, 1))

        val rx = V[0..2, 0]
        val ry = V[0..2, 1] / mk.linalg.norm(V[0..2, 1].reshape(3, 1))
        val rz = Utils.cross(rx, ry)
        val R_c_b = rx.append(ry).append(rz).reshape(3, 3).transpose()
        val t_c_b = V[0..2, 2]
        print("hio")
    }
}