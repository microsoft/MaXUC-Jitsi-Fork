// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.image.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

/**
 * This is a helper class to make it easier to implement ImageIconFuture.
 *
 * If you've got an icon already in memory then prefer to just pass it to
 * ImageIconAvailable. If you need to do some work to retrieve the icon,
 * then prefer extending AbstractImageIconPending.
 *
 * If you've got an image, see AbstractBufferedImageFuture.
 */
public abstract class AbstractImageIconFuture implements ImageIconFuture
{
    /**
     * ImageIcon loaded by this image - null if still loading
     */
    protected ImageIcon mIcon;

    @Override
    public void onUiResolve(final Resolution<ImageIcon> resolution)
    {
        onResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon icon)
            {
                if (SwingUtilities.isEventDispatchThread())
                {
                    resolution.onResolution(mIcon);
                }
                else
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            resolution.onResolution(mIcon);
                        }
                    });
                }
            }
        });
    }

    @Override
    public JLabel addToLabel(final JLabel label)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                label.setIcon(image);
            }
        });

        return label;
    }

    @Override
    public void addToButton(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setIcon(image);
            }
        });
    }

    @Override
    public JButton addToButton(final JButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setIcon(image);
            }
        });

        return button;
    }

    @Override
    public JToggleButton addToButton(final JToggleButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setIcon(image);
            }
        });

        return button;
    }

    @Override
    public JMenuItem addToMenuItem(final JMenuItem item)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                item.setIcon(image);
            }
        });

        return item;
    }

    @Override
    public void setImageObserver(final ImageObserver observer)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                if (image != null)
                {
                    image.setImageObserver(observer);
                }
            }
        });
    }

    @Override
    public void addToButtonAsSelected(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setSelectedIcon(image);
            }
        });
    }

    @Override
    public void addToButtonAsRollover(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setRolloverIcon(image);
            }
        });
    }

    @Override
    public void addToButtonAsPressed(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setPressedIcon(image);
            }
        });
    }

    @Override
    public void addToButtonAsRolloverSelected(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setRolloverSelectedIcon(image);
            }
        });
    }

    @Override
    public void addToButtonAsDisabled(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setDisabledIcon(image);
            }
        });
    }

    @Override
    public void addToButtonAsDisabledSelected(final AbstractButton button)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(ImageIcon image)
            {
                button.setDisabledSelectedIcon(image);
            }
        });
    }

    @Override
    public ImageIconFuture withAlternative(final ImageIconFuture alternative)
    {
        return new AlternativeImageIconPending(this, alternative);
    }

    /**
     * Represents an icon which may be of one of two images, which are waiting to
     * be resolved.
     *
     * If the base isn't available, then the alternative will be used instead.
     */
    private static class AlternativeImageIconPending
        extends AbstractImageIconPending
        implements Resolution<ImageIcon>
    {
        private final ImageIconFuture mBase;
        private final ImageIconFuture mAlternative;

        AlternativeImageIconPending(ImageIconFuture base, ImageIconFuture alternative)
        {
            mBase = base;
            mAlternative = alternative;

            mBase.onResolve(this);
            mAlternative.onResolve(this);
        }

        @Override
        public void onResolution(ImageIcon image)
        {
            retrieveIcon();
        }

        @Override
        public void retrieveIcon()
        {
            // We are notified when both the base and the alternative are
            // resolved.
            //
            // If the base is resolved, and isn't null, then just set our
            // icon to that.
            //
            // If the base is resolved, and is null, we need to wait for the
            // alternative to be resolved.
            if (mBase.isDone())
            {
                ImageIcon base = mBase.resolve();

                if (base != null)
                {
                    mIcon = base;

                    retrievedIcon();
                }
                else if (mAlternative.isDone())
                {
                   // Return the alternative, no matter what it is
                   mIcon = mAlternative.resolve();

                   retrievedIcon();
                }
            }
        }
    }
}
