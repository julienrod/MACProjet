import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

class MongoDBDAO {
    private static MongoDBDAO instance;
    private MongoDBDAO(){}
    static MongoDBDAO getInstance() {
        if(instance == null) {
            instance = new MongoDBDAO();
        }
        return instance;
    }
    void check(String firstName, String lastName, int userId, String username) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("user");

        long found = collection.count(Document.parse("{id : " + userId + "}"));
        if (found == 0) {
            Document doc = new Document("first_name", firstName)
                    .append("last_name", lastName)
                    .append("id", userId)
                    .append("username", username);
            collection.insertOne(doc);
            mongoClient.close();
            List<String> collections = Collections.singletonList("User");
            Neo4jDAO.getInstance().addNode("_" + userId, collections);
            System.out.println("User doesn't exist in database. Written.");
        } else {
            System.out.println("User already exists in database.");
            mongoClient.close();
        }
    }

    ObjectId addRecipe(String recipeName, String recipeDescription, String time, String kcal) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        Document doc = new Document("name", recipeName)
                .append("description", recipeDescription)
                .append("time", time)
                .append("kcal", kcal);
        collection.insertOne(doc);
        ObjectId id = (ObjectId)doc.get( "_id" );
        mongoClient.close();
        return id;
    }

    Document findDocument(String id){
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        return collection.find(eq("_id", new ObjectId(id))).first();
    }

    Document getRandomRecipe(){
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        return collection.aggregate(Collections.singletonList(Aggregates.sample(1))).first();
    }
}
