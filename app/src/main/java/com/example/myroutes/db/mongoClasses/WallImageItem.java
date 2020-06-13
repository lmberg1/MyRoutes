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
    public static final String WALL_IMAGE_DATABASE = "myRoutesApp";
    public static final String WALL_IMAGE_COLLECTION = "image-data";

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

    static BsonDocument toBsonDocument(final WallImageItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.IMAGE, new BsonBinary(item.getByte_arr()));
        return asDoc;
    }

    static WallImageItem fromBsonDocument(final BsonDocument doc) {
        // Get image
        byte[] byteArr =  doc.getBinary(Fields.IMAGE).getData();

        return new WallImageItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length)
        );
    }

    public static final class Fields {
        public static final String USER_ID = "user_id";
        public static final String WALL_ID = "wall_id";
        public static final String IMAGE = "image";
    }

    public static final Codec<WallImageItem> codec = new Codec<WallImageItem>() {

        @Override
        public void encode(
                final BsonWriter writer, final WallImageItem value, final EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<WallImageItem> getEncoderClass() {
            return WallImageItem.class;
        }

        @Override
        public WallImageItem decode(
                final BsonReader reader, final DecoderContext decoderContext) {
            final BsonDocument document = (new BsonDocumentCodec()).decode(reader, decoderContext);
            return fromBsonDocument(document);
        }
    };
}

