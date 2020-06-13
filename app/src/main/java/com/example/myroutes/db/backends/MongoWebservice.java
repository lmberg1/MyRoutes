package com.example.myroutes.db.backends;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myroutes.R;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.Result;
import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.mongoClasses.WallDataItem;
import com.example.myroutes.db.mongoClasses.WallImageItem;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.function.FunctionCredential;
import com.mongodb.stitch.core.internal.common.BsonUtils;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;

import java.util.ArrayList;
import java.util.List;

import static com.example.myroutes.db.mongoClasses.BoulderItem.BOULDER_DATABASE;
import static com.example.myroutes.db.mongoClasses.BoulderItem.BOULDER_ITEMS_COLLECTION;
import static com.example.myroutes.db.mongoClasses.WallDataItem.WALL_DATABASE;
import static com.example.myroutes.db.mongoClasses.WallDataItem.WALL_ITEMS_COLLECTION;
import static com.example.myroutes.db.mongoClasses.WallImageItem.WALL_IMAGE_COLLECTION;
import static com.example.myroutes.db.mongoClasses.WallImageItem.WALL_IMAGE_DATABASE;

public class MongoWebservice {
    private static final String TAG = "MongoWebService";

    private enum MongoTask {ADD, GET, DELETE};

    private static RemoteMongoCollection<WallDataItem> wallCollection;
    private static RemoteMongoCollection<WallImageItem> wallImageCollection;
    private static RemoteMongoCollection<BoulderItem> boulderCollection;

    // Helper class to run a mongo task
    private static class MongoHelper<T> {
        private LiveData<Result<T>> runMongoTask(Task<T> task, MongoTask taskType, String errorMsg) {
            final MutableLiveData<Result<T>> mongoResult = new MutableLiveData<>();
            task.addOnSuccessListener(item -> {
                if ((item == null) && (taskType == MongoTask.GET)) {
                    mongoResult.setValue(new Result.Error<>(SharedViewModel.Status.NOT_FOUND));
                } else {
                    mongoResult.setValue(new Result.Success<>(item));
                }
            })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, errorMsg, e);
                        mongoResult.setValue(new Result.Error<>(SharedViewModel.Status.FAILURE));
                    });
            return mongoResult;
        }
    }

    /*-------------------------------------HANDLE IMAGES------------------------------------------*/

    public static LiveData<Result<WallImageItem>> getImageFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load image for wall %s", wall_id);
        Document filter = new Document(WallImageItem.Fields.WALL_ID, wall_id);
        return new MongoHelper<WallImageItem>()
                .runMongoTask(wallImageCollection.find(filter).first(), MongoTask.GET, errorMsg);
    }

    public static LiveData<Result<RemoteInsertOneResult>> addImageToMongo(WallImageItem item) {
        assert item != null;
        String errorMsg = "Failed to add image for wall " + item.getWall_id();
        return new MongoHelper<RemoteInsertOneResult>()
                .runMongoTask(wallImageCollection.insertOne(item), MongoTask.ADD, errorMsg);
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteImageFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = "Failed to delete image for wall " + wall_id;
        Document filter = new Document(WallImageItem.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(wallImageCollection.deleteOne(filter), MongoTask.DELETE, errorMsg);
    }

    /*------------------------------------HANDLE WALL DATA----------------------------------------*/

    public static LiveData<Result<WallDataItem>> getDataFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load data for wall %s", wall_id);
        Document filter = new Document(WallImageItem.Fields.WALL_ID, wall_id);
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
        Document filter = new Document(WallImageItem.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(wallCollection.deleteOne(filter), MongoTask.DELETE, errorMsg);
    }

    /*------------------------------------HANDLE BOULDERS-----------------------------------------*/

    public static LiveData<Result<List<BoulderItem>>> getBouldersFromMongo(String wall_id) {
        assert wall_id != null;
        String errorMsg = String.format("Failed to load boulders for wall %s", wall_id);
        Document filter = new Document(WallImageItem.Fields.WALL_ID, wall_id);
        List<BoulderItem> items = new ArrayList<>();
        return new MongoHelper<List<BoulderItem>>()
                .runMongoTask(boulderCollection.find(filter).into(items), MongoTask.GET, errorMsg);
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
        Document filter = new Document(WallImageItem.Fields.WALL_ID, wall_id);
        return new MongoHelper<RemoteDeleteResult>()
                .runMongoTask(wallImageCollection.deleteMany(filter), MongoTask.DELETE, errorMsg);
    }

    /*----------------------------------HANDLE AUTHENTICATION-------------------------------------*/

    public static LiveData<Result<StitchUser>> loginToMongo(String username, String appId) {
        // MongoDB variables
        StitchAppClient client = Stitch.initializeDefaultAppClient(appId);
        RemoteMongoClient mongoClient = client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

        // Initialize wall collection
        wallCollection = mongoClient.getDatabase(WALL_DATABASE)
                .getCollection(WALL_ITEMS_COLLECTION, WallDataItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(WallDataItem.codec)));

        // Initialize wall image collection
        wallImageCollection = mongoClient.getDatabase(WALL_IMAGE_DATABASE)
                .getCollection(WALL_IMAGE_COLLECTION, WallImageItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(WallImageItem.codec)));

        // Initialize boulder collection
        boulderCollection = mongoClient.getDatabase(BOULDER_DATABASE)
                .getCollection(BOULDER_ITEMS_COLLECTION, BoulderItem.class)
                .withCodecRegistry(CodecRegistries.fromRegistries(
                        BsonUtils.DEFAULT_CODEC_REGISTRY,
                        CodecRegistries.fromCodecs(BoulderItem.codec)));

        // Authenticate with MongoDB
        Document usernameAuth = new Document("username", username);
        FunctionCredential credential = new FunctionCredential(usernameAuth);
        String errorMsg = "Unable to login with username " + username;
        return new MongoHelper<StitchUser>()
            .runMongoTask(client.getAuth().loginWithCredential(credential), MongoTask.GET, errorMsg);
    }
}
