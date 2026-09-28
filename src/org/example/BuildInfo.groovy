package org.example

class BuildInfo implements Serializable {
    private def script

    BuildInfo(script) {
        this.script = script
    }

    String summary() {
        return "${script.env.JOB_NAME} #${script.env.BUILD_NUMBER} (branch: ${script.env.BRANCH_NAME ?: 'n/a'})"
    }

    void show() {
        script.echo "Build: ${summary()}"
    }
}