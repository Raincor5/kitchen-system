package com.fft.kitchen.data

data class LabelElement(
    val id: String,
    var text: String,
    var x: Float,
    var y: Float,
    var fontSize: Float,
    var isBold: Boolean = false,
    var fieldType: FieldType
)

data class LabelLayout(
    val elements: MutableList<LabelElement>,
    var name: String,
    var width: Int,
    var height: Int
) {
    companion object {
        fun createDefault(): LabelLayout {
            return LabelLayout(
                elements = mutableListOf(
                    LabelElement("product_name", "Product Name", 10f, 10f, 24f, true, FieldType.PRODUCT_NAME),
                    LabelElement("batch", "Batch: ", 10f, 40f, 18f, false, FieldType.BATCH_NO),
                    LabelElement("prep_by", "Prep by: ", 10f, 70f, 18f, false, FieldType.EMPLOYEE_NAME),
                    LabelElement("prep_date", "Prep: ", 10f, 100f, 18f, false, FieldType.PREP_DATE),
                    LabelElement("use_by", "Use by: ", 10f, 130f, 18f, false, FieldType.USE_BY_DATE)
                ),
                name = "Default Layout",
                width = 400,
                height = 200
            )
        }
    }
}

enum class FieldType {
    PRODUCT_NAME,
    BATCH_NO,
    EMPLOYEE_NAME,
    PREP_DATE,
    USE_BY_DATE,
    CHECK_DATE,
    CUSTOM_TEXT
} 