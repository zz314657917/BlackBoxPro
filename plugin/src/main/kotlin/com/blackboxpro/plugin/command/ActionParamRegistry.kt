package com.blackboxpro.plugin.command

import com.blackboxpro.common.action.ActionCatalog

/**
 * Action 参数元数据注册表。
 *
 * 为每个 action 定义有序参数名列表，用于扁平化命令的 Tab 补全提示。
 */
object ActionParamRegistry {

    /** 获取所有已注册的 action ID */
    fun getActionIds(): List<String> = ActionCatalog.getActionIds()

    /** 获取指定 action 的参数名列表，未注册返回 null */
    fun getParams(action: String): List<String>? = ActionCatalog.getParams(action)
}
