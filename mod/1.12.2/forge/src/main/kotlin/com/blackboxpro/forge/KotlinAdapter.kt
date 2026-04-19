package com.blackboxpro.forge

import net.minecraftforge.fml.common.FMLModContainer
import net.minecraftforge.fml.common.ILanguageAdapter
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 极简 Kotlin 语言适配器，允许 @Mod 注解的 object 作为 Mod 入口。
 * 替代 Forgelin 外部依赖，避免 Kotlin 版本冲突。
 */
class KotlinAdapter : ILanguageAdapter {

    private val logger = LogManager.getLogger("BlackBoxPro-KotlinAdapter")

    override fun getNewInstance(
        container: FMLModContainer,
        objectClass: Class<*>,
        classLoader: ClassLoader,
        factoryMarkedAnnotation: Method?
    ): Any {
        // Kotlin object 编译后会生成一个 static INSTANCE 字段
        return try {
            objectClass.getField("INSTANCE").get(null)
        } catch (e: NoSuchFieldException) {
            // 不是 object，尝试普通构造
            objectClass.getDeclaredConstructor().newInstance()
        }
    }

    override fun supportsStatics(): Boolean = false

    override fun setProxy(target: Field, proxyTarget: Class<*>, proxy: Any) {
        target.set(proxyTarget.getField("INSTANCE").get(null), proxy)
    }

    override fun setInternalProxies(mod: ModContainer?, side: Side?, loader: ClassLoader?) {
        // no-op
    }
}
