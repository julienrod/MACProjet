import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBDAO {
    private static MongoDBDAO instance;
    private MongoClientURI connectionString;
    private MongoClient mongoclient;
    private MongoDatabase getDatabase(String database){
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(connectionString);
        return mongoClient.getDatabase(database);
    }
    private MongoDBDAO(){}
    public static MongoDBDAO getInstance() {
        if(instance == null) {
            instance = new MongoDBDAO();
        }
        return instance;
    }
    public void check(String firstName, String lastName, int userId, String username) {
        MongoDatabase database = getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("user");

        long found = collection.count(Document.parse("{id : " + Integer.toString(userId) + "}"));
        if (found == 0) {
            Document doc = new Document("first_name", firstName)
                    .append("last_name", lastName)
                    .append("id", userId)
                    .append("username", username);
            collection.insertOne(doc);
            this.mongoclient.close();
            List<String> collections = Arrays.asList("User");
            Neo4jDAO.getInstance().addNode("_" + userId, collections);
            System.out.println("User doesn't exist in database. Written.");
        } else {
            System.out.println("User already exists in database.");
            this.mongoclient.close();
        }
    }

    public ObjectId addRecipe(String recipeName, String recipeDescription, int time, int kcal) {
        MongoDatabase database = getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        Document doc = new Document("name", recipeName)
                .append("description", recipeDescription)
                .append("time", time)
                .append("kcal", kcal);
        collection.insertOne(doc);
        ObjectId id = (ObjectId)doc.get( "_id" );
        mongoclient.close();
        return id;
    }

    public Document findDocument(String id){
        MongoDatabase database = mongoclient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        Document doc = collection.find(eq("_id", new ObjectId(id))).first();
        mongoclient.close();
        return doc;
    }

    public Document findUser(String id){
        MongoDatabase database = getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("user");
        Document doc = collection.find(eq("id", Integer.parseInt(id))).first();
        mongoclient.close();
        return doc;
    }

    public FindIterable<Document> findDocumentByTime(int time){
        MongoDatabase database = getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("user");
        int min = time -5;
        int max = time +5;
        FindIterable<Document> docs = collection.find(Filters.and(Filters.gte("time",
                min), Filters.lte("time",  max)));
        mongoclient.close();
        return docs;
    }

    public Document getRandomRecipe(){
        MongoDatabase database = getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        Document doc = collection.aggregate(Arrays.asList(Aggregates.sample(1))).first();
        mongoclient.close();
        return doc;
    }
}
