package com.blackboxpro.forge.util

import com.blackboxpro.runtime.util.RuntimeBlockFaceUtil

/**
 * 鏂瑰潡闈腑蹇冨潗鏍囪绠楀伐鍏枫€?
 */
object BlockFaceUtil {
    fun calculateFaceCenter(blockX: Int, blockY: Int, blockZ: Int, face: String): RuntimeBlockFaceUtil.FaceCenter =
        RuntimeBlockFaceUtil.calculateFaceCenter(blockX, blockY, blockZ, face)
}

