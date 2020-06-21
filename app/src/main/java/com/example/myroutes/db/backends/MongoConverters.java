package com.example.myroutes.db.backends;

import android.graphics.BitmapFactory;

import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.db.entities.WallImageItem;
import com.example.myroutes.db.entities.WorkoutItem;

import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class MongoConverters {
    public static final String DATABASE = "myRoutesApp";
    public static final String BOULDER_ITEMS_COLLECTION = "boulder-data";
    public static final String WALL_ITEMS_COLLECTION = "wall-data";
    public static final String WALL_IMAGE_COLLECTION = "image-data";
    public static final String WORKOUT_COLLECTION = "workout-data";

    public static final class Fields {
        // Shared fields
        public static final String USER_ID = "user_id";
        public static final String WALL_ID = "wall_id";
        // WallDataItem fields
        static final String WALL_NAME = "wall_name";
        static final String CONTOURS = "contours";
        // WallImageItem fields
        public static final String IMAGE = "image";
        // BoulderItem fields
        public static final String BOULDER_ID = "boulder_id";
        public static final String BOULDER_NAME = "boulder_name";
        public static final String BOULDER_GRADE = "boulder_grade";
        public static final String BOULDER_HOLDS = "boulder_holds";
        // WorkoutItem fields
        public static final String WORKOUT_ID = "workout_id";
        public static final String WORKOUT_NAME = "name";
        public static final String WORKOUT_SETS="sets";
    }

    /*--------------------------------Convert WallDataItem----------------------------------------*/

    private static BsonArray contoursToBson(ArrayList<ArrayList<Point>> contourList) {
        BsonArray contours = new BsonArray();
        for (ArrayList<Point> hull : contourList) {
            BsonArray points = new BsonArray();
            for (Point point : hull) {
                BsonDocument o = new BsonDocument();
                o.append("x", new BsonDouble(point.x));
                o.append("y", new BsonDouble(point.y));
                points.add(o);
            }
            contours.add(points);
        }
        return contours;
    }

    private static ArrayList<ArrayList<Point>> bsonToContours(BsonArray contourArr) {
        ArrayList<ArrayList<Point>> contourList = new ArrayList<>();
        for (int i = 0; i < contourArr.size(); i++) {
            ArrayList<Point> hullList = new ArrayList<>();
            BsonArray hullArr = (BsonArray) contourArr.get(i);
            for (int j = 0; j < hullArr.size(); j++) {
                BsonDocument point = (BsonDocument) hullArr.get(j);
                Point p = new Point(point.getDouble("x").getValue(),
                        point.getDouble("y").getValue());
                hullList.add(p);
            }
            contourList.add(hullList);
        }
        return contourList;
    }

    static BsonDocument toBsonDocument(final WallDataItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.WALL_NAME, new BsonString(item.getWall_name()));
        asDoc.put(Fields.CONTOURS, contoursToBson(item.getContours()));
        return asDoc;
    }

    static WallDataItem WallDatafromBsonDocument(final BsonDocument doc) {
        return new WallDataItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                doc.getString(Fields.WALL_NAME).getValue(),
                bsonToContours(doc.getArray(Fields.CONTOURS))
        );
    }

    public static final Codec<WallDataItem> wallDataItemCodec = new Codec<WallDataItem>() {
        @Override
        public void encode(
                final BsonWriter writer, final WallDataItem value, final EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<WallDataItem> getEncoderClass() {
            return WallDataItem.class;
        }

        @Override
        public WallDataItem decode(
                final BsonReader reader, final DecoderContext decoderContext) {
            final BsonDocument document = (new BsonDocumentCodec()).decode(reader, decoderContext);
            return WallDatafromBsonDocument(document);
        }
    };

    /*----------------------------------Convert ImageItem-----------------------------------------*/

    static BsonDocument toBsonDocument(final WallImageItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.IMAGE, new BsonBinary(item.getByte_arr()));
        return asDoc;
    }

    static WallImageItem WallImagefromBsonDocument(final BsonDocument doc) {
        // Get image
        byte[] byteArr =  doc.getBinary(Fields.IMAGE).getData();

        return new WallImageItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length)
        );
    }

    public static final Codec<WallImageItem> wallImageCodec = new Codec<WallImageItem>() {

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
            return WallImagefromBsonDocument(document);
        }
    };

    /*---------------------------------Convert BoulderItem----------------------------------------*/

    static BsonDocument toBsonDocument(final BoulderItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.BOULDER_ID, new BsonString(item.getBoulder_id()));
        asDoc.put(Fields.BOULDER_NAME, new BsonString(item.getBoulder_name()));
        asDoc.put(Fields.BOULDER_GRADE, new BsonString(item.getBoulder_grade()));
        asDoc.put(Fields.BOULDER_HOLDS, item.holdsToBson());

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

    public static final Codec<BoulderItem> boulderItemCodec = new Codec<BoulderItem>() {

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

    /*---------------------------------Convert WorkoutItem----------------------------------------*/

    private static List<List<String>> bsonToWorkoutSets(BsonArray bsonSets) {
        List<List<String>> workoutSets = new ArrayList<>();
        int nSets = bsonSets.size();
        for (int i = 0; i < nSets; i++) {
            List<String> boulderIds = new ArrayList<>();
            BsonArray bsonIds = (BsonArray) bsonSets.get(i);
            int nBoulders = bsonIds.size();
            for (int j = 0; j < nBoulders; j++) {
                boulderIds.add(((BsonString) (bsonIds.get(j))).getValue());
            }
            workoutSets.add(boulderIds);
        }
        return workoutSets;
    }

    static BsonDocument toBsonDocument(final WorkoutItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.WORKOUT_ID, new BsonString(item.getWorkout_id()));
        asDoc.put(Fields.WORKOUT_NAME, new BsonString(item.getWorkout_name()));
        asDoc.put(Fields.WORKOUT_SETS, item.setsToBson());
        return asDoc;
    }

    static WorkoutItem WorkoutfromBsonDocument(final BsonDocument doc) {
        return new WorkoutItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                doc.getString(Fields.WORKOUT_ID).getValue(),
                doc.getString(Fields.WORKOUT_NAME).getValue(),
                bsonToWorkoutSets(doc.getArray(Fields.WORKOUT_SETS))
        );
    }

    public static final Codec<WorkoutItem> workoutItemCodec = new Codec<WorkoutItem>() {

        @Override
        public void encode(
                final BsonWriter writer, final WorkoutItem value, final EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<WorkoutItem> getEncoderClass() {
            return WorkoutItem.class;
        }

        @Override
        public WorkoutItem decode(
                final BsonReader reader, final DecoderContext decoderContext) {
            final BsonDocument document = (new BsonDocumentCodec()).decode(reader, decoderContext);
            return WorkoutfromBsonDocument(document);
        }
    };
}

