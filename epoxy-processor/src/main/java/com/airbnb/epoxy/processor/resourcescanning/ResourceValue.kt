package com.airbnb.epoxy.processor.resourcescanning

import com.airbnb.epoxy.processor.ClassNames.ANDROID_R
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock

class ResourceValue {

    val className: ClassName?
    val resourceName: String?
    val value: Int
    val code: CodeBlock
    val qualified: Boolean
    private val resourceType: String? get() = className?.simpleName()
    val rClass: ClassName? get() = className?.topLevelClassName()

    constructor(value: Int) {
        this.value = value
        code = CodeBlock.of("\$L", value)
        qualified = false
        resourceName = null
        className = null
    }

    constructor(
        className: ClassName,
        resourceName: String,
        value: Int
    ) {
        this.className = className
        this.resourceName = resourceName
        this.value = value
        code = if (className.topLevelClassName() == ANDROID_R)
            CodeBlock.of("\$L.\$N", className, resourceName)
        else
            CodeBlock.of("\$T.\$N", className, resourceName)
        qualified = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ResourceValue

        if (value != other.value) return false
        return code == other.code
    }

    override fun hashCode(): Int {
        var result = value
        result = 31 * result + code.hashCode()
        return result
    }

    override fun toString(): String {
        throw UnsupportedOperationException("Please use value or code explicitly")
    }

    fun debugDetails(): String = code.toString()

    fun isStringResource(): Boolean = resourceType == "string"
}
