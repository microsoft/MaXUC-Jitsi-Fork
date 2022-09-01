// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.StandaloneMainFrameController;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.impl.gui.main.presence.AccountStatusPanel;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.Logger;

public class StandaloneMainFrame extends AbstractMainFrame
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The <tt>Logger</tt> used by the <tt>UIServiceImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(StandaloneMainFrame.class);

    /**
     * The JFXPanel that will contain our JavaFX UI
     */
    private JFXPanel fxPanel;

    /**
     * The root JavaFx layout
     */
    private VBox rootLayout;

    /**
     * The current showing JavaFX scene
     */
    protected Scene scene;

    public StandaloneMainFrame()
    {
        // Don't use the background colour and use native instead. Using a
        // coloured background means that when resizing, we see the colour briefly
        // behind our JFXPanel.
        super(null, null, false);

        // Create the JFXPanel and add it to our frame.
        fxPanel = new JFXPanel();
        add(fxPanel);

        // Load the FXML file that defines the standalone meeting GUI
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(GuiActivator.getResources()
            .getResourceURLFromResourceKey("impl.gui.STANDALONE_MAIN_FRAME_LAYOUT"));

        // Set the controller for the loaded FXML
        loader.setController(new StandaloneMainFrameController());

        try
        {
            rootLayout = (VBox) loader.load();

            // Show the scene containing the root layout.
            scene = new Scene(rootLayout);

            // Add the css stylesheet to style the UI
            scene.getStylesheets().add(
                GuiActivator.getResources().getResourceURLFromResourceKey(
                    "impl.gui.STANDALONE_MAIN_FRAME_STYLE").toString());

            // Set the fxFrame's scene to be the one created so that it gets
            // displayed
            setScene(scene);
        }
        catch (IOException e)
        {
            logger.error("Unable to load UI: " +  e.getMessage());
        }
    }

    /**
     * Sets the stage's scene so that it can be displayed.
     *
     * @param scene is the Scene that the stage should display
     */
    public void setScene(Scene scene)
    {
        this.scene = scene;

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                fxPanel.setScene(scene);
            }
        });
    }

    /**
     * Get the JFXPanel that's acting as the JavaFX stage.
     *
     * @return JFXPanel that wraps JavaFX content.
     */
    public JFXPanel getFXPanel()
    {
        return fxPanel;
    }

    /**
     * Get the JFXPanel that's acting as the JavaFX stage.
     *
     * @return JFXPanel that wraps JavaFX content.
     */
    public Scene getScene()
    {
        return scene;
    }

    @Override
    protected Dimension getMinimumBoundsSize()
    {
        int width = GuiActivator.getResources()
            .getScaledSize("impl.gui.STANDALONE_WINDOW_MIN_WIDTH");

        int height = GuiActivator.getResources()
            .getScaledSize("impl.gui.STANDALONE_WINDOW_MIN_HEIGHT");

        return new Dimension(width, height);
    }

    @Override
    protected Dimension getPreferredBoundsSize()
    {
        int width = GuiActivator.getResources()
            .getScaledSize("impl.gui.STANDALONE_WINDOW_WIDTH");

        int height = GuiActivator.getResources()
            .getScaledSize("impl.gui.STANDALONE_WINDOW_HEIGHT");

        return new Dimension(width, height);
    }

    @Override
    public void setDefaultFont(Font font)
    {
        scene.getRoot().setStyle("-fx-font-family: \"" + font.getFamily() + "\"; ");
    }

    @Override
    public ContactListPane getContactListPanel()
    {
        // Do nothing - feature not supported
        return null;
    }

    @Override
    public void enableUnknownContactView(boolean isEnabled)
    {
        // Do nothing - feature not supported
    }

    @Override
    public void repaintWindow()
    {

    }

    @Override
    public void selectTab(String tabName)
    {
        // Do nothing - feature not supported
    }

    @Override
    public void selectSubTab(String tabName)
    {
        // Do nothing - feature not supported
    }

    @Override
    public void setSubTabNotification(String tabName,
        boolean notificationsActive)
    {
        // Do nothing - feature not supported
    }

    @Override
    public void addProtocolProvider(ProtocolProviderService protocolProvider)
    {
        // Do nothing - feature not supported
    }

    @Override
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        // Do nothing - feature not supported
    }

    @Override
    public void removeProtocolProvider(AccountID accountID)
    {
        // Do nothing - feature not supported
    }

    @Override
    public AccountStatusPanel getAccountStatusPanel()
    {
        // Do nothing - feature not supported
        return null;
    }

    @Override
    public OperationSetMultiUserChat getMultiUserChatOpSet(
        ProtocolProviderService protocolProvider)
    {
        // Do nothing - feature not supported
        return null;
    }

    @Override
    public boolean hasProtocolProvider(ProtocolProviderService protocolProvider)
    {
        // Do nothing - feature not supported
        return false;
    }

    @Override
    public void setContactList(MetaContactListService contactListService)
    {
        // Do nothing - feature not supported
    }

    @Override
    public int getProviderIndex(ProtocolProviderService protocolProvider)
    {
        // Do nothing - feature not supported
        return 0;
    }

    @Override
    public void addNativePlugins()
    {
        // Do nothing - feature not supported
    }
}
