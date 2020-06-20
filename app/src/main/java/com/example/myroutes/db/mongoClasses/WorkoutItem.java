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
import java.util.List;

public class WorkoutItem {
    public static final String WORKOUT_DATABASE = "myRoutesApp";
    public static final String WORKOUT_COLLECTION = "workout-data";

    private final String user_id;
    private final String wall_id;
    private final String workout_id;
    private final String workout_name;
    private final List<List<String>> workoutSets; // List of sets of boulder ids

    public WorkoutItem(
            final String user_id,
            final String wall_id,
            final String workout_id,
            final String workout_name,
            final List<List<String>> workoutSets) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.workout_id = workout_id;
        this.workout_name = workout_name;
        this.workoutSets = workoutSets;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public String getWorkout_id() {
        return workout_id;
    }

    public String getWorkout_name() {
        return workout_name;
    }

    public List<List<String>> getWorkoutSets() {
        return workoutSets;
    }

    public BsonArray setsToBson() {
        // Add string list
        BsonArray bsonSets = new BsonArray();
        for (List<String> boulderIds : workoutSets) {
            BsonArray bsonIds = new BsonArray();
            for (String id : boulderIds) {
                bsonIds.add(new BsonString(id));
            }
            bsonSets.add(bsonIds);
        }
        return bsonSets;
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

    static WorkoutItem fromBsonDocument(final BsonDocument doc) {
        // Get boulderId list
        List<List<String>> workoutSets = new ArrayList<>();
        BsonArray bsonSets = doc.getArray(Fields.WORKOUT_SETS);
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

        return new WorkoutItem(
                doc.getString(Fields.USER_ID).getValue(),
                doc.getString(Fields.WALL_ID).getValue(),
                doc.getString(Fields.WORKOUT_ID).getValue(),
                doc.getString(Fields.WORKOUT_NAME).getValue(),
                workoutSets
        );
    }

    public static final class Fields {
        public static final String USER_ID = "user_id";
        public static final String WALL_ID = "wall_id";
        public static final String WORKOUT_ID = "workout_id";
        public static final String WORKOUT_NAME = "name";
        public static final String WORKOUT_SETS="sets";
    }

    public static final Codec<WorkoutItem> codec = new Codec<WorkoutItem>() {

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
            return fromBsonDocument(document);
        }
    };
}

