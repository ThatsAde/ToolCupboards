rootProject.name = "ToolCupboards"

// :api  -> published surface, no internals.
// :core -> everything else, plus the bootstrap and resources. It produces the plugin jar,
//          with :api bundled inside it. The root project holds no sources.
include("api", "core")
