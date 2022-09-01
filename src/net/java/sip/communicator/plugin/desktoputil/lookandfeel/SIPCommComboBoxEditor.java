/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.border.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The default editor for SIPCommunicator editable combo boxes.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommComboBoxEditor
    extends MetalComboBoxEditor
    implements Skinnable
{
    /**
     * Creates an instance of <tt>SIPCommComboBoxEditor</tt>.
     */
    public SIPCommComboBoxEditor()
    {
        editor.setBorder(new EditorBorder());

        // enables delete button
        if (editor.getUI() instanceof SIPCommTextFieldUI)
        {
            ((SIPCommTextFieldUI) editor.getUI())
                .setDeleteButtonEnabled(true);
        }
    }

    /**
     * The editor border insets.
     */
    protected static final Insets editorBorderInsets
        = new Insets(2, 2, 2, 0);

    /**
     * The default editor border insets.
     */
    private static final Insets SAFE_EDITOR_BORDER_INSETS
        = new Insets(2, 2, 2, 0);

    /**
     * A custom editor border.
     */
    private static class EditorBorder
        extends AbstractBorder
    {
        private static final long serialVersionUID = 0L;

        public void paintBorder(Component c, Graphics g, int x, int y, int w,
                int h)
        {
            g = g.create();
            try
            {
                internalPaintBorder(g, x, y, w, h);
            }
            finally
            {
                g.dispose();
            }
        }

        /**
         * Paints a custom rounded border for the combo box editor.
         *
         * @param g the <tt>Graphics</tt> object used for painting
         * @param x the x coordinate
         * @param y the y coordinate
         * @param w the width of the border
         * @param h the height of the border
         */
        private void internalPaintBorder(Graphics g, int x, int y,
                int w, int h)
        {
            Graphics2D g2d = (Graphics2D)g;

            AntialiasingManager.activateAntialiasing(g2d);

            g2d.translate(x, y);

            g2d.setColor(SIPCommLookAndFeel.getControlDarkShadow());

            GeneralPath path = new GeneralPath();
            int round = 2;

            path.moveTo(w, h-1);
            path.lineTo(round, h-1);
            path.curveTo(round, h-1, 0, h-1, 0, h-round-1);
            path.lineTo(0, round);
            path.curveTo(0, round, 0, 0, round, 0);
            path.lineTo(w, 0);

            g2d.draw(path);

            g2d.translate(-x, -y);
        }

        public Insets getBorderInsets( Component c )
        {
            if (System.getSecurityManager() != null)
            {
                return SAFE_EDITOR_BORDER_INSETS;
            }
            else
            {
                return editorBorderInsets;
            }
        }
    }

    /**
     * A subclass of SIPCommComboBoxEditor that implements UIResource.
     * SIPCommComboBoxEditor doesn't implement UIResource
     * directly so that applications can safely override the
     * cellRenderer property with BasicListCellRenderer subclasses.
     */
    public static class UIResource extends SIPCommComboBoxEditor
        implements javax.swing.plaf.UIResource {}

    /**
     * Reloads UI if necessary.
     */
    public void loadSkin()
    {
        if (editor.getUI() instanceof SIPCommTextFieldUI)
        {
            ((SIPCommTextFieldUI) editor.getUI()).loadSkin();
        }
    }
}
