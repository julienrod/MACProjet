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
    private MongoClient mongoclient;
    private MongoDatabase getDatabase(){
        MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
        mongoclient = new MongoClient(connectionString);
        return mongoclient.getDatabase("syugardaddy");
    }
    private MongoDBDAO(){}
    public static MongoDBDAO getInstance() {
        if(instance == null) {
            instance = new MongoDBDAO();
        }
        return instance;
    }
    public void check(String firstName, String lastName, int userId, String username) {
        MongoDatabase database = getDatabase( );
        MongoCollection<Document> collection = database.getCollection("user");

        long found = collection.count(Document.parse("{id : " + Integer.toString(userId) + "}"));
        if (found == 0) {
            Document doc = new Document("first_name", firstName)
                    .append("last_name", lastName)
                    .append("id", userId)
                    .append("username", username);
            collection.insertOne(doc);
            //this.mongoclient.close();
            List<String> collections = Arrays.asList("User");
            Neo4jDAO.getInstance().addNode("_" + userId, collections);
            System.out.println("User doesn't exist in database. Written.");
        } else {
            System.out.println("User already exists in database.");
            //this.mongoclient.close();
        }
    }

    public ObjectId addRecipe(String recipeName, String recipeDescription, int time, int kcal) {
        MongoDatabase database = getDatabase( );
        MongoCollection<Document> collection = database.getCollection("recipe");
        Document doc = new Document("name", recipeName)
                .append("description", recipeDescription)
                .append("time", time)
                .append("kcal", kcal);
        collection.insertOne(doc);
        return (ObjectId)doc.get( "_id" );
    }

    public Document findRecipe(String id){
        MongoDatabase database = mongoclient.getDatabase("syugardaddy");
        MongoCollection<Document> collection = database.getCollection("recipe");
        return collection.find(eq("_id", new ObjectId(id))).first();
    }

    public Document findUser(String id){
        MongoDatabase database = getDatabase( );
        MongoCollection<Document> collection = database.getCollection("user");
        return collection.find(eq("id", Integer.parseInt(id))).first();
    }

    public FindIterable<Document> findDocumentByTime(int time){
        MongoDatabase database = getDatabase( );
        MongoCollection<Document> collection = database.getCollection("recipe");
        int min = time -5;
        int max = time +5;
        return collection.find(Filters.and(Filters.gte("time",
                min), Filters.lte("time",  max)));
    }

    public FindIterable<Document> findDocumentByCalories(int calories){
        MongoDatabase database = getDatabase( );
        MongoCollection<Document> collection = database.getCollection("recipe");
        return collection.find(Filters.and(Filters.gte("kcal",
                calories - 50), Filters.lte("kcal",  calories)));
    }

    public Document getRandomRecipe(){
        MongoDatabase database = getDatabase( );
        MongoCollection<Document> collection = database.getCollection("recipe");
        return collection.aggregate(Arrays.asList(Aggregates.sample(1))).first();
    }
}
