package com.blackboxpro.plugin.command.testframework

enum class BlackBoxTestProfile {
    SMOKE,
    FULL
}

enum class BlackBoxLoaderProfile {
    MC_1122,
    MC_12111;

    companion object {
        fun detect(bukkitVersion: String): BlackBoxLoaderProfile =
            if (bukkitVersion.startsWith("1.12")) MC_1122 else MC_12111
    }
}

enum class BlackBoxTestStatus {
    PASSED,
    FAILED,
    SKIPPED
}
