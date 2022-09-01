/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.skin.*;

/**
 * SipCommunicator default theme.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */

public class SIPCommDefaultTheme
    extends DefaultMetalTheme
    implements Skinnable
{
    /**
     * Used for tooltip borders, progress bar selection background, scroll bar
     * thumb shadow, tabbed pane focus, toolbar docking foreground
     */
    private static ColorUIResource PRIMARY_CONTROL_DARK_SHADOW =
        new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.BORDER_SHADOW"));

    /**
     * Used for desktop color, menu selected background, focus color, slider
     * foreground, progress bar foreground, combo box selection background,
     * scroll bar thumb
     */
    private static ColorUIResource PRIMARY_CONTROL_SHADOW =
        new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.BORDER_SHADOW"));

    /**
     * Used for progress bar border, tooltip border inactive, tooltip foreground
     * inactive, scroll bar dark shadow.
     */
    private static ColorUIResource CONTROL_DARK_SHADOW =
        new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.BORDER_SHADOW"));

    /**
     * Tabbed pane shadow.
     */
    private static ColorUIResource CONTROL_SHADOW =
        new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.BORDER_SHADOW"));

    /**
     * Used for window title inactive background, menu background, tooltip
     * inactive background, combo box background, desktop icon background,
     * scroll bar background, tabbed pane tab area background.
     */
    private static ColorUIResource CONTROL_COLOR =
        new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Used for text hightlight color, window title background, scroll bar thumb
     * hightlight, split pane devider focus color, Tree.line, Tree.hash,
     * ToolBar.floatingForeground
     */
    private static ColorUIResource PRIMARY_CONTROL_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Used to paint a gradient for a check box or a radio button.
     */
    private static ColorUIResource BUTTON_GRADIENT_DARK_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Used to paint a gradient for a check box or a radio button.
     */
    private static ColorUIResource BUTTON_GRADIENT_LIGHT_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Used to paint a gradient for sliders.
     */
    private static ColorUIResource SLIDER_GRADIENT_DARK_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Used to paint a gradient for sliders.
     */
    private static ColorUIResource SLIDER_GRADIENT_LIGHT_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Foreground color for selected components.
     */
    private static ColorUIResource SELECTION_FOREGROUND
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.DARK_BACKGROUND"));

    /**
     * Background color for selected components.
     */
    private static ColorUIResource SELECTION_BACKGROUND
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_BACKGROUND"));

    /**
     * Used to paint a focused split pane divider.
     */
    private static ColorUIResource SPLIT_PANE_DEVIDER_FOCUS_COLOR
        = new ColorUIResource(GuiActivator.getResources()
            .getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Tabbed pane border highlight color.
     */
    private static ColorUIResource TABBED_PANE_HIGHLIGHT_COLOR
        = new ColorUIResource(GuiActivator.getResources()
            .getColor("service.gui.BORDER_SHADOW"));

    /**
     * Table grid color.
     */
    private static ColorUIResource TABLE_GRID_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * These two are not used for now.
     */
    private static ColorUIResource SCROLL_BAR_TRACK_HIGHLIGHT
        = new ColorUIResource(GuiActivator.getResources()
            .getColor("service.gui.SCROLLBAR_TRACK_LIGHT"));

    /**
     * Used to paint scroll bar dark shadow.
     */
    private static ColorUIResource SCROLL_BAR_DARK_SHADOW
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.SCROLLBAR_DARK_SHADOW"));

    /**
     * Background color for all windows.
     */
    private static ColorUIResource DESKTOP_BACKGROUND_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * Text color for all texts.
     */
    private static ColorUIResource CONTROL_TEXT_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.DARK_TEXT"));

    /**
     * Used for inactive labels, text areas, etc.
     */
    private static ColorUIResource INACTIVE_CONTROL_TEXT_COLOR
        = new ColorUIResource(GuiActivator.getResources().
            getColor("service.gui.INACTIVE_TEXT"));

    /**
     * Foreground color for disabled menus.
     */
    private static ColorUIResource MENU_DISABLED_FOREGROUND
        = new ColorUIResource(GuiActivator.getResources()
            .getColor("service.gui.MENU_DISABLED_FOREGROUND"));

    /**
     * Color used in tab title for non read incoming messages.
     */
    private static ColorUIResource TAB_TITLE_HIGHLIGHT
        = new ColorUIResource(GuiActivator.getResources()
            .getColor("service.gui.TAB_TITLE_HIGHLIGHT"));

    /**
     * Color used in tab title for non read incoming messages.
     */
    private static ColorUIResource TAB_TITLE
        = new ColorUIResource(GuiActivator.getResources()
            .getColor("service.gui.TAB_TITLE"));

    /**
     * The basic font.
     */
    private static final FontUIResource BASIC_FONT
        = new FontUIResource(Constants.FONT);

    /**
     * Default input map for this theme.
     */
    Object fieldInputMap = new UIDefaults.LazyInputMap(
        new Object[]
           {
            "meta C", DefaultEditorKit.copyAction,
            "ctrl C", DefaultEditorKit.copyAction,
            "meta V", DefaultEditorKit.pasteAction,
            "ctrl V", DefaultEditorKit.pasteAction,
            "meta X", DefaultEditorKit.cutAction,
            "ctrl X", DefaultEditorKit.cutAction,
            "COPY", DefaultEditorKit.copyAction,
            "PASTE", DefaultEditorKit.pasteAction,
            "CUT", DefaultEditorKit.cutAction,
            "shift LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
            "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
            "ctrl LEFT", DefaultEditorKit.previousWordAction,
            "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
            "ctrl RIGHT", DefaultEditorKit.nextWordAction,
            "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
            "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
            "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
            "ctrl A", DefaultEditorKit.selectAllAction,
            "meta LEFT", DefaultEditorKit.previousWordAction,
            "meta KP_LEFT", DefaultEditorKit.previousWordAction,
            "meta RIGHT", DefaultEditorKit.nextWordAction,
            "meta KP_RIGHT", DefaultEditorKit.nextWordAction,
            "meta shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "meta shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "meta shift RIGHT", DefaultEditorKit.selectionNextWordAction,
            "meta shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
            "meta A", DefaultEditorKit.selectAllAction,
            "HOME", DefaultEditorKit.beginLineAction,
            "END", DefaultEditorKit.endLineAction,
            "shift HOME", DefaultEditorKit.selectionBeginLineAction,
            "shift END", DefaultEditorKit.selectionEndLineAction,
            "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
            "ctrl H", DefaultEditorKit.deletePrevCharAction,
            "meta H", DefaultEditorKit.deletePrevCharAction,
            "DELETE", DefaultEditorKit.deleteNextCharAction,
            "RIGHT", DefaultEditorKit.forwardAction,
            "LEFT", DefaultEditorKit.backwardAction,
            "KP_RIGHT", DefaultEditorKit.forwardAction,
            "KP_LEFT", DefaultEditorKit.backwardAction,
            "ENTER", JTextField.notifyAction,
            "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
            "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
           });

    /**
     * The default password input map.
     */
    Object passwordInputMap = new UIDefaults.LazyInputMap(
        new Object[]
           {
            "meta C", DefaultEditorKit.copyAction,
            "ctrl C", DefaultEditorKit.copyAction,
            "meta V", DefaultEditorKit.pasteAction,
            "ctrl V", DefaultEditorKit.pasteAction,
            "meta X", DefaultEditorKit.cutAction,
            "ctrl X", DefaultEditorKit.cutAction,
            "COPY", DefaultEditorKit.copyAction,
            "PASTE", DefaultEditorKit.pasteAction,
            "CUT", DefaultEditorKit.cutAction,
            "shift LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
            "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
            "ctrl LEFT", DefaultEditorKit.beginLineAction,
            "ctrl KP_LEFT", DefaultEditorKit.beginLineAction,
            "ctrl RIGHT", DefaultEditorKit.endLineAction,
            "ctrl KP_RIGHT", DefaultEditorKit.endLineAction,
            "ctrl shift LEFT", DefaultEditorKit.selectionBeginLineAction,
            "ctrl shift KP_LEFT", DefaultEditorKit.selectionBeginLineAction,
            "ctrl shift RIGHT", DefaultEditorKit.selectionEndLineAction,
            "ctrl shift KP_RIGHT", DefaultEditorKit.selectionEndLineAction,
            "ctrl A", DefaultEditorKit.selectAllAction,
            "meta LEFT", DefaultEditorKit.beginLineAction,
            "meta KP_LEFT", DefaultEditorKit.beginLineAction,
            "meta RIGHT", DefaultEditorKit.endLineAction,
            "meta KP_RIGHT", DefaultEditorKit.endLineAction,
            "meta shift LEFT", DefaultEditorKit.selectionBeginLineAction,
            "meta shift KP_LEFT", DefaultEditorKit.selectionBeginLineAction,
            "meta shift RIGHT", DefaultEditorKit.selectionEndLineAction,
            "meta shift KP_RIGHT", DefaultEditorKit.selectionEndLineAction,
            "meta A", DefaultEditorKit.selectAllAction,
            "HOME", DefaultEditorKit.beginLineAction,
            "END", DefaultEditorKit.endLineAction,
            "shift HOME", DefaultEditorKit.selectionBeginLineAction,
            "shift END", DefaultEditorKit.selectionEndLineAction,
            "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
            "ctrl H", DefaultEditorKit.deletePrevCharAction,
            "DELETE", DefaultEditorKit.deleteNextCharAction,
            "RIGHT", DefaultEditorKit.forwardAction,
            "LEFT", DefaultEditorKit.backwardAction,
            "KP_RIGHT", DefaultEditorKit.forwardAction,
            "KP_LEFT", DefaultEditorKit.backwardAction,
            "ENTER", JTextField.notifyAction,
            "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
            "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
    });

    /**
     * The default multi-line text component input map.
     */
    Object multilineInputMap = new UIDefaults.LazyInputMap(
        new Object[]
           {
            "meta C", DefaultEditorKit.copyAction,
            "ctrl C", DefaultEditorKit.copyAction,
            "meta V", DefaultEditorKit.pasteAction,
            "ctrl V", DefaultEditorKit.pasteAction,
            "meta X", DefaultEditorKit.cutAction,
            "ctrl X", DefaultEditorKit.cutAction,
            "COPY", DefaultEditorKit.copyAction,
            "PASTE", DefaultEditorKit.pasteAction,
            "CUT", DefaultEditorKit.cutAction,
            "shift LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
            "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
            "ctrl LEFT", DefaultEditorKit.previousWordAction,
            "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
            "ctrl RIGHT", DefaultEditorKit.nextWordAction,
            "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
            "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
            "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
            "ctrl A", DefaultEditorKit.selectAllAction,
            "meta LEFT", DefaultEditorKit.previousWordAction,
            "meta KP_LEFT", DefaultEditorKit.previousWordAction,
            "meta RIGHT", DefaultEditorKit.nextWordAction,
            "meta KP_RIGHT", DefaultEditorKit.nextWordAction,
            "meta shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "meta shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "meta shift RIGHT", DefaultEditorKit.selectionNextWordAction,
            "meta shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
            "meta A", DefaultEditorKit.selectAllAction,
            "HOME", DefaultEditorKit.beginLineAction,
            "END", DefaultEditorKit.endLineAction,
            "shift HOME", DefaultEditorKit.selectionBeginLineAction,
            "shift END", DefaultEditorKit.selectionEndLineAction,

            "UP", DefaultEditorKit.upAction,
            "KP_UP", DefaultEditorKit.upAction,
            "DOWN", DefaultEditorKit.downAction,
            "KP_DOWN", DefaultEditorKit.downAction,
            "PAGE_UP", DefaultEditorKit.pageUpAction,
            "PAGE_DOWN", DefaultEditorKit.pageDownAction,
            "shift PAGE_UP", "selection-page-up",
            "shift PAGE_DOWN", "selection-page-down",
            "ctrl shift PAGE_UP", "selection-page-left",
            "ctrl shift PAGE_DOWN", "selection-page-right",
            "shift UP", DefaultEditorKit.selectionUpAction,
            "shift KP_UP", DefaultEditorKit.selectionUpAction,
            "shift DOWN", DefaultEditorKit.selectionDownAction,
            "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
            "ENTER", DefaultEditorKit.insertBreakAction,
            "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
            "ctrl H", DefaultEditorKit.deletePrevCharAction,
            "DELETE", DefaultEditorKit.deleteNextCharAction,
            "RIGHT", DefaultEditorKit.forwardAction,
            "LEFT", DefaultEditorKit.backwardAction,
            "KP_RIGHT", DefaultEditorKit.forwardAction,
            "KP_LEFT", DefaultEditorKit.backwardAction,
            "TAB", DefaultEditorKit.insertTabAction,
            "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
            "ctrl HOME", DefaultEditorKit.beginAction,
            "ctrl END", DefaultEditorKit.endAction,
            "ctrl shift HOME", DefaultEditorKit.selectionBeginAction,
            "ctrl shift END", DefaultEditorKit.selectionEndAction,
            "ctrl T", "next-link-action",
            "ctrl shift T", "previous-link-action",
            "ctrl SPACE", "activate-link-action",
            "meta BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
            "meta HOME", DefaultEditorKit.beginAction,
            "meta END", DefaultEditorKit.endAction,
            "meta shift HOME", DefaultEditorKit.selectionBeginAction,
            "meta shift END", DefaultEditorKit.selectionEndAction,
            "meta T", "next-link-action",
            "meta shift T", "previous-link-action",
            "meta SPACE", "activate-link-action",
            "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
           });

    /**
     * Adds a list of entries in the given defaults <tt>table</tt>.
     *
     * @param table the table of user interface defaults
     */
    public void addCustomEntriesToTable(UIDefaults table)
    {
        List<Object> buttonGradient
            = Arrays.asList(new Object[]
               { new Float(.3f), new Float(0f), BUTTON_GRADIENT_DARK_COLOR,
                getWhite(), BUTTON_GRADIENT_LIGHT_COLOR });

        List<Object> sliderGradient
            = Arrays.asList(new Object[]
               { new Float(.3f), new Float(.2f), SLIDER_GRADIENT_DARK_COLOR,
                getWhite(), SLIDER_GRADIENT_LIGHT_COLOR });

        Object textFieldBorder = SIPCommBorders.getTextFieldBorder();

        ImageLoaderService imageLoaderService = GuiActivator.getImageLoaderService();

        Object[] defaults =
            new Object[]
            {
                "Button.rollover", Boolean.TRUE,

                "CheckBox.rollover", Boolean.TRUE,
                "CheckBox.gradient", buttonGradient,

                "CheckBoxMenuItem.gradient", buttonGradient,

                "Menu.opaque", Boolean.FALSE,

                "MenuBar.border", null,

                "Menu.borderPainted", Boolean.FALSE,
                "Menu.border", textFieldBorder,
                "Menu.selectionBackground", SELECTION_BACKGROUND,
                "Menu.selectionForeground", SELECTION_FOREGROUND,
                "Menu.margin", new InsetsUIResource(0, 0, 0, 0),

                "MenuItem.borderPainted", Boolean.FALSE,
                "MenuItem.border", textFieldBorder,
                "MenuItem.selectionBackground", SELECTION_BACKGROUND,
                "MenuItem.selectionForeground", SELECTION_FOREGROUND,

                "CheckBoxMenuItem.borderPainted", Boolean.FALSE,
                "CheckBoxMenuItem.border", textFieldBorder,
                "CheckBoxMenuItem.selectionBackground", SELECTION_BACKGROUND,
                "CheckBoxMenuItem.selectionForeground", SELECTION_FOREGROUND,

                "InternalFrame.activeTitleGradient", buttonGradient,

                "OptionPane.warningIcon",
                imageLoaderService.getImage(ImageLoaderService.WARNING_ICON).resolve(),

                "OptionPane.errorIcon",
                imageLoaderService.getImage(ImageLoaderService.ERROR_ICON).resolve(),

                "OptionPane.infoIcon",
                imageLoaderService.getImage(ImageLoaderService.INFO_ICON).resolve(),

                "RadioButton.gradient", buttonGradient,
                "RadioButton.rollover", Boolean.TRUE,

                "RadioButtonMenuItem.gradient", buttonGradient,

                "Spinner.arrowButtonBorder", SIPCommBorders.getTextFieldBorder(),

                "Slider.altTrackColor", SLIDER_GRADIENT_LIGHT_COLOR,
                "Slider.gradient", sliderGradient,
                "Slider.focusGradient", sliderGradient,

                "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
                "SplitPane.dividerFocusColor", SPLIT_PANE_DEVIDER_FOCUS_COLOR,
                "SplitPane.dividerSize", Integer.valueOf(5),

                "ScrollBar.width", Integer.valueOf(12),
                "ScrollBar.horizontalThumbIcon",
                imageLoaderService.getImage(ImageLoaderService.SCROLLBAR_THUMB_HORIZONTAL),
                "ScrollBar.verticalThumbIcon",
                imageLoaderService.getImage(ImageLoaderService.SCROLLBAR_THUMB_VERTICAL),
                "ScrollBar.horizontalThumbHandleIcon",
                imageLoaderService
                    .getImage(ImageLoaderService.SCROLLBAR_THUMB_HANDLE_HORIZONTAL),
                "ScrollBar.verticalThumbHandleIcon",
                imageLoaderService
                    .getImage(ImageLoaderService.SCROLLBAR_THUMB_HANDLE_VERTICAL),
                "ScrollBar.trackHighlight", SCROLL_BAR_TRACK_HIGHLIGHT,
                "ScrollBar.highlight", SELECTION_BACKGROUND,
                "ScrollBar.darkShadow", SCROLL_BAR_DARK_SHADOW,

                "TabbedPane.borderHightlightColor", TABBED_PANE_HIGHLIGHT_COLOR,
                "TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3),
                "TabbedPane.selected", SELECTION_BACKGROUND,
                "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
                "TabbedPane.unselectedBackground", SELECTION_BACKGROUND,
                "TabbedPane.shadow", CONTROL_SHADOW,
                "TabbedPane.darkShadow", CONTROL_DARK_SHADOW,
                "TabbedPane.tabTitleHighlight", TAB_TITLE_HIGHLIGHT,
                "TabbedPane.foreground", TAB_TITLE,

                "TextField.border", textFieldBorder,
                "TextField.margin", new InsetsUIResource(3, 3, 3, 3),

                "TextField.focusInputMap", fieldInputMap,
                "TextArea.focusInputMap", multilineInputMap,
                "TextPane.focusInputMap", multilineInputMap,
                "EditorPane.focusInputMap", multilineInputMap,

                "PasswordField.border", textFieldBorder,
                "PasswordField.margin", new InsetsUIResource(3, 3, 3, 3),
                "PasswordField.focusInputMap", passwordInputMap,

                "FormattedTextField.border", textFieldBorder,
                "FormattedTextField.margin", new InsetsUIResource(3, 3, 3, 3),

                "Table.gridColor", TABLE_GRID_COLOR,
                "Table.background", getDesktopColor(),

                "ToggleButton.gradient", buttonGradient,

                "ToolBar.isRollover", Boolean.TRUE,
                "ToolBar.separatorColor", PRIMARY_CONTROL_COLOR,
                "ToolBar.separatorSize", new DimensionUIResource(8, 22),

                "ToolTip.background", SELECTION_BACKGROUND,
                "ToolTip.backgroundInactive", SELECTION_BACKGROUND,
                "ToolTip.hideAccelerator", Boolean.FALSE,

                "TitledBorder.border", SIPCommBorders.getBoldRoundBorder()
            };

        table.putDefaults(defaults);
    }

    /**
     * Returns the name of this theme.
     *
     * @return the name of this theme
     */
    public String getName()
    {
        return "SipCommunicator";
    }

    /**
     * Returns the primary control dark shadow color resource.
     *
     * @return the primary control dark shadow color resource
     */
    protected ColorUIResource getPrimary1()
    {
        return PRIMARY_CONTROL_DARK_SHADOW;
    }

    /**
     * Returns the primary control shadow color resource.
     *
     * @return the primary control shadow color resource
     */
    protected ColorUIResource getPrimary2()
    {
        return PRIMARY_CONTROL_SHADOW;
    }

    /**
     * Returns the primary control color resource.
     *
     * @return the primary control color resource
     */
    protected ColorUIResource getPrimary3()
    {
        return PRIMARY_CONTROL_COLOR;
    }

    /**
     * Returns the control dark shadow color resource.
     *
     * @return the control dark shadow color resource
     */
    protected ColorUIResource getSecondary1()
    {
        return CONTROL_DARK_SHADOW;
    }

    /**
     * Returns the control shadow color resource.
     *
     * @return the control shadow color resource
     */
    protected ColorUIResource getSecondary2()
    {
        return CONTROL_SHADOW;
    }

    /**
     * Returns the control color resource.
     *
     * @return the control color resource
     */
    protected ColorUIResource getSecondary3()
    {
        return CONTROL_COLOR;
    }

    /**
     * Returns the control text color resource.
     *
     * @return the control text color resource
     */
    protected ColorUIResource getBlack()
    {
        return CONTROL_TEXT_COLOR;
    }

    /**
     * Returns the desktop color resource.
     *
     * @return the desktop color resource
     */
    public ColorUIResource getDesktopColor()
    {
        return DESKTOP_BACKGROUND_COLOR;
    }

    /**
     * Returns the window background color resource.
     *
     * @return the window background color resource
     */
    public ColorUIResource getWindowBackground()
    {
        return getWhite();
    }

    /**
     * Returns the desktop background color resource.
     *
     * @return the desktop background color resource
     */
    public ColorUIResource getControl()
    {
        return DESKTOP_BACKGROUND_COLOR;
    }

    /**
     * Returns the window background color resource.
     *
     * @return the window background color resource
     */
    public ColorUIResource getMenuBackground()
    {
        return DESKTOP_BACKGROUND_COLOR;
    }

    /**
     * Returns the inactive control text color resource.
     *
     * @return the inactive control text color resource
     */
    public ColorUIResource getInactiveControlTextColor()
    {
        return INACTIVE_CONTROL_TEXT_COLOR;
    }

    /**
     * Returns the menu disabled foreground color resource.
     *
     * @return the menu disabled foreground color resource
     */
    public ColorUIResource getMenuDisabledForeground()
    {
        return MENU_DISABLED_FOREGROUND;
    }

    /**
     * Returns the control text font color resource.
     *
     * @return the control text font color resource
     */
    public FontUIResource getControlTextFont()
    {
        return BASIC_FONT;
    }

    /**
     * Returns the system text font color resource.
     *
     * @return the system text font color resource
     */
    public FontUIResource getSystemTextFont()
    {
        return BASIC_FONT;
    }

    /**
     * Returns the user text font color resource.
     *
     * @return the user text font color resource
     */
    public FontUIResource getUserTextFont()
    {
        return BASIC_FONT;
    }

    /**
     * Returns the menu text font color resource.
     *
     * @return the menu text font color resource
     */
    public FontUIResource getMenuTextFont()
    {
        return BASIC_FONT;
    }

    /**
     * Returns the window title font color resource.
     *
     * @return the window title font color resource
     */
    public FontUIResource getWindowTitleFont()
    {
        return BASIC_FONT;
    }

    /**
     * Returns the window title font color resource.
     *
     * @return the window title font color resource
     */
    public FontUIResource getSubTextFont()
    {
        return BASIC_FONT;
    }

    /**
     * Reloads defaults.
     */
    public void loadSkin()
    {
        PRIMARY_CONTROL_DARK_SHADOW =
            new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.BORDER_SHADOW"));

        PRIMARY_CONTROL_SHADOW =
            new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.BORDER_SHADOW"));

        CONTROL_DARK_SHADOW =
            new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.BORDER_SHADOW"));

        CONTROL_SHADOW =
            new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.BORDER_SHADOW"));

        CONTROL_COLOR =
            new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        PRIMARY_CONTROL_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        BUTTON_GRADIENT_DARK_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        BUTTON_GRADIENT_LIGHT_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        SLIDER_GRADIENT_DARK_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        SLIDER_GRADIENT_LIGHT_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        SELECTION_FOREGROUND
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.DARK_BACKGROUND"));

        SELECTION_BACKGROUND
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_BACKGROUND"));

        SPLIT_PANE_DEVIDER_FOCUS_COLOR
            = new ColorUIResource(GuiActivator.getResources()
                .getColor("service.gui.LIGHT_HIGHLIGHT"));

        TABBED_PANE_HIGHLIGHT_COLOR
            = new ColorUIResource(GuiActivator.getResources()
                .getColor("service.gui.BORDER_SHADOW"));

        TABLE_GRID_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        SCROLL_BAR_TRACK_HIGHLIGHT
            = new ColorUIResource(GuiActivator.getResources()
                .getColor("service.gui.SCROLLBAR_TRACK_LIGHT"));

        SCROLL_BAR_DARK_SHADOW
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.SCROLLBAR_DARK_SHADOW"));

        DESKTOP_BACKGROUND_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.LIGHT_BACKGROUND"));

        CONTROL_TEXT_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.DARK_TEXT"));

        INACTIVE_CONTROL_TEXT_COLOR
            = new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.INACTIVE_TEXT"));

        MENU_DISABLED_FOREGROUND
            = new ColorUIResource(GuiActivator.getResources()
                .getColor("service.gui.MENU_DISABLED_FOREGROUND"));

        TAB_TITLE_HIGHLIGHT
            = new ColorUIResource(GuiActivator.getResources()
                .getColor("service.gui.TAB_TITLE_HIGHLIGHT"));

        TAB_TITLE
            = new ColorUIResource(GuiActivator.getResources()
                .getColor("service.gui.TAB_TITLE"));
    }
}
