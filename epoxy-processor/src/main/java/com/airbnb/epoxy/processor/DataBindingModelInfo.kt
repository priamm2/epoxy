package com.airbnb.epoxy.processor

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XTypeElement
import com.airbnb.epoxy.processor.ClassNames.EPOXY_DATA_BINDING_HOLDER
import com.airbnb.epoxy.processor.ClassNames.EPOXY_DATA_BINDING_MODEL
import com.airbnb.epoxy.processor.resourcescanning.ResourceValue
import com.squareup.javapoet.ClassName

internal class DataBindingModelInfo(
    val layoutResource: ResourceValue,
    val moduleName: String,
    private val layoutPrefix: String = "",
    val enableDoNotHash: Boolean,
    val annotatedElement: XElement,
    memoizer: Memoizer
) : GeneratedModelInfo(memoizer) {
    private val dataBindingClassName: ClassName

    private var dataBindingClassElement: XTypeElement? = null
        get() {
            if (field == null) {
                field = memoizer.environment.findTypeElement(dataBindingClassName)
            }
            return field
        }

    init {
        dataBindingClassName = getDataBindingClassNameForResource(layoutResource, moduleName)

        superClassElement = memoizer.epoxyDataBindingModelBaseClass ?: error("Epoxy Databinding library not found")
        superClassName = EPOXY_DATA_BINDING_MODEL
        generatedName = buildGeneratedModelName()
        parameterizedGeneratedName = generatedName
        modelType = EPOXY_DATA_BINDING_HOLDER
        shouldGenerateModel = true

        collectMethodsReturningClassType(superClassElement)
    }

    fun parseDataBindingClass(logger: Logger): XTypeElement? {
        val dataBindingClassElement = this.dataBindingClassElement ?: return null
        val hashCodeValidator = HashCodeValidator(memoizer.environment, memoizer, logger)

        dataBindingClassElement.getDeclaredMethods()
            .filter { Utils.isSetterMethod(it) }
            .map {
                DataBindingAttributeInfo(this, it, hashCodeValidator, memoizer)
            }
            .filter { it.fieldName !in FIELD_NAME_BLACKLIST }
            .let {
                addAttributes(it)
            }

        return dataBindingClassElement
    }

    private fun getDataBindingClassNameForResource(
        layoutResource: ResourceValue,
        moduleName: String
    ): ClassName {
        val modelName = layoutResource.resourceName?.toUpperCamelCase()?.plus(BINDING_SUFFIX)
            ?: error("Resource name not found for layout: ${layoutResource.debugDetails()}")

        return ClassName.get("$moduleName.databinding", modelName)
    }

    private fun buildGeneratedModelName(): ClassName {
        val modelName = layoutResource.resourceName!!
            .removePrefix(layoutPrefix)
            .toUpperCamelCase()
            .plus(BINDING_SUFFIX)
            .plus(GENERATED_MODEL_SUFFIX)

        return ClassName.get(moduleName, modelName)
    }

    companion object {

        const val BINDING_SUFFIX = "Binding"

        val FIELD_NAME_BLACKLIST = listOf(
            "lifecycleOwner"
        )
    }

    override fun additionalOriginatingElements() =
        listOfNotNull(annotatedElement, dataBindingClassElement)
}
