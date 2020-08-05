package com.github.sparkycola.connectcanoncamera

import android.util.Log
import org.fourthline.cling.binding.staging.MutableAction
import org.fourthline.cling.binding.staging.MutableActionArgument
import org.fourthline.cling.binding.staging.MutableService
import org.fourthline.cling.binding.xml.Descriptor
import org.fourthline.cling.binding.xml.Descriptor.Service.ATTRIBUTE
import org.fourthline.cling.binding.xml.DescriptorBindingException
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.fourthline.cling.model.XMLUtil
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.CustomDatatype
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory


class IminkServiceDescriptionBinderImpl : UDA10ServiceDescriptorBinderImpl() {
    private val TAG = "IminkSrvcDescBindImpl"

    enum class iminkDirection(val upnpDirection: ActionArgument.Direction){
        get(ActionArgument.Direction.OUT),
        set(ActionArgument.Direction.IN)
    }

    @Throws(DescriptorBindingException::class)
    override fun buildDOM(service: Service<*, *>): Document? {
        return try {
            Log.d(TAG, "Generating XML descriptor from service model: $service")
            val factory =
                DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val d = factory.newDocumentBuilder().newDocument()
            generateScpd(service, d)
            d
        } catch (ex: Exception) {
            throw DescriptorBindingException(
                "Could not generate service descriptor: " + ex.message,
                ex
            )
        }
    }

    //no changes made, just some extra debugging output in case it doesn't work (with other cameras)
    override fun hydrateBasic(
        descriptor: MutableService,
        undescribedService: Service<*, *>
    ) {
        descriptor.serviceId = undescribedService.serviceId
        descriptor.serviceType = undescribedService.serviceType
        Log.d(TAG, "service found id: ${descriptor.serviceId} type: ${descriptor.serviceType}")
        if (undescribedService is RemoteService) {
            val rs = undescribedService
            descriptor.controlURI = rs.controlURI
            descriptor.eventSubscriptionURI = rs.eventSubscriptionURI
            descriptor.descriptorURI = rs.descriptorURI
            Log.d(TAG, "it's remote and controlURI: ${descriptor.controlURI}, " +
                    "eventsub: ${descriptor.eventSubscriptionURI}" +
                    "descriptorURI: ${descriptor.descriptorURI}")
        }
    }

    @Throws(DescriptorBindingException::class)
    override fun hydrateRoot(
        descriptor: MutableService?,
        rootElement: Element
    ) {

        // We don't check the XMLNS, nobody bothers anyway...
        if (!Descriptor.Service.ELEMENT.scpd.equals(rootElement)) {
            throw DescriptorBindingException("Root element name is not <scpd>: " + rootElement.nodeName)
        }
        val rootChildren = rootElement.childNodes
        for (i in 0 until rootChildren.length) {
            val rootChild = rootChildren.item(i)
            if (rootChild.nodeType != Node.ELEMENT_NODE) continue
            when {
                Descriptor.Service.ELEMENT.specVersion.equals(
                    rootChild
                ) -> {
                    // We don't care about UDA major/minor specVersion anymore - whoever had the brilliant idea that
                    // the spec versions can be declared on devices _AND_ on their services should have their fingers
                    // broken so they never touch a keyboard again.
                    // hydrateSpecVersion(descriptor, rootChild);
                }
                Descriptor.Service.ELEMENT.actionList.equals(
                    rootChild
                ) -> {
                    hydrateActionList(descriptor, rootChild)
                }
                Descriptor.Service.ELEMENT.serviceStateTable.equals(
                    rootChild
                ) -> {
                    hydrateServiceStateTableList(descriptor, rootChild)
                }
                else -> {
                    Log.d(TAG,"Ignoring unknown element: " + rootChild.nodeName)
                }
            }
        }

        if(descriptor?.serviceType == MCC_SERVICE_TYPE){
            //Todo: generate fake service state table
        }
    }

    override fun hydrateAction(action: MutableAction, actionNode: Node) {
        val actionNodeChildren = actionNode.childNodes
        for (i in 0 until actionNodeChildren.length) {
            val actionNodeChild = actionNodeChildren.item(i)
            if (actionNodeChild.nodeType != Node.ELEMENT_NODE) continue
            if (Descriptor.Service.ELEMENT.name.equals(
                    actionNodeChild
                )
            ) {
                action.name = XMLUtil.getTextContent(actionNodeChild)
            } else if (Descriptor.Service.ELEMENT.argumentList.equals(
                    actionNodeChild
                )
            ) {
                val argumentChildren = actionNodeChild.childNodes
                for (j in 0 until argumentChildren.length) {
                    val actionArgument = MutableActionArgument()
                    val argumentChild = argumentChildren.item(j)
                    if (argumentChild.nodeType != Node.ELEMENT_NODE) continue
                    when(argumentChild.nodeName){
                        "argumentList" -> {
                            val actionArgument = MutableActionArgument()
                            hydrateActionArgument(actionArgument, argumentChild)
                            action.arguments.add(actionArgument)
                        }
                        //imink arguments
                        //Todo: check namespace, just for cleanliness
                        "X_actKind" -> {
                            actionArgument.direction =
                                iminkDirection.valueOf(argumentChild.textContent.toLowerCase()).upnpDirection
                        }

                        "X_resourceName" -> {
                            actionArgument.relatedStateVariable = argumentChild.textContent
                        }
                    }
                }
            }
        }
    }

    //we have to override all of those because they're private

    private fun generateScpd(
        serviceModel: Service<*, *>,
        descriptor: Document
    ) {
        val scpdElement = descriptor.createElementNS(
            Descriptor.Service.NAMESPACE_URI,
            Descriptor.Service.ELEMENT.scpd.toString()
        )
        descriptor.appendChild(scpdElement)
        generateSpecVersion(serviceModel, descriptor, scpdElement)
        if (serviceModel.hasActions()) {
            generateActionList(serviceModel, descriptor, scpdElement)
        }
        //IMINK config does not contain the serviceStateTable, so I'm skipping it
        if (serviceModel.serviceType == CCM_SERVICE_TYPE) {
            Log.d(TAG, "Cannon connect service found, skipping service table generation")
        } else {
            generateServiceStateTable(serviceModel, descriptor, scpdElement)
        }
        //Log.d(TAG," ${XMLUtil.documentToString(descriptor)}")
    }

    private fun generateSpecVersion(
        serviceModel: Service<*, *>,
        descriptor: Document,
        rootElement: Element
    ) {
        val specVersionElement = XMLUtil.appendNewElement(
            descriptor,
            rootElement,
            Descriptor.Service.ELEMENT.specVersion
        )
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            specVersionElement,
            Descriptor.Service.ELEMENT.major,
            serviceModel.device.version.major
        )
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            specVersionElement,
            Descriptor.Service.ELEMENT.minor,
            serviceModel.device.version.minor
        )
    }

    private fun generateActionList(
        serviceModel: Service<*, *>,
        descriptor: Document,
        scpdElement: Element
    ) {
        val actionListElement: Element
        if (serviceModel.serviceType == CCM_SERVICE_TYPE){
            actionListElement = XMLUtil.appendNewElement(
                    descriptor,
            scpdElement,
            Descriptor.Service.ELEMENT.actionList
            )
        }else{
            actionListElement = XMLUtil.appendNewElement(
                descriptor,
                scpdElement,
                Descriptor.Service.ELEMENT.actionList
            )

        }
        for (action in serviceModel.actions) {
            if (action.name != QueryStateVariableAction.ACTION_NAME) generateAction(
                serviceModel,
                action,
                descriptor,
                actionListElement
            )
        }
    }

    private fun generateAction(
        serviceModel: Service<*, *>,
        action: Action<*>,
        descriptor: Document,
        actionListElement: Element
    ) {
        val actionElement = XMLUtil.appendNewElement(
            descriptor,
            actionListElement,
            Descriptor.Service.ELEMENT.action
        )
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            actionElement,
            Descriptor.Service.ELEMENT.name,
            action.name
        )
        if (action.hasArguments()) {
            if (serviceModel.serviceType == CCM_SERVICE_TYPE) {
                Log.d(
                    TAG,
                    "Cannon connect service found, generating alternative (imink) argument list"
                )
                var actType = ""
                when (action.arguments[0]?.direction.toString().toLowerCase(Locale.ROOT)) {
                    "out" -> actType = "Get"
                    "in" -> actType = "Set"
                }
                XMLUtil.appendNewElementIfNotNull(
                    descriptor,
                    actionElement,
                    "X_actKind",
                    actType,
                    IMINK_NAMESPACE
                ).prefix = "ns"
                XMLUtil.appendNewElementIfNotNull(
                    descriptor,
                    actionElement,
                    "X_resourceName",
                    action.arguments[0]?.relatedStateVariableName,
                    IMINK_NAMESPACE
                ).prefix = "ns"


            } else {
                val argumentListElement = XMLUtil.appendNewElement(
                    descriptor,
                    actionElement,
                    Descriptor.Service.ELEMENT.argumentList
                )
                for (actionArgument in action.arguments) {
                    generateActionArgument(actionArgument, descriptor, argumentListElement)
                }
            }
        }
    }

    private fun generateActionArgument(
        actionArgument: ActionArgument<*>,
        descriptor: Document,
        actionElement: Element
    ) {
        val actionArgumentElement = XMLUtil.appendNewElement(
            descriptor,
            actionElement,
            Descriptor.Service.ELEMENT.argument
        )
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            actionArgumentElement,
            Descriptor.Service.ELEMENT.name,
            actionArgument.name
        )
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            actionArgumentElement,
            Descriptor.Service.ELEMENT.direction,
            actionArgument.direction.toString().toLowerCase(Locale.ROOT)
        )
        if (actionArgument.isReturnValue) {
            // TODO: UPNP VIOLATION: WMP12 will discard RenderingControl service if it contains <retval> tags
            Log.w(
                TAG,
                "UPnP specification violation: Not producing <retval> element to be compatible with WMP12: $actionArgument"
            )
            // appendNewElement(descriptor, actionArgumentElement, ELEMENT.retval);
        }
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            actionArgumentElement,
            Descriptor.Service.ELEMENT.relatedStateVariable,
            actionArgument.relatedStateVariableName
        )
    }

    private fun generateServiceStateTable(
        serviceModel: Service<*, *>,
        descriptor: Document,
        scpdElement: Element
    ) {
        val serviceStateTableElement = XMLUtil.appendNewElement(
            descriptor,
            scpdElement,
            Descriptor.Service.ELEMENT.serviceStateTable
        )
        for (stateVariable in serviceModel.stateVariables) {
            generateStateVariable(stateVariable, descriptor, serviceStateTableElement)
        }
    }

    private fun generateStateVariable(
        stateVariable: StateVariable<*>,
        descriptor: Document,
        serviveStateTableElement: Element
    ) {
        val stateVariableElement = XMLUtil.appendNewElement(
            descriptor,
            serviveStateTableElement,
            Descriptor.Service.ELEMENT.stateVariable
        )
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            stateVariableElement,
            Descriptor.Service.ELEMENT.name,
            stateVariable.name
        )
        if (stateVariable.typeDetails.datatype is CustomDatatype) {
            XMLUtil.appendNewElementIfNotNull(
                descriptor,
                stateVariableElement,
                Descriptor.Service.ELEMENT.dataType,
                (stateVariable.typeDetails.datatype as CustomDatatype).name
            )
        } else {
            XMLUtil.appendNewElementIfNotNull(
                descriptor,
                stateVariableElement,
                Descriptor.Service.ELEMENT.dataType,
                stateVariable.typeDetails.datatype.builtin.descriptorName
            )
        }
        XMLUtil.appendNewElementIfNotNull(
            descriptor,
            stateVariableElement,
            Descriptor.Service.ELEMENT.defaultValue,
            stateVariable.typeDetails.defaultValue
        )

        // The default is 'yes' but we generate it anyway just to be sure
        if (stateVariable.eventDetails.isSendEvents) {
            stateVariableElement.setAttribute(ATTRIBUTE.sendEvents.toString(), "yes")
        } else {
            stateVariableElement.setAttribute(ATTRIBUTE.sendEvents.toString(), "no")
        }
        if (stateVariable.typeDetails.allowedValues != null) {
            val allowedValueListElement = XMLUtil.appendNewElement(
                descriptor,
                stateVariableElement,
                Descriptor.Service.ELEMENT.allowedValueList
            )
            for (allowedValue in stateVariable.typeDetails.allowedValues) {
                XMLUtil.appendNewElementIfNotNull(
                    descriptor,
                    allowedValueListElement,
                    Descriptor.Service.ELEMENT.allowedValue,
                    allowedValue
                )
            }
        }
        if (stateVariable.typeDetails.allowedValueRange != null) {
            val allowedValueRangeElement = XMLUtil.appendNewElement(
                descriptor,
                stateVariableElement,
                Descriptor.Service.ELEMENT.allowedValueRange
            )
            XMLUtil.appendNewElementIfNotNull(
                descriptor,
                allowedValueRangeElement,
                Descriptor.Service.ELEMENT.minimum,
                stateVariable.typeDetails.allowedValueRange.minimum
            )
            XMLUtil.appendNewElementIfNotNull(
                descriptor,
                allowedValueRangeElement,
                Descriptor.Service.ELEMENT.maximum,
                stateVariable.typeDetails.allowedValueRange.maximum
            )
            if (stateVariable.typeDetails.allowedValueRange.step >= 1L) {
                XMLUtil.appendNewElementIfNotNull(
                    descriptor,
                    allowedValueRangeElement,
                    Descriptor.Service.ELEMENT.step,
                    stateVariable.typeDetails.allowedValueRange.step
                )
            }
        }
    }
}