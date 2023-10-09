package net.java.sip.communicator.service.keybindings;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

/**
 * Convenience methods providing a quick means of loading and saving
 * keybindings. None preserve disabled mappings. The formats provided are as
 * follows:<br>
 * SERIAL_HASH- Serialized hash map of bindings. Ordering is preserved if
 * available.<br>
 * SERIAL_INPUT- Serialized input map of bindings.<br>
 * PROPERTIES_PAIR- Persistence provided by java.util.Properties using plain
 * text key/value pairs.<br>
 * PROPERTIES_XML- Persistence provided by java.util.Properties using its XML
 * format.
 *
 * @author Damian Johnson (atagar1@gmail.com)
 * @version September 21, 2007
 */
public enum Persistence
{
    SERIAL_HASH, SERIAL_INPUT, PROPERTIES_PAIRS, PROPERTIES_XML;

    private static final String PROPERTIES_COMMENT =
        "Keybindings (mapping of KeyStrokes to string representations of actions)";

    /**
     * Attempts to load this type of persistent keystroke map from a given path.
     * This is unable to parse any null content.
     *
     * @param path absolute path to resource to be loaded
     * @return keybinding map reflecting file contents
     * @throws IOException if unable to load resource
     * @throws ParseException if unable to parse content
     */
    public LinkedHashMap<KeyStroke, String> load(String path)
        throws IOException,
        ParseException
    {
        return load(new FileInputStream(path));
    }

    /**
     * Attempts to load this type of persistent keystroke map from a given
     * stream. This is unable to parse any null content.
     *
     * @param input source of keybindings to be parsed
     * @return keybinding map reflecting file contents
     * @throws IOException if unable to load resource
     * @throws ParseException if unable to parse content
     */
    public LinkedHashMap<KeyStroke, String> load(InputStream input)
        throws IOException,
        ParseException
    {
        LinkedHashMap<KeyStroke, String> output =
                new LinkedHashMap<>();

        if (this == SERIAL_HASH || this == SERIAL_INPUT)
        {
            Object instance = null; // Loaded serialized object

            try
            {
                ObjectInputStream objectInput = new ObjectInputStream(input);
                instance = objectInput.readObject();
                objectInput.close();
            }
            catch (ClassNotFoundException exc)
            {
                throw new ParseException("Unable to load serialized content", 0);
            }

            if (this == SERIAL_HASH)
            {
                if (!(instance instanceof HashMap<?, ?>))
                {
                    throw new ParseException(
                        "Serialized resource doesn't represent a HashMap", 0);
                }

                HashMap<?, ?> mapping = (HashMap<?, ?>)instance;
                for (Object key : mapping.keySet())
                {
                    Object value = mapping.get(key);

                    if (key instanceof KeyStroke && value instanceof String)
                    {
                        output.put((KeyStroke) key, (String) value);
                    }
                    else
                    {
                        if (key == null || value == null)
                        {
                            throw new ParseException(
                                "Unable to load null content", 0);
                        }
                        else
                        {
                            StringBuilder message = new StringBuilder();
                            message
                                .append("Entry doesn't represent a keybinding: ");
                            message.append(key.getClass().getName());
                            message.append(" -> ");
                            message.append(value.getClass().getName());
                            message
                                .append("\nMust match KeyStroke -> String mapping");
                            throw new ParseException(message.toString(), 0);
                        }
                    }
                }
            }
            else
            {
                if (!(instance instanceof InputMap))
                {
                    throw new ParseException(
                        "Serialized resource doesn't represent an InputMap", 0);
                }

                InputMap mapping = (InputMap) instance;
                if (mapping.keys() != null)
                {
                    for (KeyStroke shortcut : mapping.keys())
                    {
                        if (shortcut == null || mapping.get(shortcut) == null)
                        {
                            throw new ParseException(
                                "Unable to load null content", 0);
                        }
                        else
                        {
                            output.put(shortcut, mapping.get(shortcut)
                                .toString());
                        }
                    }
                }
            }
        }
        else if (this == PROPERTIES_PAIRS || this == PROPERTIES_XML)
        {
            Properties properties = new Properties();
            if (this == PROPERTIES_PAIRS)
                properties.load(input);
            else if (this == PROPERTIES_XML)
                properties.loadFromXML(input);

            for (Object key : properties.keySet())
            {
                Object value = properties.get(key);

                if (key instanceof String && value instanceof String)
                {
                    KeyStroke keystroke = KeyStroke.getKeyStroke((String) key);
                    if (keystroke == null)
                    {
                        StringBuilder message = new StringBuilder();
                        message
                            .append("Unable to parse keystroke, see the getKeyStroke(String) method of ");
                        message.append(KeyStroke.class.getName());
                        message.append(" for proper format");
                        throw new ParseException(message.toString(), 0);
                    }
                    else
                    {
                        output.put(keystroke, (String) value);
                    }
                }
                else
                {
                    if (key == null || value == null)
                    {
                        throw new ParseException("Unable to load null content",
                            0);
                    }
                    else
                    {
                        StringBuilder message = new StringBuilder();
                        message
                            .append("Entry doesn't represent a keybinding: ");
                        message.append(key.getClass().getName());
                        message.append(" -> ");
                        message.append(value.getClass().getName());
                        message
                            .append("\nMust match String -> String mapping where the first string represents a keystroke");
                        throw new ParseException(message.toString(), 0);
                    }
                }
            }
        }

        input.close();
        return output;
    }

    /**
     * Writes the persistent state of the bindings to an output stream.
     *
     * @param output stream where persistent state should be written
     * @param bindings keybindings to be saved
     * @throws IOException if unable to save bindings
     * @throws UnsupportedOperationException if any keys or values of the
     *             binding are null
     */
    public void save(OutputStream output, Map<KeyStroke, String> bindings)
        throws IOException
    {
        for (KeyStroke key : bindings.keySet())
        {
            if (key == null || bindings.get(key) == null)
            {
                throw new UnsupportedOperationException(
                    "Invalid binding: Shortcuts and actions cannot be null");
            }
        }

        if (this == SERIAL_HASH || this == SERIAL_INPUT)
        {
            Object mapping; // Mapping to be serialized
            if (this == SERIAL_HASH)
                mapping = bindings;
            else
            {
                InputMap inputMap = new InputMap();
                for (KeyStroke shortcut : bindings.keySet())
                {
                    inputMap.put(shortcut, bindings.get(shortcut));
                }
                mapping = inputMap;
            }

            ObjectOutputStream objectOutput = new ObjectOutputStream(output);
            objectOutput.writeObject(mapping);
            objectOutput.flush();
            objectOutput.close();
        }
        else if (this == PROPERTIES_PAIRS || this == PROPERTIES_XML)
        {
            Properties properties = new Properties();
            for (KeyStroke shortcut : bindings.keySet())
            {
                properties.setProperty(shortcut.toString(), bindings
                    .get(shortcut));
            }

            if (this == PROPERTIES_PAIRS)
                properties.store(output, PROPERTIES_COMMENT);
            else
                properties.storeToXML(output, PROPERTIES_COMMENT);
        }
    }

    /**
     * Writes the persistent state of the bindings to a file.
     *
     * @param path absolute path to where bindings should be saved
     * @param bindings keybindings to be saved
     * @throws IOException if unable to save bindings
     * @throws UnsupportedOperationException if any keys or values of the
     *             binding are null
     */
    public void save(String path, Map<KeyStroke, String> bindings)
        throws IOException
    {
        FileOutputStream output = new FileOutputStream(path);
        try
        {
            save(output, bindings);
            output.flush();
            output.close();
        }
        catch (IOException | UnsupportedOperationException exc)
        {
            output.flush();
            output.close();
            throw exc;
        }
    }

    @Override
    public String toString()
    {
        if (this == SERIAL_HASH)
            return "Serialized Hash Map";
        else if (this == SERIAL_INPUT)
            return "Serialized Input Map";
        else if (this == PROPERTIES_XML)
            return "Properties XML";
        else
            return getReadableConstant(this.name());
    }

    /**
     * Provides a more readable version of constant names. Spaces replace
     * underscores and this changes the input to lowercase except the first
     * letter of each word. For instance, "RARE_CARDS" would become
     * "Rare Cards".
     *
     * @param input string to be converted
     * @return reader friendly variant of constant name
     */
    public static String getReadableConstant(String input)
    {
        char[] name = input.toCharArray();

        boolean isStartOfWord = true;
        for (int i = 0; i < name.length; ++i)
        {
            char chr = name[i];
            if (chr == '_')
                name[i] = ' ';
            else if (isStartOfWord)
                name[i] = Character.toUpperCase(chr);
            else
                name[i] = Character.toLowerCase(chr);
            isStartOfWord = chr == '_';
        }

        return new String(name);
    }
}
