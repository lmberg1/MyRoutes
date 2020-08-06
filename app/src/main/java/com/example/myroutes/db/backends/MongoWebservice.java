package com.example.myroutes.db.backends;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myroutes.db.Result;
import com.example.myroutes.db.dao.StringArrayConverter;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.db.entities.WallImageItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.google.android.gms.tasks.Task;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.function.FunctionCredential;
import com.mongodb.stitch.core.internal.common.BsonUtils;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static com.example.myroutes.db.backends.MongoConverters.BOULDER_ITEMS_COLLECTION;
import static com.example.myroutes.db.backends.MongoConverters.DATABASE;
import static com.example.myroutes.db.backends.MongoConverters.WALL_IMAGE_COLLECTION;
import static com.example.myroutes.db.backends.MongoConverters.WALL_ITEMS_COLLECTION;
import static com.example.myroutes.db.backends.MongoConverters.WORKOUT_COLLECTION;

import static com.example.myroutes.SharedViewModel.Status;

public class MongoWebservice {
    private static final String TAG = "MongoWebService";

    private enum MongoTask {ADD, GET, DELETE}

    private static RemoteMongoCollection<WallDataItem> wallCollection;
    private static RemoteMongoCollection<WallImageItem> wallImageCollection;
    private static RemoteMongoCollection<BoulderItem> boulderCollection;
    private static RemoteMongoCollection<WorkoutItem> workoutCollection;

    // Helper class to run a mongo task
    private static class MongoHelper<T> {
        private LiveData<Result<T>> runMongoTask(Task<T> task, MongoTask taskType, String errorMsg) {
            final MutableLiveData<Result<T>> mongoResult = new MutableLiveData<>();
            task.addOnSuccessListener(item -> {
                if ((item == null) && (taskType == MongoTask.GET)) {
                    mongoResult.setValue(new Result<>(null, Status.NOT_FOUND));
                } else {
                    mongoResult.setValue(new Result<>(item, Status.SUCCESS));
                }
            })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, errorMsg, e);
                        mongoResult.setValue(new Result<>(null, Status.FAILURE));
                    });
            return mongoResult;
        }
    }

    /*-------------------------------------HANDLE IMAGES------------------------------------------*/

    public static LiveData<Result<WallImageItem>> getImageFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load image for wall %s", wall_id);
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        return new MongoHelper<WallImageItem>()
                .runMongoTask(wallImageCollection.find(filter).first(), MongoTask.GET, errorMsg);
    }

    public static LiveData<Result<RemoteInsertOneResult>> addImageToMongo(WallImageItem item) {
        assert item != null;
        String errorMsg = "Failed to add image for wall " + item.getWall_id();
        Log.e(TAG, "adding image");
        Log.e(TAG, String.format("%s %s", item.getUser_id(), item.getWall_id()));
        return new MongoHelper<RemoteInsertOneResult>()
                .runMongoTask(wallImageCollection.insertOne(item), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteImageFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete image for wall " + wall_id;
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(wallImageCollection.deleteOne(filter), MongoTask.DELETE, errorMsg);
    }

    /*------------------------------------HANDLE WALL DATA----------------------------------------*/

    public static LiveData<Result<WallDataItem>> getDataFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load data for wall %s", wall_id);
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        return new MongoHelper<WallDataItem>()
                .runMongoTask(wallCollection.find(filter).first(), MongoTask.GET, errorMsg);
    }

    public static LiveData<Result<RemoteInsertOneResult>> addDataToMongo(WallDataItem item) {
        assert item != null;
        String errorMsg = "Failed to add data for wall " + item.getWall_id();
        return new MongoHelper<RemoteInsertOneResult>()
                .runMongoTask(wallCollection.insertOne(item), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteDataFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete data for wall " + wall_id;
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(wallCollection.deleteOne(filter), MongoTask.DELETE, errorMsg);
    }

    /*------------------------------------HANDLE BOULDERS-----------------------------------------*/

    public static LiveData<Result<List<BoulderItem>>> getBouldersFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load boulders for wall %s", wall_id);
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        List<BoulderItem> items = new ArrayList<>();
        return new MongoHelper<List<BoulderItem>>()
                .runMongoTask(boulderCollection.find(filter).into(items), MongoTask.GET, errorMsg);
    }

    public static LiveData<Result<RemoteUpdateResult>> editBoulderInMongo(BoulderItem item) {
        assert item != null;
        String errorMsg = "Failed to add boulder for wall " + item.getWall_id();
        Bson filter = Filters.and(Filters.eq(MongoConverters.Fields.WALL_ID, item.getWall_id()),
                Filters.eq(MongoConverters.Fields.BOULDER_ID, item.getBoulder_id()));
        BasicDBObject updateQuery = new BasicDBObject();
        BasicDBObject updateItems = new BasicDBObject();
        // Create document of possible updated items
        updateItems.append(MongoConverters.Fields.BOULDER_NAME, item.getBoulder_name())
                .append(MongoConverters.Fields.BOULDER_GRADE, item.getBoulder_grade())
                .append(MongoConverters.Fields.BOULDER_HOLDS, item.holdsToBson());
        if (item.hasStart_holds()) {
            updateItems.append(MongoConverters.Fields.START_HOLDS, item.intListToBson(item.getStart_holds()));
        }
        if (item.hasFinish_hold()) {
            updateItems.append(MongoConverters.Fields.FINISH_HOLD, item.getFinish_hold());
        }
        updateQuery.append("$set", updateItems);
        return new MongoHelper<RemoteUpdateResult>()
                .runMongoTask(boulderCollection.updateOne(filter, updateQuery), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteInsertOneResult>> addBoulderToMongo(BoulderItem item) {
        assert item != null;
        String errorMsg = "Failed to add boulder for wall " + item.getWall_id();
        return new MongoHelper<RemoteInsertOneResult>()
                .runMongoTask(boulderCollection.insertOne(item), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteBouldersFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete boulders for wall " + wall_id;
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(boulderCollection.deleteMany(filter), MongoTask.DELETE, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteBoulderFromMongo(String wall_id, String boulder_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete workouts for wall " + wall_id;
        Bson filter = Filters.and(Filters.eq(MongoConverters.Fields.WALL_ID, wall_id),
                Filters.eq(MongoConverters.Fields.BOULDER_ID, boulder_id));
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(boulderCollection.deleteOne(filter), MongoTask.DELETE, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteAllUserBouldersFromMongo(String user_id) {
        assert user_id != null;
        String errorMsg = "Failed to delete all boulders";
        Document filter = new Document(MongoConverters.Fields.USER_ID, user_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(boulderCollection.deleteMany(filter), MongoTask.DELETE, errorMsg);
    }

    /*------------------------------------HANDLE WORKOUTS-----------------------------------------*/

    public static LiveData<Result<List<WorkoutItem>>> getWorkoutsFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load workouts for wall %s", wall_id);
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        List<WorkoutItem> items = new ArrayList<>();
        return new MongoHelper<List<WorkoutItem>>()
                .runMongoTask(workoutCollection.find(filter).into(items), MongoTask.GET, errorMsg);
    }

    public static LiveData<Result<RemoteInsertOneResult>> addWorkoutToMongo(WorkoutItem item) {
        assert item != null;
        String errorMsg = "Failed to add workouts for wall " + item.getWall_id();
        return new MongoHelper<RemoteInsertOneResult>()
                .runMongoTask(workoutCollection.insertOne(item), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteUpdateResult>> editWorkoutInMongo(WorkoutItem item) {
        assert item != null;
        String errorMsg = "Failed to add workout for wall " + item.getWall_id();
        Bson filter = Filters.and(Filters.eq(MongoConverters.Fields.WALL_ID, item.getWall_id()),
                Filters.eq(MongoConverters.Fields.WORKOUT_ID, item.getWorkout_id()));
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append("$set", new BasicDBObject()
                .append(MongoConverters.Fields.WORKOUT_ID, item.getWorkout_name())
                .append(MongoConverters.Fields.WORKOUT_SETS, item.setsToBson()));
        return new MongoHelper<RemoteUpdateResult>()
                .runMongoTask(workoutCollection.updateOne(filter, updateQuery), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteWorkoutsFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete workouts for wall " + wall_id;
        Document filter = new Document(MongoConverters.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(workoutCollection.deleteMany(filter), MongoTask.DELETE, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteWorkoutFromMongo(String wall_id, String workout_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete workouts for wall " + wall_id;
        Bson filter = Filters.and(Filters.eq(MongoConverters.Fields.WALL_ID, wall_id),
                Filters.eq(MongoConverters.Fields.WORKOUT_ID, workout_id));
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(workoutCollection.deleteOne(filter), MongoTask.DELETE, errorMsg);
    }

    /*----------------------------------HANDLE AUTHENTICATION-------------------------------------*/

    public static LiveData<Result<StitchUser>> loginToMongo(String username, String appId) {
        // MongoDB variables
        StitchAppClient client = Stitch.initializeDefaultAppClient(appId);
        RemoteMongoClient mongoClient = client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

        // Initialize wall collection
        wallCollection = mongoClient.getDatabase(DATABASE)
                .getCollection(WALL_ITEMS_COLLECTION, WallDataItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(MongoConverters.wallDataItemCodec)));

        // Initialize wall image collection
        wallImageCollection = mongoClient.getDatabase(DATABASE)
                .getCollection(WALL_IMAGE_COLLECTION, WallImageItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(MongoConverters.wallImageCodec)));

        // Initialize boulder collection
        boulderCollection = mongoClient.getDatabase(DATABASE)
                .getCollection(BOULDER_ITEMS_COLLECTION, BoulderItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(MongoConverters.boulderItemCodec)));

        // Initialize workout collection
        workoutCollection = mongoClient.getDatabase(DATABASE)
                .getCollection(WORKOUT_COLLECTION, WorkoutItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(MongoConverters.workoutItemCodec)));

        // Authenticate with MongoDB
        Document usernameAuth = new Document("username", username);
        FunctionCredential credential = new FunctionCredential(usernameAuth);
        String errorMsg = "Unable to login with username " + username;
        return new MongoHelper<StitchUser>()
            .runMongoTask(client.getAuth().loginWithCredential(credential), MongoTask.GET, errorMsg);
    }
}
