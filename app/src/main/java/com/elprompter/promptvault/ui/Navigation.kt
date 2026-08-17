package com.elprompter.promptvault.ui

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RULES = "rules"
    const val ADD_EDIT_RULE = "add_edit_rule?ruleId={ruleId}"
    fun addEditRule(ruleId: String? = null) = "add_edit_rule?ruleId=${ruleId ?: ""}"
    const val ACTIVITY_LOG = "activity_log"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
    const val SKIPPED_FILES = "skipped_files"
    const val PANDUAN = "panduan"
}
