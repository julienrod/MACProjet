import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBDAO {
    private static MongoDBDAO instance;
    private MongoDBDAO(){}
    public static MongoDBDAO getInstance(){
        if(instance == null){
            instance = new MongoDBDAO();
        }
        return instance;
    }
    public void check(String first_name, String last_name, int user_id, String username) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("user");
        long found = collection.count(Document.parse("{id : " + Integer.toString(user_id) + "}"));
        if (found == 0) {
            Document doc = new Document("first_name", first_name)
                    .append("last_name", last_name)
                    .append("id", user_id)
                    .append("username", username);
            collection.insertOne(doc);
            mongoClient.close();
            List<String> collections = Arrays.asList("User");
            Neo4jDAO.getInstance().addNode("_" + user_id, collections);
            System.out.println("User not exists in database. Written.");
        } else {
            System.out.println("User exists in database.");
            mongoClient.close();
        }
    }

    public ObjectId addreceipe(String receipeName, String receipeDescription, String time, String kcal){
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("receipe");
        Document doc = new Document("name", receipeName)
                .append("description", receipeDescription)
                .append("time", time)
                .append("kcal", kcal);
        collection.insertOne(doc);
        ObjectId id = (ObjectId)doc.get( "_id" );
        mongoClient.close();
        return id;
    }

    public Document findDocument(String id){
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("receipe");
        return collection.find(eq("_id", new ObjectId(id))).first();
    }
}
