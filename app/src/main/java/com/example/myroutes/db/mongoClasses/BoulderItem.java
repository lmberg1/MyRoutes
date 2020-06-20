package com.example.myroutes.db.mongoClasses;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;

public class BoulderItem {
    public static final String BOULDER_DATABASE = "myRoutesApp";
    public static final String BOULDER_ITEMS_COLLECTION = "boulder-data";

    private final String wall_id;
    private final String boulder_id;
    private final String boulder_grade;
    private final String user_id;
    private final String boulder_name;
    private final ArrayList<Integer> boulder_holds;

    public BoulderItem(
            final String user_id,
            final String wall_id,
            final String boulder_id,
            final String boulder_name,
            final String boulder_grade,
            final ArrayList<Integer> boulder_holds) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.boulder_id = boulder_id;
        this.boulder_name = boulder_name;
        this.boulder_grade = boulder_grade;
        this.boulder_holds = boulder_holds;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public String getBoulder_id() {
        return boulder_id;
    }

    public String getBoulder_name() {
        return boulder_name;
    }

    public String getBoulder_grade() {
        return boulder_grade;
    }

    public ArrayList<Integer> getBoulder_holds() {
        return boulder_holds;
    }

    public BsonArray holdsToBson() {
        BsonArray holds = new BsonArray();
        for (int ind : boulder_holds) {
            holds.add(new BsonInt32(ind));
        }
        return holds;
    }

    static BsonDocument toBsonDocument(final BoulderItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.BOULDER_ID, new BsonString(item.getBoulder_id()));
        asDoc.put(Fields.BOULDER_NAME, new BsonString(item.getBoulder_name()));
        asDoc.put(Fields.BOULDER_GRADE, new BsonString(item.getBoulder_grade()));

        // Add integer array
        BsonArray holds = new BsonArray();
        ArrayList<Integer> holdList = item.getBoulder_holds();
        for (int ind : holdList) {
            holds.add(new BsonInt32(ind));
        }
        asDoc.put(Fields.BOULDER_HOLDS, holds);

        return asDoc;
    }

    static BoulderItem fromBsonDocument(final BsonDocument doc) {
        // Get integer list
        ArrayList<Integer> holdList = new ArrayList<>();
        BsonArray holdArr = doc.getArray(Fields.BOULDER_HOLDS);
        for (int i = 0; i < holdArr.size(); i++) {
            holdList.add(((BsonInt32) holdArr.get(i)).getValue());
        }

        return new BoulderItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                doc.getString(Fields.BOULDER_ID).getValue(),
                doc.getString(Fields.BOULDER_NAME).getValue(),
                doc.getString(Fields.BOULDER_GRADE).getValue(),
                holdList
        );
    }

    public static final class Fields {
        public static final String USER_ID = "user_id";
        public static final String WALL_ID = "wall_id";
        public static final String BOULDER_ID = "boulder_id";
        public static final String BOULDER_NAME = "boulder_name";
        public static final String BOULDER_GRADE = "boulder_grade";
        public static final String BOULDER_HOLDS = "boulder_holds";
    }

    public static final Codec<BoulderItem> codec = new Codec<BoulderItem>() {

        @Override
        public void encode(
                final BsonWriter writer, final BoulderItem value, final EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<BoulderItem> getEncoderClass() {
            return BoulderItem.class;
        }

        @Override
        public BoulderItem decode(
                final BsonReader reader, final DecoderContext decoderContext) {
            final BsonDocument document = (new BsonDocumentCodec()).decode(reader, decoderContext);
            return fromBsonDocument(document);
        }
    };
}

