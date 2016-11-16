
package ch.mobi.xml.datatype.common.commons.v3;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for ValidationException complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ValidationException">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="validationContext" type="{http://xml.mobi.ch/datatype/common/Commons/v3}ValidationContext" minOccurs="0"/>
 *         &lt;element name="message" type="{http://xml.mobi.ch/datatype/common/Commons/v3}Message" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="localizedMessage" type="{http://xml.mobi.ch/datatype/common/Commons/v3}LocalizedMessage" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ValidationException", propOrder = {
    "validationContext",
    "message",
    "localizedMessage"
})
public class ValidationException {

    protected ValidationContext validationContext;
    protected List<Message> message;
    protected List<LocalizedMessage> localizedMessage;

    /**
     * Default no-arg constructor
     * 
     */
    public ValidationException() {
        super();
    }

    /**
     * Fully-initialising value constructor
     * 
     */
    public ValidationException(final ValidationContext validationContext, final List<Message> message, final List<LocalizedMessage> localizedMessage) {
        this.validationContext = validationContext;
        this.message = message;
        this.localizedMessage = localizedMessage;
    }

    /**
     * Gets the value of the validationContext property.
     * 
     * @return
     *     possible object is
     *     {@link ValidationContext }
     *     
     */
    public ValidationContext getValidationContext() {
        return validationContext;
    }

    /**
     * Sets the value of the validationContext property.
     * 
     * @param value
     *     allowed object is
     *     {@link ValidationContext }
     *     
     */
    public void setValidationContext(ValidationContext value) {
        this.validationContext = value;
    }

    /**
     * Gets the value of the message property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the message property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMessage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Message }
     * 
     * 
     */
    public List<Message> getMessage() {
        if (message == null) {
            message = new ArrayList<Message>();
        }
        return this.message;
    }

    /**
     * Gets the value of the localizedMessage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the localizedMessage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLocalizedMessage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LocalizedMessage }
     * 
     * 
     */
    public List<LocalizedMessage> getLocalizedMessage() {
        if (localizedMessage == null) {
            localizedMessage = new ArrayList<LocalizedMessage>();
        }
        return this.localizedMessage;
    }

}