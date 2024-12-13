package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.platform.diagnostic.telemetry.impl.rootTask
import org.ini4j.Ini
import java.io.File
import kotlin.io.path.fileVisitor
import kotlin.math.log

class AnsibleVaultSecret {

    companion object {


        fun search(): AnsibleVaultSecret {
            val logger = Logger.getInstance(AnsibleVaultSecret.Companion::class.java)

            val projects = ProjectManager.getInstance().openProjects;
            projects
                .flatMap { project ->
                    ModuleManager.getInstance(project).modules.asSequence()
                }
                .flatMap { module ->
                    ModuleRootManager.getInstance(module).contentRoots.asSequence()
                }
                .flatMap { root ->
                    File(root.path).walk(FileWalkDirection.TOP_DOWN)
                        .filter { file -> file.isFile }
                        .filter { file -> file.name == "ansible.cfg" } //TODO: use also "ANSIBLE_CONFIG", ~/.ansible.cfg, /etc/ansible/ansible.cfg
                        .asSequence()
                }
                .map { file ->
                    val ini = Ini(file)
                    ini
                }
            return AnsibleVaultSecret()
        }
    }

}
