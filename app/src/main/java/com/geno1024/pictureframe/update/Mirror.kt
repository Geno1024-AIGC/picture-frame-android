package com.geno1024.pictureframe.update

    /**
     * A GitHub "prefix proxy": it serves any GitHub URL when the target URL is
     * appended after the prefix, e.g.
     *
     *     https://ghfast.top/https://github.com/owner/repo/releases/download/...
     *
     * These mirrors are third-party, community-run infrastructure with no SLA.
     * Domains appear and disappear frequently, which is why the list is
     * user-selectable, persisted, and accepts an arbitrary custom prefix rather
     * than being fixed at the call site.
     */

data class Mirror(
    val label: String,
    val prefix: String,
) {
    /** Wraps a GitHub URL for this mirror. Returns it unchanged for [DIRECT]. */
    fun wrap(url: String): String = if (prefix.isEmpty()) url else prefix.trimEnd('/') + "/" + url
}

object Mirrors {

    /** No proxy at all. Usually the most reliable, but can be slow or blocked. */
    val DIRECT = Mirror("直连 GitHub", "")

    /**
     * Only mirrors that actually served a canary APK when this list was last
     * touched are listed. `gh.llkk.cc` and `hub.gitmirror.com` timed out and
     * `gh-proxy.llyke.com` answered with an error page, so they were dropped
     * rather than shipped as dead options. The custom field below covers the
     * long tail without a new build.
     */
    val PRESETS: List<Mirror> = listOf(
        DIRECT,
        Mirror("ghfast.top", "https://ghfast.top"),
        Mirror("gh-proxy.com", "https://gh-proxy.com"),
        Mirror("ghproxy.net", "https://ghproxy.net"),
    )

    const val PREFS = "pictureframe.update"

    const val KEY_SELECTED = "selected_mirror"

    /**
     * Accepts either a known preset prefix or an arbitrary user-typed one, so a
     * dead mirror can be worked around without shipping a new build.
     */
    fun resolve(prefix: String): Mirror {
        val trimmed = prefix.trim()
        PRESETS.firstOrNull { it.prefix == trimmed }?.let { return it }
        return Mirror("自定义", trimmed)
    }
}
