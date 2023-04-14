package ru.topradio.util;


import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;

/**
 * Default implementation of {@link PlayerNotificationManager.MediaDescriptionAdapter}.
 *
 * <p>Uses values from the {@link Player#getMediaMetadata() player mediaMetadata} to populate the
 * notification.
 */
public final class DefaultMediaDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

    @Nullable
    private final PendingIntent pendingIntent;

    /**
     * Creates a default {@link PlayerNotificationManager.MediaDescriptionAdapter}.
     *
     * @param pendingIntent The {@link PendingIntent} to be returned from {@link
     *     #createCurrentContentIntent(Player)}, or null if no intent should be fired.
     */
    public DefaultMediaDescriptionAdapter(@Nullable PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    @Override
    public CharSequence getCurrentContentTitle(Player player) {
        @Nullable CharSequence displayTitle = player.getMediaMetadata().displayTitle;
        if (!TextUtils.isEmpty(displayTitle)) {
            return displayTitle;
        }

        @Nullable CharSequence title = player.getMediaMetadata().title;
        return title != null ? title : "";
    }

    @Nullable
    @Override
    public PendingIntent createCurrentContentIntent(Player player) {
        return pendingIntent;
    }

    @Nullable
    @Override
    public CharSequence getCurrentContentText(Player player) {
        @Nullable CharSequence artist = player.getMediaMetadata().artist;
        if (!TextUtils.isEmpty(artist)) {
            return artist;
        }

        return player.getMediaMetadata().albumArtist;
    }

    @Nullable
    @Override
    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
        @Nullable byte[] data = player.getMediaMetadata().artworkData;
        if (data == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(data, /* offset= */ 0, data.length);
    }
}

