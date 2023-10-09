/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.resources.*;

/**
 * Stores and loads images used throughout this UI implementation.
 */
public interface ImageLoaderService
{
    /**
     * The SIP Communicator logo 16x16 icon.
     */
    ImageID SIP_COMMUNICATOR_LOGO
        = new ImageID("service.gui.SIP_COMMUNICATOR_LOGO_64x64");

    /**
     * The jabber generic IM 16x16 logo.
     */
    ImageID JABBER_IM_LOGO_16x16
        = new ImageID("service.protocol.jabber.JABBER_16x16");

    /**
     * The jabber generic IM 16x16 logo.
     */
    ImageID JABBER_IM_LOGO_32x32
        = new ImageID("service.protocol.jabber.JABBER_32x32");

    /**
     * The jabber generic IM 16x16 logo.
     */
    ImageID JABBER_IM_LOGO_48x48
        = new ImageID("service.protocol.jabber.JABBER_48x48");

    /*------------------------------------------------------------------------
     * =========================LOOK AND FEEL IMAGES==========================
     * -----------------------------------------------------------------------
     */

    /**
     * The background used in the splash screen and about pages
     */
    ImageID SPLASH_SCREEN_BACKGROUND
         = new ImageID("plugin.branding.SPLASH_SCREEN_BACKGROUND");

    /**
     * The image used for the "thumb" of a vertical scrollbar.
     */
    ImageID SCROLLBAR_THUMB_VERTICAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_VERTICAL");

    /**
     * The image used for the "thumb" of a horizontal scrollbar.
     */
    ImageID SCROLLBAR_THUMB_HORIZONTAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_HORIZONTAL");

    /**
     * The image used for the "thumb handle" of a horizontal scrollbar.
     */
    ImageID SCROLLBAR_THUMB_HANDLE_HORIZONTAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_THUMB_HORIZONTAL");

    /**
     * The image used for the "thumb handle" of a vertical scrollbar.
     */
    ImageID SCROLLBAR_THUMB_HANDLE_VERTICAL
        = new ImageID("service.gui.lookandfeel.SCROLLBAR_THUMB_VERTICAL");

    /*
     * =======================================================================
     * ------------------------ OPTION PANE ICONS ----------------------------
     * =======================================================================
     */
    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane warning message.
     */
    ImageID WARNING_ICON
        = new ImageID("service.gui.icons.WARNING_ICON");

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane error message.
     */
    ImageID ERROR_ICON
        = new ImageID("service.gui.icons.ERROR_ICON");

    /**
     * The icon used in the <tt>SIPCommLookAndFeel</tt> to paint the icon
     * of an option pane info message.
     */
    ImageID INFO_ICON
        = new ImageID("service.gui.icons.INFO_ICON");

    /*------------------------------------------------------------------------
     * ============================APPLICATION ICONS =========================
     * -----------------------------------------------------------------------
     */

    /**
     * The background of the main window and chat window.
     */
    ImageID MAIN_WINDOW_BACKGROUND
        = new ImageID("service.gui.LIGHT_BACKGROUND");

    /**
     * A down arrow icon
     */
    ImageID DOWN_ARROW_ICON
        = new ImageID("service.gui.icons.DOWN_ARROW_ICON");

    /**
     * The merge call button image. The icon shown in the Call Frame panel.
     */
    ImageID MERGE_CALL_BUTTON
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON");

    /**
     * The merge call button image.
     */
    ImageID MERGE_CALL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON_ROLLOVER");

    /**
     * The merge call button image.
     */
    ImageID MERGE_CALL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON_PRESSED");

    /**
     * The merge call button image. The icon shown in the Call Frame panel.
     */
    ImageID MERGE_CALL_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.MERGE_CALL_BUTTON_FOCUS");

    /**
     * The add participant button image. The icon shown in the Call Frame panel.
     */
    ImageID ADD_PARTICIPANT
        = new ImageID("service.gui.buttons.ADD_PARTICIPANT");

    /**
     * The add participant button image. The icon shown in the Call Frame panel.
     */
    ImageID ADD_PARTICIPANT_ROLLOVER
        = new ImageID("service.gui.buttons.ADD_PARTICIPANT_ROLLOVER");

    /**
     * The add participant button image. The icon shown in the Call Frame panel.
     */
    ImageID ADD_PARTICIPANT_PRESSED
        = new ImageID("service.gui.buttons.ADD_PARTICIPANT_PRESSED");

    /**
     * The add participant button image. The icon shown in the Call Frame panel.
     */
    ImageID ADD_PARTICIPANT_MULTI
        = new ImageID("service.gui.buttons.ADD_PARTICIPANT_MULTI");

    /**
     * The add participant button image. The icon shown in the Call Frame panel.
     */
    ImageID ADD_PARTICIPANT_MULTI_ROLLOVER
        = new ImageID("service.gui.buttons.ADD_PARTICIPANT_MULTI_ROLLOVER");

    /**
     * The add participant button image. The icon shown in the Call Frame panel.
     */
    ImageID ADD_PARTICIPANT_MULTI_PRESSED
        = new ImageID("service.gui.buttons.ADD_PARTICIPANT_MULTI_PRESSED");

    /**
     * The photo frame to use in the Call Frame.
     */
    ImageID USER_PHOTO_FRAME
        = new ImageID("service.gui.USER_PHOTO_FRAME");

    /**
     * The background image for a button in contact list that is shown on the
     * left of the button toolbar.
     */
    ImageID CONTACT_LIST_BUTTON_BG_LEFT
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_BG_LEFT");

    /**
     * The background image for a button in contact list that is shown on the
     * right of the button toolbar.
     */
    ImageID CONTACT_LIST_BUTTON_BG_RIGHT
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_BG_RIGHT");

    /**
     * The background image for a button in contact list that is shown in the
     * middle of other buttons.
     */
    ImageID CONTACT_LIST_BUTTON_BG_MIDDLE
        = new ImageID("service.gui.buttons.CONTACT_LIST_BUTTON_BG_MIDDLE");

    /**
     * The background image for a button in contact list if there's only one
     * button shown.
     */
    ImageID CONTACT_LIST_ONE_BUTTON_BG
        = new ImageID("service.gui.buttons.CONTACT_LIST_ONE_BUTTON_BG");

    /**
     * The call button small image.
     */
    ImageID CALL_BUTTON_SMALL
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL");

    /**
     * The call button small pressed image.
     */
    ImageID CALL_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_ROLLOVER");

    /**
     * The call button small disabled image
     */
    ImageID CALL_BUTTON_SMALL_DISABLED
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_DISABLED");

    /**
     * The call button small pressed image.
     */
    ImageID CALL_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_PRESSED");

    /**
     * The call button small image for when there are multiple numbers
     */
    ImageID CALL_BUTTON_SMALL_MULTI
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_MULTI");

    /**
     * The call button small rollover image for when there are multiple numbers
     */
    ImageID CALL_BUTTON_SMALL_MULTI_ROLLOVER
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_MULTI_ROLLOVER");

    /**
     * The call button small pressed image for when there are multiple numbers
     */
    ImageID CALL_BUTTON_SMALL_MULTI_PRESSED
        = new ImageID("service.gui.buttons.CALL_BUTTON_SMALL_MULTI_PRESSED");

    /**
     * The call button small image.
     */
    ImageID CALL_VIDEO_BUTTON_SMALL
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_SMALL");

    /**
     * The call button small pressed image.
     */
    ImageID CALL_VIDEO_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_SMALL_ROLLOVER");

    /**
     * The call button small pressed image.
     */
    ImageID CALL_VIDEO_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.CALL_VIDEO_BUTTON_SMALL_PRESSED");

    /**
     * The add contact button small image, shown when an external source contact
     * is selected.
     */
    ImageID ADD_CONTACT_BUTTON_SMALL
        = new ImageID("service.gui.buttons.ADD_CONTACT_BUTTON_SMALL");

    /**
     * The add contact button small pressed image, shown when an external source
     * contact is selected and add contact button is pressed.
     */
    ImageID ADD_CONTACT_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.ADD_CONTACT_BUTTON_SMALL_ROLLOVER");

    /**
     * The add contact button small pressed image, shown when an external source
     * contact is selected and add contact button is pressed.
     */
    ImageID ADD_CONTACT_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.ADD_CONTACT_BUTTON_SMALL_PRESSED");

    /**
     * The conference button image.
     */
    ImageID CONFERENCE_BUTTON
        = new ImageID("service.gui.buttons.CONFERENCE_BUTTON");

    /**
     * The conference button pressed image.
     */
    ImageID CONFERENCE_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.CONFERENCE_BUTTON_PRESSED");

    /**
     * The conference button rollover image.
     */
    ImageID CONFERENCE_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.CONFERENCE_BUTTON_ROLLOVER");

    /**
     * The conference button disabled image.
     */
    ImageID CONFERENCE_BUTTON_DISABLED
        = new ImageID("service.gui.buttons.CONFERENCE_BUTTON_DISABLED");

    /**
     * The chat button small image.
     */
    ImageID CHAT_BUTTON_SMALL
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL");

    /**
     * The chat button small disabled image
     */
    ImageID CHAT_BUTTON_SMALL_DISABLED
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL_DISABLED");

    /**
     * The icon used to separate buttons in the call toolbar.
     */
    ImageID CALL_TOOLBAR_SEPARATOR
        = new ImageID("service.gui.icons.CALL_TOOLBAR_SEPARATOR");

    /**
     * The icon used to separate buttons in the call toolbar.
     */
    ImageID CALL_TOOLBAR_DARK_SEPARATOR
        = new ImageID("service.gui.icons.CALL_TOOLBAR_DARK_SEPARATOR");

    /**
     * The notify when available button image in the chat toolbar.
     */
    ImageID NOTIFY_WHEN_AVAILABLE
            = new ImageID("service.gui.icons.NOTIFY_WHEN_AVAILABLE");

    ImageID NOTIFY_WHEN_AVAILABLE_DISABLED
            = new ImageID("service.gui.icons.NOTIFY_WHEN_AVAILABLE_DISABLED");

    ImageID NOTIFY_WHEN_AVAILABLE_ON
            = new ImageID("service.gui.icons.NOTIFY_WHEN_AVAILABLE_ON");

    ImageID NOTIFY_WHEN_AVAILABLE_ON_ROLLOVER
            = new ImageID("service.gui.icons.NOTIFY_WHEN_AVAILABLE_ON_ROLLOVER");
    ImageID NOTIFY_WHEN_AVAILABLE_PRESSED
            = new ImageID("service.gui.icons.NOTIFY_WHEN_AVAILABLE_PRESSED");

    ImageID NOTIFY_WHEN_AVAILABLE_ROLLOVER
            = new ImageID("service.gui.icons.NOTIFY_WHEN_AVAILABLE_ROLLOVER");

    /**
     * The chat call button image.
     */
    ImageID CHAT_CALL
        = new ImageID("service.gui.buttons.CHAT_CALL");

    /**
     * The chat button small pressed image.
     */
    ImageID CHAT_BUTTON_SMALL_PRESSED
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL_PRESSED");

    /**
     * The chat button small pressed image.
     */
    ImageID CHAT_BUTTON_SMALL_ROLLOVER
        = new ImageID("service.gui.buttons.CHAT_BUTTON_SMALL_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID ONE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID TWO_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID THREE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID FOUR_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID FIVE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID SIX_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID SEVEN_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID EIGHT_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID NINE_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID STAR_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID ZERO_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID DIEZ_DIAL_BUTTON_MAC
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_MAC");

    /**
     * A dial button icon.
     */
    ImageID ONE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID TWO_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID THREE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID FOUR_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID FIVE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID SIX_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID SEVEN_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID EIGHT_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID NINE_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID STAR_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID ZERO_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID DIEZ_DIAL_BUTTON_MAC_ROLLOVER
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_MAC_ROLLOVER");

    /**
     * A dial button icon.
     */
    ImageID ONE_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID TWO_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID THREE_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID FOUR_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID FIVE_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID SIX_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID SEVEN_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID EIGHT_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID NINE_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID STAR_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID ZERO_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID DIEZ_DIAL_BUTTON_MAC_PRESSED
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON_MAC_PRESSED");

    /**
     * A dial button icon.
     */
    ImageID ONE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.ONE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID TWO_DIAL_BUTTON
        = new ImageID("service.gui.buttons.TWO_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID THREE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.THREE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID FOUR_DIAL_BUTTON
        = new ImageID("service.gui.buttons.FOUR_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID FIVE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.FIVE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID SIX_DIAL_BUTTON
        = new ImageID("service.gui.buttons.SIX_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID SEVEN_DIAL_BUTTON
        = new ImageID("service.gui.buttons.SEVEN_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID EIGHT_DIAL_BUTTON
        = new ImageID("service.gui.buttons.EIGHT_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID NINE_DIAL_BUTTON
        = new ImageID("service.gui.buttons.NINE_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID STAR_DIAL_BUTTON
        = new ImageID("service.gui.buttons.STAR_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID ZERO_DIAL_BUTTON
        = new ImageID("service.gui.buttons.ZERO_DIAL_BUTTON");

    /**
     * A dial button icon.
     */
    ImageID DIEZ_DIAL_BUTTON
        = new ImageID("service.gui.buttons.DIEZ_DIAL_BUTTON");

    /**
     * A dial button icon. The icon shown in the Call Frame panel.
     */
    ImageID DIAL_BUTTON
        = new ImageID("service.gui.buttons.DIAL_BUTTON");

    /**
     * A dial button icon. The icon shown in the Call Frame panel.
     */
    ImageID DIAL_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.DIAL_BUTTON_ROLLOVER");

    /**
     * A dial button icon. The icon shown in the Call Frame panel.
     */
    ImageID DIAL_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.DIAL_BUTTON_FOCUS");

    /**
     * A dial button icon. The icon shown in the Call Frame panel.
     */
    ImageID DIAL_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.DIAL_BUTTON_PRESSED");

    /**
     * A dial button icon. The icon shown in the Call Frame panel.
     */
    ImageID DIAL_BUTTON_DISABLED
        = new ImageID("service.gui.buttons.DIAL_BUTTON_DISABLED");

    /**
     * An end call button icon. The icon shown in the Call Frame panel.
     */
    ImageID END_CALL
        = new ImageID("service.gui.buttons.END_CALL");

    /**
     * An end call button icon. The icon shown in the Call Frame panel.
     */
    ImageID END_CALL_ROLLOVER
        = new ImageID("service.gui.buttons.END_CALL_ROLLOVER");

    /**
     * An end call button icon. The icon shown in the Call Frame panel.
     */
    ImageID END_CALL_FOCUS
        = new ImageID("service.gui.buttons.END_CALL_FOCUS");

    /**
     * An end call button icon. The icon shown in the Call Frame panel.
     */
    ImageID END_CALL_PRESSED
        = new ImageID("service.gui.buttons.END_CALL_PRESSED");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON
        = new ImageID("service.gui.buttons.HOLD_BUTTON");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.HOLD_BUTTON_ROLLOVER");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.HOLD_BUTTON_FOCUS");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.HOLD_BUTTON_PRESSED");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON_ON
        = new ImageID("service.gui.buttons.HOLD_BUTTON_ON");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON_ON_ROLLOVER
        = new ImageID("service.gui.buttons.HOLD_BUTTON_ON_ROLLOVER");

    /**
     * A put-on/off-hold button icon. The icon shown in the Call Frame
     * panel.
     */
    ImageID HOLD_BUTTON_ON_FOCUS
        = new ImageID("service.gui.buttons.HOLD_BUTTON_ON_FOCUS");

    /**
     * A mute button icon. The icon shown in the Call Frame panel.
     */
    ImageID MUTE_BUTTON
        = new ImageID("service.gui.buttons.MUTE_BUTTON");

    /**
     * A call park button icon. The icon shown in the Call Frame panel.
     */
    ImageID PARK_BUTTON
        = new ImageID("service.gui.buttons.PARK_BUTTON");

    /**
     * A call park button icon. The icon shown in the Call Frame panel.
     */
    ImageID PARK_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.PARK_BUTTON_ROLLOVER");

    /**
     * A call park button icon. The icon shown in the Call Frame panel.
     */
    ImageID PARK_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.PARK_BUTTON_FOCUS");

    /**
     * A call park button icon. The icon shown in the Call Frame panel.
     */
    ImageID PARK_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.PARK_BUTTON_PRESSED");

    /**
     * A record button icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON
        = new ImageID("service.gui.buttons.RECORD_BUTTON");

    /**
     * A record button pressed icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.RECORD_BUTTON_PRESSED");

    /**
     * A record button rollover icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.RECORD_BUTTON_ROLLOVER");

    /**
     * A record button focus icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.RECORD_BUTTON_FOCUS");

    /**
     * A record button on icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON_ON
        = new ImageID("service.gui.buttons.RECORD_BUTTON_ON");

    /**
     * A record button on rollover icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON_ON_ROLLOVER
        = new ImageID("service.gui.buttons.RECORD_BUTTON_ON_ROLLOVER");

    /**
     * A record button on focus icon. The icon shown in the Call Frame panel.
     */
    ImageID RECORD_BUTTON_ON_FOCUS
        = new ImageID("service.gui.buttons.RECORD_BUTTON_ON_FOCUS");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_ROLLOVER");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_FOCUS");

    /**
     * A local video button pressed icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_PRESSED");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_ON
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_ON");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_ON_ROLLOVER
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_ON_ROLLOVER");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_ON_FOCUS
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_ON_FOCUS");

    /**
     * A local video button icon. The icon shown in the Call Frame panel.
     */
    ImageID LOCAL_VIDEO_BUTTON_DISABLED
        = new ImageID("service.gui.buttons.LOCAL_VIDEO_BUTTON_DISABLED");

    /**
     * The chat button that appears in the call window.
     */
    ImageID CHAT_AVATAR_BUTTON
        = new ImageID("service.gui.buttons.CHAT_AVATAR");

    /**
     * The chat button that appears in the call window.
     */
    ImageID CHAT_AVATAR_BUTTON_ROLLOVER
        = new ImageID("service.gui.buttons.CHAT_AVATAR_ROLLOVER");

    /**
     * The chat button that appears in the call window.
     */
    ImageID CHAT_AVATAR_BUTTON_FOCUS
        = new ImageID("service.gui.buttons.CHAT_AVATAR_FOCUS");

    /**
     * The chat button that appears in the call window.
     */
    ImageID CHAT_AVATAR_BUTTON_PRESSED
        = new ImageID("service.gui.buttons.CHAT_AVATAR_PRESSED");

    /**
     * The chat button that appears in the call window.
     */
    ImageID CHAT_AVATAR_BUTTON_DISABLED
        = new ImageID("service.gui.buttons.CHAT_AVATAR_DISABLED");

    /**
     * A call-transfer button icon. The icon shown in the Call Frame panel.
     */
    ImageID TRANSFER_CALL_BUTTON =
        new ImageID("service.gui.buttons.TRANSFER_CALL_BUTTON");

    /**
     * A call-transfer button icon. The icon shown in the Call Frame panel.
     */
    ImageID TRANSFER_CALL_BUTTON_ROLLOVER =
        new ImageID("service.gui.buttons.TRANSFER_CALL_BUTTON_ROLLOVER");

    /**
     * A call-transfer button icon. The icon shown in the Call Frame panel.
     */
    ImageID TRANSFER_CALL_BUTTON_FOCUS =
        new ImageID("service.gui.buttons.TRANSFER_CALL_BUTTON_FOCUS");

    /**
     * A call-transfer button icon. The icon shown in the Call Frame panel.
     */
    ImageID TRANSFER_CALL_BUTTON_PRESSED =
        new ImageID("service.gui.buttons.TRANSFER_CALL_BUTTON_PRESSED");

    /**
     * A call-transfer button icon. The icon shown in the Call Frame panel.
     */
    ImageID TRANSFER_CALL_BUTTON_DISABLED =
            new ImageID("service.gui.buttons.TRANSFER_CALL_BUTTON_DISABLED");

    /**
     * A call push button icon. The icon shown in the Call Frame panel.
     */
    ImageID CALL_PUSH_BUTTON =
            new ImageID("service.gui.buttons.CALL_PUSH_BUTTON");

    /**
     * A call push button icon. The icon shown in the Call Frame panel.
     */
    ImageID CALL_PUSH_BUTTON_ROLLOVER =
            new ImageID("service.gui.buttons.CALL_PUSH_BUTTON_ROLLOVER");

    /**
     * A call push button icon. The icon shown in the Call Frame panel.
     */
    ImageID CALL_PUSH_BUTTON_FOCUS =
            new ImageID("service.gui.buttons.CALL_PUSH_BUTTON_FOCUS");

    /**
     * A call push button icon. The icon shown in the Call Frame panel.
     */
    ImageID CALL_PUSH_BUTTON_PRESSED =
            new ImageID("service.gui.buttons.CALL_PUSH_BUTTON_PRESSED");

    /**
     * A call push button icon. The icon shown in the Call Frame panel.
     */
    ImageID CALL_PUSH_BUTTON_DISABLED =
            new ImageID("service.gui.buttons.CALL_PUSH_BUTTON_DISABLED");

    /**
     * A web conference button image shown in the Call Frame panel.
     */
    ImageID WEB_CONFERENCE_BUTTON =
        new ImageID("service.gui.buttons.WEB_CONFERENCE_BUTTON");

    /**
     * A web conference button rollover image shown in the Call Frame panel.
     */
    ImageID WEB_CONFERENCE_BUTTON_ROLLOVER =
        new ImageID("service.gui.buttons.WEB_CONFERENCE_BUTTON_ROLLOVER");

    /**
     * A web conference button disabled image shown in the Call Frame panel.
     */
    ImageID WEB_CONFERENCE_BUTTON_DISABLED =
        new ImageID("service.gui.buttons.WEB_CONFERENCE_BUTTON_DISABLED");

    /**
     * A web conference button pressed image shown in the Call Frame panel.
     */
    ImageID WEB_CONFERENCE_BUTTON_PRESSED =
        new ImageID("service.gui.buttons.WEB_CONFERENCE_BUTTON_PRESSED");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON =
        new ImageID("service.gui.buttons.HOLD_AVATAR");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_ROLLOVER =
        new ImageID("service.gui.buttons.HOLD_AVATAR_ROLLOVER");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_FOCUS =
        new ImageID("service.gui.buttons.HOLD_AVATAR_FOCUS");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_PRESSED =
        new ImageID("service.gui.buttons.HOLD_AVATAR_PRESSED");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_ON =
        new ImageID("service.gui.buttons.HOLD_AVATAR_ON");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_ON_ROLLOVER =
        new ImageID("service.gui.buttons.HOLD_AVATAR_ON_ROLLOVER");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_ON_FOCUS =
        new ImageID("service.gui.buttons.HOLD_AVATAR_ON_FOCUS");

    /**
     * A hold peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID HOLD_AVATAR_BUTTON_DISABLED =
        new ImageID("service.gui.buttons.HOLD_AVATAR_DISABLED");

    /**
     * A remove peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID CLOSE_TAB_BUTTON =
        new ImageID("service.gui.buttons.CLOSE_TAB");

    /**
     * A remove peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID CLOSE_TAB_BUTTON_ROLLOVER =
        new ImageID("service.gui.buttons.CLOSE_TAB_ROLLOVER");

    /**
     * A remove peer button icon. The icon shown in the Call Frame panel.
     */
    ImageID CLOSE_TAB_BUTTON_PRESSED =
        new ImageID("service.gui.buttons.CLOSE_TAB_PRESSED");

    /**
     * The secure button on icon. The icon shown in the Call Frame panel.
     */
    ImageID SECURE_BUTTON_ON =
        new ImageID("service.gui.buttons.SECURE_BUTTON_ON");

    /**
     * The secure button off icon. The icon shown in the Call Frame panel.
     */
    ImageID SECURE_BUTTON_OFF =
        new ImageID("service.gui.buttons.SECURE_BUTTON_OFF");

    /**
     * The image used in place of an avatar in the start conference dialog.
     */
    ImageID CONFERENCE_AVATAR
        = new ImageID("service.gui.conf.AVATAR");

    /**
     * The image used as the default group contact avatar in the view/edit
     * contact window.
     */
    ImageID DEFAULT_GROUP_CONTACT_PHOTO
        = new ImageID("service.gui.DEFAULT_GROUP_CONTACT_PHOTO");

    /**
     * The image used as the default group contact small avatar for the cells
     * in the contact list.
     */
    ImageID DEFAULT_GROUP_CONTACT_PHOTO_SMALL
        = new ImageID("service.gui.DEFAULT_GROUP_CONTACT_PHOTO_SMALL");

    /**
     * The image used, when a contact has no photo specified.
     */
    ImageID DEFAULT_USER_PHOTO
        = new ImageID("service.gui.DEFAULT_USER_PHOTO");

    /**
     * The image used, when a contact is unauthorized.
     */
    ImageID UNAUTHORIZED_CONTACT_PHOTO
        = new ImageID("service.gui.icons.UNAUTHORIZED_CONTACT_PHOTO");

    /**
     * Re-request authorization menu item icon.
     */
    ImageID UNAUTHORIZED_CONTACT_16x16
        = new ImageID("service.gui.icons.UNAUTHORIZED_CONTACT_16x16");

    /**
     * The icon image of the "Add contact to chat" button in the
     * chat window.
     */
    ImageID ADD_TO_CHAT_ICON
        = new ImageID("service.gui.icons.ADD_TO_CHAT_ICON");

    /**
     * The rollover icon image of the "Add contact to chat" button in the
     * chat window.
     */
    ImageID ADD_TO_CHAT_ICON_ROLLOVER
        = new ImageID("service.gui.icons.ADD_TO_CHAT_ICON_ROLLOVER");

    /**
     * The pressed icon image of the "Add contact to chat" button in the
     * chat window.
     */
    ImageID ADD_TO_CHAT_ICON_PRESSED
        = new ImageID("service.gui.icons.ADD_TO_CHAT_ICON_PRESSED");

    /**
     * The disabled icon image of the "Add contact to chat" button in the
     * chat window.
     */
    ImageID ADD_TO_CHAT_ICON_DISABLED
        = new ImageID("service.gui.icons.ADD_TO_CHAT_ICON_DISABLED");

    /**
     * The image used for decoration of the "Rename contact" window.
     */
    ImageID RENAME_DIALOG_ICON
        = new ImageID("service.gui.icons.RENAME_DIALOG_ICON");

    /**
     * The image used for error messages in the chat window.
     */
    ImageID EXCLAMATION_MARK
        = new ImageID("service.gui.icons.EXCLAMATION_MARK");

    /**
     * The image used for opened groups.
     */
    ImageID OPENED_GROUP_ICON
        = new ImageID("service.gui.icons.OPENED_GROUP");

    /**
     * The image used for closed groups.
     */
    ImageID CLOSED_GROUP_ICON
        = new ImageID("service.gui.icons.CLOSED_GROUP");

    /**
     * The image used to indicate in the contact list that a message is received
     * from a certain contact.
     */
    ImageID MESSAGE_RECEIVED_ICON
        = new ImageID("service.gui.icons.MESSAGE_RECEIVED_ICON");

    /**
     * Background image of the dial button.
     */
    ImageID DIAL_BUTTON_BG
        = new ImageID("service.gui.buttons.DIAL_BUTTON_BG");

    /**
     * Title bar background image.
     */
    ImageID WINDOW_TITLE_BAR
        = new ImageID("service.gui.WINDOW_TITLE_BAR");

    /**
     * The default icon used in file transfer ui.
     */
    ImageID DEFAULT_FILE_ICON
        = new ImageID("service.gui.icons.DEFAULT_FILE_ICON");

    /**
     * The video call menu item icon.
     */
    ImageID VIDEO_CALL
        = new ImageID("service.gui.icons.VIDEO_CALL_16x16_ICON");

    /**
     * The volume control button icon.
     */
    ImageID VOLUME_CONTROL_BUTTON
        = new ImageID("service.gui.buttons.VOLUME_CONTROL");

    /**
     * The volume muted button icon
     */
    ImageID VOLUME_MUTED_BUTTON
        = new ImageID("service.gui.buttons.VOLUME_MUTED");

    /*
     * =======================================================================
     * ------------------------ EDIT TOOLBAR ICONS ---------------------------
     * =======================================================================
     */
    /**
     * Add not in contact list contact icon.
     */
    ImageID ADD_CONTACT_CHAT_ICON
        = new ImageID("service.gui.icons.ADD_CONTACT_CHAT_ICON");

    /**
     * The icon shown in the invite dialog.
     */
    ImageID CLOSE_VIDEO
        = new ImageID("service.gui.buttons.CLOSE_VIDEO");

    /*
     * =======================================================================
     * ------------------------ MAIN TOOLBAR ICONS ---------------------------
     * =======================================================================
     */

    /**
     * Copy icon.
     */
    ImageID COPY_ICON
        = new ImageID("service.gui.icons.COPY_ICON");

    /**
     * Cut icon.
     */
    ImageID CUT_ICON
        = new ImageID("service.gui.icons.CUT_ICON");

    /**
     * Paste icon.
     */
    ImageID PASTE_ICON
        = new ImageID("service.gui.icons.PASTE_ICON");

    /**
     * Smiley icon, used for the "Smiley" button in the <tt>MainToolBar</tt>.
     */
    ImageID SMILIES_ICON
        = new ImageID("service.gui.icons.SMILIES_ICON");

    /**
     * Close icon.
     */
    ImageID CLOSE_ICON
        = new ImageID("service.gui.icons.CLOSE_ICON");

    /**
     * History background image.
     */
    ImageID HISTORY_BG
        = new ImageID("service.gui.icons.HISTORY_BG");

    /**
     * History rollover image.
     */
    ImageID HISTORY_ROLLOVER
        = new ImageID("service.gui.icons.HISTORY_ROLLOVER");

    /**
     * History pressed image.
     */
    ImageID HISTORY_PRESSED
        = new ImageID("service.gui.icons.HISTORY_PRESSED");

    /**
     * Send file background image.
     */
    ImageID SEND_FILE_BG
        = new ImageID("service.gui.icons.SEND_FILE_BG");

    /**
     * Send file rollover image.
     */
    ImageID SEND_FILE_ROLLOVER
        = new ImageID("service.gui.icons.SEND_FILE_ROLLOVER");

    /**
     * Send file pressed image.
     */
    ImageID SEND_FILE_PRESSED
        = new ImageID("service.gui.icons.SEND_FILE_PRESSED");

    /**
     * Send file disabled image.
     */
    ImageID SEND_FILE_DISABLED
        = new ImageID("service.gui.icons.SEND_FILE_DISABLED");

    /**
     * Start web conference background image.
     */
    ImageID WEB_CONFERENCE_BG
        = new ImageID("service.gui.icons.WEB_CONFERENCE_BG");

    /**
     * Start web conference rollover image.
     */
    ImageID WEB_CONFERENCE_ROLLOVER
        = new ImageID("service.gui.icons.WEB_CONFERENCE_ROLLOVER");

    /**
     * Start web conference pressed image.
     */
    ImageID WEB_CONFERENCE_PRESSED
        = new ImageID("service.gui.icons.WEB_CONFERENCE_PRESSED");

    /*
     * =======================================================================
     * ------------------------ CHAT CONTACT ICONS ---------------------------
     * =======================================================================
     */
    ImageID CHAT_CONFIGURE_ICON
        = new ImageID("service.gui.icons.CHAT_CONFIGURE_ICON");

    ImageID CHAT_CONFIGURE_ICON_ROLLOVER
        = new ImageID("service.gui.icons.CHAT_CONFIGURE_ICON_ROLLOVER");

    ImageID CHAT_CONFIGURE_ICON_PRESSED
        = new ImageID("service.gui.icons.CHAT_CONFIGURE_ICON_PRESSED");

    ImageID CHAT_CONFIGURE_ICON_DISABLED
        = new ImageID("service.gui.icons.CHAT_CONFIGURE_ICON_DISABLED");

    /*
     * =======================================================================
     * ------------------------- 16x16 ICONS ---------------------------------
     * =======================================================================
     */
    /**
     * Send message 16x16 image.
     */
    ImageID SEND_MESSAGE_16x16_ICON
        = new ImageID("service.gui.icons.SEND_MESSAGE_16x16_ICON");

    /**
     * Call 16x16 image.
     * //TODO : change to an appropriate logo
     */
    ImageID CALL_16x16_ICON
            = new ImageID("service.gui.icons.CALL_16x16_ICON");

    /**
     * Delete 16x16 image.
     */
    ImageID DELETE_16x16_ICON
        = new ImageID("service.gui.icons.DELETE_16x16_ICON");

    /**
     * History 16x16 image.
     */
    ImageID HISTORY_16x16_ICON
        = new ImageID("service.gui.icons.HISTORY_16x16_ICON");

    /**
     * Send file 16x16 image.
     */
    ImageID SEND_FILE_16x16_ICON
        = new ImageID("service.gui.icons.SEND_FILE_16x16_ICON");

    /**
     * Add contact 16x16 image.
     */
    ImageID ADD_CONTACT_16x16_ICON
        = new ImageID("service.gui.icons.ADD_CONTACT_16x16_ICON");

    /**
     * Rename 16x16 image.
     */
    ImageID RENAME_16x16_ICON
        = new ImageID("service.gui.icons.RENAME_16x16_ICON");

    /**
     * Toolbar drag area icon.
     */
    ImageID TOOLBAR_DRAG_ICON = new ImageID(
            "service.gui.icons.TOOLBAR_DRAG_ICON");

    /**
     * The background image of the <tt>AuthenticationWindow</tt>.
     */
    ImageID AUTH_WINDOW_BACKGROUND = new ImageID(
            "service.gui.AUTH_WINDOW_BACKGROUND");

    /**
     * The icon used to indicate a search.
     */
    ImageID SEARCH_ICON
        = new ImageID("service.gui.icons.SEARCH_ICON");

    /*
     * =======================================================================
     * ------------------------ USERS' ICONS ---------------------------------
     * =======================================================================
     */

    /**
     * Contact "online" icon.
     */
    ImageID USER_ONLINE_ICON
        = new ImageID("service.gui.statusicons.USER_ONLINE_ICON");

    /**
     * Contact "offline" icon.
     */
    ImageID USER_OFFLINE_ICON
        = new ImageID("service.gui.statusicons.USER_OFFLINE_ICON");

    /**
     * Contact "offline" icon for non-Accession contacts.
     */
    ImageID USER_OFFLINE_LSM_ICON
        = new ImageID("service.gui.statusicons.USER_OFFLINE_LSM_ICON");

    /**
     * Contact "away" icon.
     */
    ImageID USER_AWAY_ICON
        = new ImageID("service.gui.statusicons.USER_AWAY_ICON");

    /**
     * Contact "free for chat" icon.
     */
    ImageID USER_FFC_ICON
        = new ImageID("service.gui.statusicons.USER_FFC_ICON");

    /**
     * Contact "do not disturb" icon.
     */
    ImageID USER_DND_ICON
        = new ImageID("service.gui.statusicons.USER_DND_ICON");

    /**
     * Contact "do not disturb" icon for non-Accession contacts.
     */
    ImageID USER_DND_LSM_ICON
        = new ImageID("service.gui.statusicons.USER_DND_LSM_ICON");

    /**
     * Contact "busy" icon
     */
    ImageID USER_BUSY_ICON
        = new ImageID("service.gui.statusicons.USER_BUSY_ICON");

    /**
     * Contact "in meeting" icon
     */
    ImageID USER_MEETING_ICON
        = new ImageID("service.gui.statusicons.USER_MEETING_ICON");

    /**
     * Contact "on the phone" icon.
     */
    ImageID USER_ON_THE_PHONE_ICON
        = new ImageID("service.gui.statusicons.USER_ON_THE_PHONE_ICON");

    /**
     * Contact "on the phone" icon for non-Accession contacts.
     */
    ImageID USER_ON_THE_PHONE_LSM_ICON
        = new ImageID("service.gui.statusicons.USER_ON_THE_PHONE_LSM_ICON");

    /**
     * Contact "ringing" icon.
     */
    ImageID USER_RINGING_ICON
        = new ImageID("service.gui.statusicons.USER_RINGING_ICON");

    /**
     * Contact "available for calls" icon.
     */
    ImageID USER_AVAILABLE_FOR_CALLS_ICON
        = new ImageID("service.gui.statusicons.USER_AVAILABLE_FOR_CALLS_ICON");

    /**
     * Contact "available for calls" icon for non-Accession contacts.
     */
    ImageID USER_AVAILABLE_FOR_CALLS_LSM_ICON
        = new ImageID("service.gui.statusicons.USER_AVAILABLE_FOR_CALLS_LSM_ICON");

    /**
     * Contact "has no status" icon.
     */
    ImageID USER_NO_STATUS_ICON
        = new ImageID("service.gui.statusicons.USER_NO_STATUS_ICON");

    /**
     * Contact "status unknown" icon.
     */
    ImageID USER_STATUS_UNKNOWN_ICON
        = new ImageID("service.gui.statusicons.USER_STATUS_UNKNOWN_ICON");

    ImageID MICROPHONE
        = new ImageID("service.gui.soundlevel.MICROPHONE");

    /**
     * Icon used to indicate that the user is a favorite
     */
    ImageID FAVORITE_ICON =
                                       new ImageID("service.gui.FAVORITE_ICON");

    /**
     * Icon used to indicate an IM chat transport
     */
    ImageID CHAT_MESSAGE =
                                  new ImageID("service.gui.icons.CHAT_MESSAGE");

    /**
     * Icon used to indicate an IM chat transport
     */
    ImageID SMS_MESSAGE =
                                   new ImageID("service.gui.icons.SMS_MESSAGE");

    /**
     * The background to use on the dormant group chat panel
     */
    ImageID DORMANT_GROUP_CHAT_BACKGROUND =
                 new ImageID("service.gui.DORMANT_GROUP_CHAT_BACKGROUND");

    /**
     * The image icon to display on a selected cell to show it is selected
     */
    ImageID CONTACT_LIST_SELECTED =
                      new ImageID("service.gui.buttons.CONTACT_LIST_SELECTED");

    /**
     * The image icon to display on an selected cell to show it is not selected
     */
    ImageID CONTACT_LIST_UNSELECTED =
                      new ImageID("service.gui.buttons.CONTACT_LIST_UNSELECTED");

    /**
     * The image icon to display on a disabled unselected cell
     */
    ImageID CONTACT_LIST_DISABLED_UNSELECTED =
            new ImageID("service.gui.buttons.CONTACT_LIST_DISABLED_UNSELECTED");

    /**
     * The image icon to display on a disabled selected cell
     */
    ImageID CONTACT_LIST_DISABLED_SELECTED =
              new ImageID("service.gui.buttons.CONTACT_LIST_DISABLED_SELECTED");

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    BufferedImageFuture getImage(ImageID imageID);

    /**
     * Returns the URI corresponding to the image with the given image
     * identifier.
     *
     * @param imageID the identifier of the image
     * @return the URI corresponding to the image with the given image
     * identifier
     */
    String getImageUri(ImageID imageID);

    /**
     * Obtains the indexed status image for the given protocol provider.
     *
     * @param pps the protocol provider for which to create the image
     *
     * @return the indexed status image
     */
    ImageIconFuture getAccountStatusImage(ProtocolProviderService pps);

    /**
     * Returns an icon for the given protocol image with an index allowing to
     * distinguish different accounts from the same protocol.
     *
     * @param image the initial image to badge with an index
     * @param pps the protocol provider service corresponding to the account,
     * containing the index.
     * @return an icon for the given protocol image with an index allowing to
     * distinguish different accounts from the same protocol.
     */
    ImageIconFuture getIndexedProtocolIcon(BufferedImageFuture image,
                                           ProtocolProviderService pps);

    /**
     * Returns the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     *
     * @param image the initial image to badge with an index
     * @param pps the protocol provider service corresponding to the account,
     * containing the index.
     * @return the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     */
    BufferedImageFuture getIndexedProtocolImage(
            BufferedImageFuture image, ProtocolProviderService pps);

    /**
     * Returns the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     *
     * @param bgImage the background image
     * @param topImage the image that should be painted on the top of the
     * background image
     * @param x the x coordinate of the top image
     * @param y the y coordinate of the top image
     * @return the result merged image
     */
    BufferedImageFuture getImage(BufferedImageFuture bgImage,
                                 BufferedImageFuture topImage,
                                 int x,
                                 int y);

    /**
     * Returns the given protocol image with an index allowing to distinguish
     * different accounts from the same protocol.
     *
     * @param bgImage the background image
     * @param text the text that should be painted on the top of the
     * background image
     * @return the result merged image
     */
    BufferedImageFuture getImage(BufferedImageFuture bgImage,
                                 String text,
                                 Component c);

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    BufferedImageFuture getImageFromPath(String imagePath);

    /**
     * Loads an image icon from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    ImageIconFuture getIconFromPath(String imagePath);

    /**
     * Clears the images cache.
     */
    void clearCache();

    /**
     * Returns the icon corresponding to the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which icon
     * we're looking for
     * @return the icon to show on the authentication window
     */
    ImageIconFuture getAuthenticationWindowIcon(
            ProtocolProviderService protocolProvider);
}
