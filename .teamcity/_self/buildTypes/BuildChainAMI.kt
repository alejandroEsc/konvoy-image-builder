package _self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.retryBuild
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

class BuildChainAMI(osTarget: String, upstreamDependency: String ) : BuildType({
    id("BuildChainAMI${osTarget}")
    name = "Build Chain - AMI ${osTarget}"

    steps {

        script {
            name = "Build ${osTarget} AMI"
            scriptContent = """
                cat <<EOF > buildchain-overrides.yaml
                build_name_extra: -buildchain
                EOF
                
                make devkit.run WHAT="make ${osTarget} ADDITIONAL_OVERRIDES=buildchain-overrides.yaml ADDITIONAL_ARGS=\"--extra-vars kubernetes-version=%dep.${upstreamDependency}.teamcity.build.branch%\" BUILD_DRY_RUN=true"
            """.trimIndent()
        }

    }
    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            id = "vcsTrigger"
            enabled = false
            branchFilter = """
                +:*
                -:<default>
            """.trimIndent()
        }
        finishBuildTrigger {
            id = "TRIGGER_153"
            buildType = "${upstreamDependency}"
            successfulOnly = true
            branchFilter = """
                +:v*
                -:v1.21.0
            """.trimIndent()
        }
        retryBuild {
            id = "retryBuildTrigger"
            delaySeconds = 120
        }
    }

    dependencies {
        snapshot(AbsoluteId("${upstreamDependency}")) {
        }
    }

    requirements {
        exists("DOCKER_VERSION", "RQ_26")
    }
})
