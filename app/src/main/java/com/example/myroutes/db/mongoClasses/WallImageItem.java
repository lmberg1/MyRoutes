package com.example.myroutes.db.mongoClasses;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.io.ByteArrayOutputStream;

public class WallImageItem {
    private final String user_id;
    private final String wall_id;
    private final Bitmap bitmap;
    private final byte[] byte_arr;

    public WallImageItem(
            final String user_id,
            final String wall_id,
            final Bitmap image) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.bitmap = image;
        this.byte_arr = findByte_arr(image);
    }

    private static byte[] findByte_arr(Bitmap b) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public byte[] getByte_arr() {
        return byte_arr;
    }
}

