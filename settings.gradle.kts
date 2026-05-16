rootProject.name = "ItemsAdderAdditions"

fun enabled(name: String): Boolean =
    settings.providers.gradleProperty(name).orElse("true").get().toBoolean()

include("nms:api")
if (enabled("enable_nms_v26_1_2")) include("nms:nms_v26_1_2")
if (enabled("enable_nms_v1_21_11")) include("nms:nms_v1_21_11")
if (enabled("enable_nms_v1_21_10")) include("nms:nms_v1_21_10")
if (enabled("enable_nms_v1_21_8")) include("nms:nms_v1_21_8")
if (enabled("enable_nms_v1_21_7")) include("nms:nms_v1_21_7")
if (enabled("enable_nms_v1_21_6")) include("nms:nms_v1_21_6")
if (enabled("enable_nms_v1_21_5")) include("nms:nms_v1_21_5")
if (enabled("enable_nms_v1_21_4")) include("nms:nms_v1_21_4")
if (enabled("enable_nms_v1_21_3")) include("nms:nms_v1_21_3")
if (enabled("enable_nms_v1_21_1")) include("nms:nms_v1_21_1")
if (enabled("enable_nms_v1_20_6")) include("nms:nms_v1_20_6")
