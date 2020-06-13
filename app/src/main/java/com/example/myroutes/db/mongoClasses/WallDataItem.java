package com.example.myroutes.db.mongoClasses;

import android.graphics.Path;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.opencv.core.Point;

import java.util.ArrayList;

public class WallDataItem {
    public static final String WALL_DATABASE = "myRoutesApp";
    public static final String WALL_ITEMS_COLLECTION = "wall-data";

    private final String user_id;
    private final String wall_id;
    private final ArrayList<ArrayList<Point>> contours;
    private final ArrayList<Path> paths;
    private String wall_name;

    public WallDataItem (
            final String user_id,
            final String wall_id,
            final String wall_name,
            final ArrayList<ArrayList<Point>> contours,
            final ArrayList<Path> paths) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.wall_name = wall_name;
        this.contours = contours;
        this.paths = paths;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public String getWall_name() {
        return wall_name;
    }

    public void setWall_name(String name) { this.wall_name = name; }

    public ArrayList<ArrayList<Point>> getContours() { return contours; }

    public ArrayList<Path> getPaths() {
        return paths;
    }

    static BsonDocument toBsonDocument(final WallDataItem item) {
        final BsonDocument asDoc = new BsonDocument();
        asDoc.put(Fields.USER_ID, new BsonString(item.getUser_id()));
        asDoc.put(Fields.WALL_ID, new BsonString(item.getWall_id()));
        asDoc.put(Fields.WALL_NAME, new BsonString(item.getWall_name()));

        // Add point array
        BsonArray contours = new BsonArray();
        ArrayList<ArrayList<Point>> contourList = item.getContours();
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
        asDoc.put(Fields.CONTOURS, contours);

        return asDoc;
    }

    static WallDataItem fromBsonDocument(final BsonDocument doc) {
        // Get list of points and paths
        ArrayList<ArrayList<Point>> contourList = new ArrayList<>();
        ArrayList<Path> pathList = new ArrayList<>();
        BsonArray contourArr = doc.getArray(Fields.CONTOURS);
        for (int i = 0; i < contourArr.size(); i++) {
            ArrayList<Point> hullList = new ArrayList<>();
            Path path = new Path();
            BsonArray hullArr = (BsonArray) contourArr.get(i);
            for (int j = 0; j < hullArr.size(); j++) {
                BsonDocument point = (BsonDocument) hullArr.get(j);
                Point p = new Point(point.getDouble("x").getValue(),
                        point.getDouble("y").getValue());
                if (j == 0) { path.moveTo((float) p.x, (float) p.y); }
                else {path.lineTo((float) p.x, (float) p.y); }
                hullList.add(p);
            }
            contourList.add(hullList);
            pathList.add(path);
        }

        return new WallDataItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                doc.getString(Fields.WALL_NAME).getValue(),
                contourList,
                pathList
        );
    }

    static final class Fields {
        static final String USER_ID = "user_id";
        static final String WALL_ID = "wall_id";
        static final String WALL_NAME = "wall_name";
        static final String CONTOURS = "contours";
    }

    public static final Codec<WallDataItem> codec = new Codec<WallDataItem>() {

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
            return fromBsonDocument(document);
        }
    };
}

