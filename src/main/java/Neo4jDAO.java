import org.neo4j.driver.v1.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Neo4jDAO implements AutoCloseable
{
    private final Driver driver;
    private static Neo4jDAO instance;

    private Neo4jDAO()
    {
        driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic( "bot", "SyugarDaddy4j"));
    }

    public static Neo4jDAO getInstance()
    {
        if(instance == null){
            instance = new Neo4jDAO();
        }
        return instance;
    }

    @Override
    public void close() throws Exception
    {
        driver.close();
    }

    public StatementResult addNode(String id, List<String> collections)
    {
        String args = id;
        for(String c : collections)
        {
            args += ":" + c;
        }
        StatementResult result = runRequest("MERGE ("+ args +"{name:'"+ id + "'})");
        return result;
    }

    public StatementResult runRequest(String request)
    {
        try ( Session session = driver.session() )
        {
            return session.run(request);
        }
    }

    public void addRecipe(long userId, String recipeId, List<String> ingredients, List<String> ustenciles,
                           List<String> subCategories) {
        List<String> collections = new LinkedList<>();
        collections.add("Recipe");
        for(String sub : subCategories) {
            collections.add(sub);
        }
        addNode("_" + recipeId, collections);
        for(String ingredient : ingredients) {
            String[] ingredientXquantite = ingredient.split("/");
            addNode(ingredientXquantite[0], Arrays.asList("Ingredient"));
            runRequest("MATCH (ing: Ingredient{name: '" + ingredientXquantite[0] + "' })," +
                    "(rcp:Recipe{ name:'_" + recipeId +"'})\n" +
                    "CREATE (ing)-[:IN{quantite:'"+
                    ingredientXquantite[1] + "'}]->(rcp)");
        }

        for(String ustencile : ustenciles) {
            addNode(ustencile, Arrays.asList("Ustencile"));
            runRequest("MATCH (ust: Ustencile{name: '" + ustencile + "' })," +
                    "(rcp:Recipe{ name:'_" + recipeId +"'})\n" +
                    "CREATE (ust)-[:USEFULL]->(rcp)");
        }
        addNode("_" + userId, Arrays.asList("User"));
        runRequest("MATCH (usr: User{name: '_" + userId + "' })," +
                "(rcp:Recipe{ name:'_" + recipeId +"'})\n" +
                "CREATE (usr)-[:PROPOSED{date:datetime()}]->(rcp)");
    }

    public void addLike(List<String> liked){
        String user = "_" + liked.get(0);
        addNode(user, Arrays.asList("User"));
        for(int i = 1; i < liked.size(); ++i){
            runRequest("MATCH (usr: User{name: '" + user + "' })," +
                    "(like{ name:'" + liked.get(i) + "'})\n" +
                    "CREATE (usr)-[:LIKE{date:datetime()}]->(like)");
        }

    }

    public StatementResult getRecipesByIngredients(List<String> ingredients){
        String requestBegin = "";
        String requestEnd = "WHERE ";
        for(String ingredient : ingredients){
            requestBegin += "MATCH (" + ingredient + ":Ingredient)-[:IN]->(r:Recipe)\n";
            requestEnd += " " + ingredient +  ".name = '" + ingredient + "' AND";
        }
        String request = requestBegin + requestEnd.substring(0, requestEnd.length() -3) + "\nRETURN r.name\n";
        return runRequest(request);
    }

    public StatementResult getRecipesByTools(List<String> tools){
        String requestBegin = "";
        String requestEnd = "WHERE ";
        for(String tool : tools){
            requestBegin += "MATCH (" + tool + ":Ustencile)-[:IN]->(r:Recipe)\n";
            requestEnd += " " + tool +  ".name = '" + tool + "' AND";
        }
        String request = requestBegin + requestEnd.substring(0, requestEnd.length() -3) + "\nRETURN r.name\n";
        return runRequest(request);
    }

    public StatementResult getRecipesByCalories(String calories){
        /*
        String request = "MATCH (" + calories + ":Calories)-[:IN]->(r:Recipe) WHERE r.calories <= " + calories + "RETURN r.name\n";
        return runRequest(request);
        */
        return null;
    }

    public StatementResult getRecipeParts(String recipeId, String relation, String autreparam){
        return runRequest("MATCH (zeug)-[rel:" + relation + "]->(r:Recipe) WHERE r.name = '_" + recipeId +
                "' RETURN zeug.name" + autreparam);
    }

    public StatementResult getRecipesByUser(String user) {
        //TODO
        return null;
    }

    public StatementResult getRecipesByTime(String time) {
        //TODO
        return null;
    }

    public StatementResult getSimilarUsers(String recipeId){
        //TODO Controller si cette requête marche bel et bien avec une base de données plus fournie
        String request = "MATCH(r:Recipe) WHERE r.name = '_"+ recipeId+ "' WITH r\n" +
                "MATCH (i:Ingredient)-[:IN]->(r)\n" +
                "WITH i, r\n" +
                "MATCH (i)-[:IN]->(r2:Recipe) WHERE labels(r) = labels(r2) \n" +
                "WITH r2 \n" +
                "MATCH (u:User)-[:PROPOSED]->(r2) \n" +
                "RETURN u.name;";
        return runRequest(request);
    }
}