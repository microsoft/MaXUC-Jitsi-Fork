/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormField.Type;
import org.jivesoftware.smackx.xdata.ListMultiFormField;
import org.jivesoftware.smackx.xdata.ListSingleFormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;

import net.java.sip.communicator.service.protocol.ChatRoomConfigurationFormField;

/**
 * The Jabber protocol implementation of the
 * <tt>ChatRoomConfigurationFormField</tt>. This implementation is based on the
 * smack Form and FormField types.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomConfigurationFormFieldJabberImpl
    implements ChatRoomConfigurationFormField
{
    /**
     * The smack library for field.
     */
    private final FormField smackFormField;

    /**
     * The smack library submit form field. It's the one that will care all
     * values set by user, before submitting the form.
     */
    private FormField smackSubmitFormField;

    private FillableForm smackSubmitForm;

    /**
     * Creates an instance of <tt>ChatRoomConfigurationFormFieldJabberImpl</tt>
     * by passing to it the smack form field and the smack submit form, which
     * are the base of this implementation.
     *
     * @param formField the smack form field
     * @param submitForm the smack submit form
     */
    public ChatRoomConfigurationFormFieldJabberImpl(
        FormField formField,
        FillableForm submitForm)
    {
        this.smackFormField = formField;
        this.smackSubmitForm = submitForm;

        reloadFormField();
    }

    /**
     * Returns the variable name of the corresponding smack property.
     *
     * @return the variable name of the corresponding smack property
     */
    public String getName()
    {
        return smackFormField.getFieldName();
    }

    /**
     * Returns the description of the corresponding smack property.
     *
     * @return the description of the corresponding smack property
     */
    public String getDescription()
    {
        return smackFormField.getDescription();
    }

    /**
     * Returns the label of the corresponding smack property.
     *
     * @return the label of the corresponding smack property
     */
    public String getLabel()
    {
        return smackFormField.getLabel();
    }

    /**
     * Returns the options of the corresponding smack property.
     *
     * @return the options of the corresponding smack property
     */
    public Iterator<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        Iterator<FormField.Option> smackOptions = Collections.emptyIterator();
        FormField.Type smackType = smackFormField.getType();

        if (smackType == Type.list_single)
        {
            ListSingleFormField smackFormFieldWithOptions =
                (ListSingleFormField)smackFormField;
            smackOptions = smackFormFieldWithOptions.getOptions().iterator();
        }
        else if (smackType == Type.list_multi)
        {
            ListMultiFormField smackFormFieldWithOptions =
                (ListMultiFormField)smackFormField;
            smackOptions = smackFormFieldWithOptions.getOptions().iterator();
        }

        while(smackOptions.hasNext())
        {
            FormField.Option smackOption = smackOptions.next();

            options.add(smackOption.getValue().getValue().toString());
        }

        return Collections.unmodifiableList(options).iterator();
    }

    /**
     * Returns the isRequired property of the corresponding smack property.
     *
     * @return the isRequired property of the corresponding smack property
     */
    public boolean isRequired()
    {
        return smackFormField.isRequired();
    }

    /**
     * For each of the smack form field types returns the corresponding
     * <tt>ChatRoomConfigurationFormField</tt> type.
     *
     * @return the type of the property
     */
    public String getType()
    {
        FormField.Type smackType = smackFormField.getType();

        String typeString;

        switch (smackType)
        {
            case bool:
                typeString = TYPE_BOOLEAN;
            case fixed:
                typeString = TYPE_TEXT_FIXED;
            case text_private:
                typeString = TYPE_TEXT_PRIVATE;
            case text_single:
                typeString = TYPE_TEXT_SINGLE;
            case text_multi:
                typeString = TYPE_TEXT_MULTI;
            case list_single:
                typeString = TYPE_LIST_SINGLE;
            case list_multi:
                typeString = TYPE_LIST_MULTI;
            case jid_single:
                typeString = TYPE_ID_SINGLE;
            case jid_multi:
                typeString = TYPE_ID_MULTI;
            default:
                typeString = TYPE_UNDEFINED;
        }

        return typeString;
    }

    /**
     * Returns an Iterator over the list of values of this field.
     *
     * @return an Iterator over the list of values of this field
     */
    public Iterator<?> getValues()
    {
        Iterator<? extends CharSequence> smackValues =
            smackFormField.getValues().iterator();
        Iterator<?> valuesIter;

        if (smackFormField.getType().equals(Type.bool))
        {
            List<Boolean> values = new ArrayList<>();

            while (smackValues.hasNext())
            {
                String smackValue = smackValues.next().toString();

                values
                    .add(
                        (smackValue.equals("1") || smackValue.equals("true"))
                            ? Boolean.TRUE
                            : Boolean.FALSE);
            }

            valuesIter = values.iterator();
        }
        else
            valuesIter = smackValues;

        return valuesIter;
    }

    /**
     * Adds the given value to the list of values of this field.
     *
     * @param value the value to add
     */
    public void addValue(Object value)
    {
        if (value instanceof Boolean)
        {
            value = (Boolean) value ? "1" : "0";
        }

        // The field itself is not writable, only the form containing the field.
        // So we fill in the form then reload the field.
        smackSubmitForm.setAnswer(this.getName(), value.toString());
        reloadFormField();
    }

    /**
     * Sets the given list of values to this field.
     *
     * @param newValues the list of values to set
     */
    public void setValues(Object[] newValues)
    {
        List<String> list = new ArrayList<>();

        for (Object value : newValues)
        {
            String stringValue;

            if (value instanceof Boolean)
            {
                stringValue = (Boolean) value ? "1" : "0";
            }
            else
            {
                stringValue = (value == null) ? null : value.toString();
            }

            list.add(stringValue);
        }

        // The field itself is not writable, only the form containing the field.
        // So we fill in the form then reload the field.
        smackSubmitForm.setAnswer(this.getName(), list);
        reloadFormField();
    }

    private void reloadFormField()
    {
        if (!smackFormField.getType().equals(Type.fixed))
        {
            this.smackSubmitFormField
                = smackSubmitForm.getField(this.getName());
        }
        else
        {
            this.smackSubmitFormField = null;
        }
    }
}
