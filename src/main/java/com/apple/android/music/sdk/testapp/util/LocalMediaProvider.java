package com.apple.android.music.sdk.testapp.util;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.JsonReader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.apple.android.music.playback.model.MediaContainerType;
import com.apple.android.music.playback.model.MediaItemType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */

public final class LocalMediaProvider {

    public static final String MEDIA_ROOT_ID = "MEDIA_ROOT";

    private final Context applicationContext;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public LocalMediaProvider(Context context) {
        applicationContext = context.getApplicationContext();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadMediaItems(@NonNull String parentId, @NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
        executorService.execute(new DataLoader(applicationContext, parentId, result, mainHandler));
    }




    private static class DataLoader implements Runnable {

        private final Context applicationContext;
        private final String fileName;
        private final MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result;
        private final Handler mainHandler;

        DataLoader(Context applicationContext, String parentId, MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result, Handler mainHandler) {
            this.applicationContext = applicationContext;
            this.fileName = getFileName(parentId);
            this.result = result;
            this.mainHandler = mainHandler;
        }

        @Override
        public void run() {
            try {
                final List<MediaBrowserCompat.MediaItem> items = readItemsFromFile(applicationContext, fileName);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        result.sendResult(items);
                    }
                });
            } catch (IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        result.sendResult(Collections.<MediaBrowserCompat.MediaItem>emptyList());
                    }
                });
            }
        }


        private static String getFileName(String parentId) {
            StringBuilder result = new StringBuilder("media_data/");
            if (MEDIA_ROOT_ID.equals(parentId)) {
                result.append("root");
            } else {
                result.append(parentId.replace(':', '_'));
            }
            result.append(".json");
            return result.toString();
        }


        private static List<MediaBrowserCompat.MediaItem> readItemsFromFile(Context context, String fileName) throws IOException {
            final JsonReader reader = new JsonReader(new InputStreamReader(context.getAssets().open(fileName), "UTF-8"));
            try {
                return readItemsArray(reader);
            } finally {
                reader.close();
            }
        }


        private static List<MediaBrowserCompat.MediaItem> readItemsArray(JsonReader reader) throws IOException {
            List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                result.add(readItem(reader));
            }
            reader.endArray();
            return result;
        }

        private static MediaBrowserCompat.MediaItem readItem(JsonReader reader) throws IOException {
            int flags = 0;
            MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();
            reader.beginObject();
            while (reader.hasNext()) {
                final String fieldName = reader.nextName();
                if ("id".equals(fieldName)) {
                    mediaDescriptionBuilder.setMediaId(reader.nextString());
                } else if ("title".equals(fieldName)) {
                    mediaDescriptionBuilder.setTitle(reader.nextString());
                } else if ("subtitle".equals(fieldName)) {
                    mediaDescriptionBuilder.setSubtitle(reader.nextString());
                } else if ("description".equals(fieldName)) {
                    mediaDescriptionBuilder.setDescription(reader.nextString());
                } else if ("browseable".equals(fieldName)) {
                    if (reader.nextBoolean()) {
                        flags |= MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
                    }
                } else if ("playable".equals(fieldName)) {
                    if (reader.nextBoolean()) {
                        flags |= MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
                    }
                } else if ("mediaUri".equals(fieldName)) {
                    mediaDescriptionBuilder.setMediaUri(Uri.parse(reader.nextString()));
                } else if ("iconUri".equals(fieldName)) {
                    mediaDescriptionBuilder.setIconUri(Uri.parse(reader.nextString()));
                } else if ("type".equals(fieldName)) {
                    String type = reader.nextString();
                    Bundle extras = new Bundle(1);
                    if ("song".equals(type)) {
                        extras.putInt("itemType", MediaItemType.SONG);
                    } else if ("album".equals(type)) {
                        extras.putInt("containerType", MediaContainerType.ALBUM);
                    } else if ("playlist".equals(type)) {
                        extras.putInt("containerType", MediaContainerType.PLAYLIST);
                    }
                    mediaDescriptionBuilder.setExtras(extras);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), flags);
        }
    }
}
