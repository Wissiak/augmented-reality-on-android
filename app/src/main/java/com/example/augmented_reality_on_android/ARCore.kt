package com.example.augmented_reality_on_android

import android.graphics.Bitmap
import org.opencv.imgcodecs.Imgcodecs
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.linalg.inv
import org.jetbrains.kotlinx.multik.api.linalg.norm
import org.jetbrains.kotlinx.multik.api.linalg.solve
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.core.Core.perspectiveTransform
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.SIFT
import kotlin.math.*

class Utils {
    companion object {
        fun cross(
            vectorA: MultiArray<Double, D1>,
            vectorB: MultiArray<Double, D1>,
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

class RecoveryFromHomography(
    public var R_c_b: D2Array<Double>,
    public var t_c_cb: MultiArray<Double, D2>,
    public var fx: Double = -1.0,
    public var fy: Double = -1.0
) {

}

//https://answers.opencv.org/question/5597/cvtype-meaning/
class ARCore {
    private var sift: SIFT
    private var matcher: BFMatcher
    private var reference_image: Mat

    constructor(reference_image: Mat) {
        this.reference_image = reference_image
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
                doubleArrayOf(H_c_b[0, 0].pow(2), H_c_b[1][0].pow(2), H_c_b[2][0].pow(2)),
                doubleArrayOf(H_c_b[0, 1].pow(2), H_c_b[1][1].pow(2), H_c_b[2][1].pow(2)),
                doubleArrayOf(
                    H_c_b[0, 0] * H_c_b[0][1],
                    H_c_b[1][0] * H_c_b[1][1],
                    H_c_b[2][0] * H_c_b[2][1]
                )
            )
        )
        val y = mk.ndarray(doubleArrayOf(1.0, 1.0, 0.0))

        val diags = mk.linalg.inv(Ma).dot((y.reshape(3, 1))).flatten()

        //if ((diags[0] * diags[2])[0] != 0.0 ) {

        val diags_sqrt = mk.zeros<Double>(3, 3)
        for (i in 0..2) {
            diags_sqrt[i, i] = sqrt(diags[i])
        }
        val B = diags_sqrt.dot(H_c_b)
        val rx = B[0..2, 0]
        val ry = B[0..2, 1]
        val rz = Utils.cross(rx, ry)
        val R_c_b = rx.append(ry).append(rz).reshape(3, 3).transpose()
        val t_c_cb = B[0..2, 2].reshape(3, 1)
        val fx = diags_sqrt[2, 2] / diags_sqrt[0, 0]
        val fy = diags_sqrt[2, 2] / diags_sqrt[1, 1]

        return RecoveryFromHomography(R_c_b, t_c_cb, fx, fy)
    }

    fun homographyFrom4PointCorrespondences(
        x_d: D2Array<Double>,
        x_u: D2Array<Double>
    ): D2Array<Double> {
        val A = mk.zeros<Double>(8, 8)
        val y = mk.zeros<Double>(8)

        for (n in 0..3) {
            A[2 * n] = mk.ndarray(
                doubleArrayOf(
                    x_u[n, 0],
                    x_u[n, 1],
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                    -x_u[n, 0] * x_d[n, 0],
                    -x_u[n, 1] * x_d[n, 0]
                )
            )
            A[2 * n + 1] = mk.ndarray(
                doubleArrayOf(
                    0.0,
                    0.0,
                    0.0,
                    x_u[n, 0],
                    x_u[n, 1],
                    1.0,
                    -x_u[n, 0] * x_d[n, 1],
                    -x_u[n, 1] * x_d[n, 1]
                )
            )
            y[2 * n] = x_d[n, 0]
            y[2 * n + 1] = x_d[n, 1]
        }
        val theta = mk.linalg.solve(A, y)
        val H_d_u = theta.append(1.0).reshape(3, 3)
        return H_d_u
    }

    fun findPoseTransformationParams(
        shape: Size,
        x_d: D2Array<Double>,
        x_u: D2Array<Double>
    ): RecoveryFromHomography? {
        val x_d_center = Size(shape.width / 2, shape.height / 2)
        val rotations = 3
        val steps_per_rotation = 50.0
        val delta_per_rotation = 6
        val length: Int = (rotations * steps_per_rotation).toInt()
        val solutions = MutableList<RecoveryFromHomography?>(length) { null }
        val ratios = MutableList<Double>(length) { 0.0 }
        val angles = MutableList<Double>(length) { 0.0 }
        for (iter in 0 until length) {
            val alpha = iter * 2 * Math.PI / steps_per_rotation
            val dr:Double = iter / steps_per_rotation * delta_per_rotation
            val dx = dr * cos(alpha)
            val dy = dr * sin(alpha)
            val x_ds = x_d.copy()
            x_ds[1] = x_ds[1] + mk.ndarray(doubleArrayOf(dx, dy)) //TODO check this
            val x_ds_center: D2Array<Double> = mk.zeros(4, 2)
            for (i in 0..3) {
                x_ds_center[i] = mk.ndarray(
                    doubleArrayOf(
                        x_ds[i, 0] - x_d_center.width,
                        x_ds[i, 1] - x_d_center.height
                    )
                )
            }
            val cH_c_b = homographyFrom4PointCorrespondences(x_ds_center, x_u)
            // Determine the pose and the focal lengths
            val res: RecoveryFromHomography = recoverRigidBodyMotionAndFocalLengths(cH_c_b)
            if (res.fx != -1.0) {
                ratios[iter] = min(res.fx, res.fy) / max(res.fx, res.fy)
                angles[iter] = alpha
                solutions[iter] = res
            }
        }
        if (ratios.size == 0) {
            println("Could not find a Rotation Matrix and Transformation Vector")
            return null
        }

        // Identify the most plausible solution
        val idx = ratios.indices.maxBy { ratios[it] }
        return solutions[idx]
    }
/*
    fun drawARObject(
        video_frame: Bitmap,
        K_c: D2Array<Double>,
        R_c_b: D2Array<Double>,
        t_c_cb: D2Array<Double>
    ) {
        //TODO: implement
    }*/

    fun focalLength(H_c_b: D2Array<Double>): Double {
        val h00 = H_c_b[0, 0]
        val h01 = H_c_b[0, 1]
        val h10 = H_c_b[1, 0]
        val h11 = H_c_b[1, 1]
        val h20 = H_c_b[2, 0]
        val h21 = H_c_b[2, 1]

        val fsquare = -(h00 * h01 + h10 * h11) / (h20 * h21)
        return sqrt(fsquare)
    }

    fun rigidBodyMotion(H_c_b: D2Array<Double>, f: Double): RecoveryFromHomography {
        val K_c = mk.ndarray(
            arrayOf(
                doubleArrayOf(f, 0.0, 0.0),
                doubleArrayOf(0.0, f, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0)
            )
        )
        var V = mk.linalg.inv(K_c).dot(H_c_b)

        V = V / mk.linalg.norm(V[0..2, 0].reshape(3, 1))

        val rx = V[0..2, 0]
        val ry = V[0..2, 1] / mk.linalg.norm(V[0..2, 1].reshape(3, 1))
        val rz = Utils.cross(rx, ry)
        val R_c_b = rx.append(ry).append(rz).reshape(3, 3).transpose()
        val t_c_cb = V[0..2, 2].reshape(3, 1)

        return RecoveryFromHomography(R_c_b, t_c_cb, f, f)
    }

    fun android_ar(video_frame: Mat) {
        val reference_keypoints = MatOfKeyPoint()
        val reference_descriptors = Mat()
        sift.detectAndCompute(reference_image, Mat(), reference_keypoints, reference_descriptors)

        // Detect and compute keypoints and descriptors for the video_frame
        val frame_keypoints = MatOfKeyPoint()
        val frame_descriptors = Mat()
        sift.detectAndCompute(video_frame, Mat(), frame_keypoints, frame_descriptors)

        val frame_keypoints_list = frame_keypoints.toList()
        val reference_keypoints_list = reference_keypoints.toList()

        val matches = MatOfDMatch()
        matcher.match(frame_descriptors, reference_descriptors, matches)

        val dstPoints = matches.toArray().map { m: DMatch -> frame_keypoints_list[m.queryIdx].pt }
        val srcPoints =
            matches.toArray().map { m: DMatch -> reference_keypoints_list[m.trainIdx].pt }

        val dstPtsCoords = MatOfPoint2f()
        val srcPtsCoords = MatOfPoint2f()
        dstPtsCoords.fromList(dstPoints)
        srcPtsCoords.fromList(srcPoints)

        // Find the homography matrix H using RANSAC
        val H = Calib3d.findHomography(srcPtsCoords, dstPtsCoords, Calib3d.RANSAC, 5.0)

        val ref_corners = mk.ndarray(
            arrayOf(
                intArrayOf(0, 0),
                intArrayOf(reference_image.size(1) - 1, 0),  // reference_image.size(1) = width
                intArrayOf(
                    reference_image.size(1) - 1,
                    reference_image.size(0) - 1
                ), // reference_image.size(0) = height
                intArrayOf(0, reference_image.size(0) - 1)
            )
        ).reshape(-1, 1, 2)
        val x = Mat(4,2,CvType.CV_32FC2)
        x.put(ref_corners.toIntArray())
        val ref_corners_transformed = Mat(4,2,CvType.CV_32FC2)
        perspectiveTransform(x, ref_corners_transformed, H)

    }
}