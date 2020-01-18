import org.neo4j.driver.v1.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Neo4jDAO implements AutoCloseable
{
    private final Driver driver;
    private static Neo4jDAO instance;

    private Neo4jDAO() {
        driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic( "bot", "SyugarDaddy4j"));
    }

    static Neo4jDAO getInstance() {
        if(instance == null) {
            instance = new Neo4jDAO();
        }
        return instance;
    }

    @Override
    public void close() {
        driver.close();
    }

    void addNode(String id, List<String> collections) {
        StringBuilder args = new StringBuilder(id);
        for (String c : collections) {
            args.append(":").append(c);
        }
        runRequest("MERGE (" + args + "{name:'" + id + "'})");
    }

    private StatementResult runRequest(String request) {
        try (Session session = driver.session()) {
            return session.run(request);
        }
    }

    void addRecipe(long userId, String recipeId, List<String> ingredients, List<String> tools,
                   List<String> subCategories) {
        List<String> collections = new LinkedList<>();
        collections.add("Recipe");
        collections.addAll(subCategories);
        addNode("_" + recipeId, collections);
        for (String ingredient : ingredients) {
            String[] ingredientXquantite = ingredient.split("/");
            addNode(ingredientXquantite[0], Collections.singletonList("Ingredient"));
            runRequest("MATCH (ing: Ingredient{name: '" + ingredientXquantite[0] + "' })," +
                    "(rcp:Recipe{ name:'_" + recipeId +"'})\n" +
                    "CREATE (ing)-[:IN{quantite:'"+
                    ingredientXquantite[1] + "'}]->(rcp)");
        }

        for (String tool : tools) {
            tool = tool.replaceAll("-", " ");
            addNode(tool, Collections.singletonList("Ustensile"));
            runRequest("MATCH (ust: Ustensile{name: '" + tool + "' })," +
                    "(rcp:Recipe{ name:'_" + recipeId +"'})\n" +
                    "CREATE (ust)-[:USEFULL]->(rcp)");
        }
        addNode("_" + userId, Collections.singletonList("User"));
        runRequest("MATCH (usr: User{name: '_" + userId + "' })," +
                "(rcp:Recipe{ name:'_" + recipeId +"'})\n" +
                "CREATE (usr)-[:PROPOSED{date:datetime()}]->(rcp)");
    }

    void addLike(List<String> liked) {
        String user = "_" + liked.get(0);
        addNode(user, Collections.singletonList("User"));
        for (int i = 1; i < liked.size(); ++i) {
            runRequest("MATCH (usr: User{name: '" + user + "' })," +
                    "(like{ name:'" + liked.get(i) + "'})\n" +
                    "CREATE (usr)-[:LIKE{date:datetime()}]->(like)");
        }
    }

    StatementResult getRecipesByIngredients(List<String> ingredients) {
        StringBuilder requestBegin = new StringBuilder();
        StringBuilder requestEnd = new StringBuilder("WHERE ");
        for (String ingredient : ingredients) {
            requestBegin.append("MATCH (").append(ingredient).append(":Ingredient)-[:IN]->(r:Recipe)\n");
            requestEnd.append(" ").append(ingredient).append(".name = '").append(ingredient).append("' AND");
        }
        String request = requestBegin + requestEnd.substring(0, requestEnd.length() -3) + "\nRETURN r.name\n";
        return runRequest(request);
    }

    StatementResult getRecipesByTools(List<String> tools) {
        StringBuilder requestBegin = new StringBuilder();
        StringBuilder requestEnd = new StringBuilder("WHERE ");
        for (String tool : tools) {
            requestBegin.append("MATCH (").append(tool).append(":Ustensile)-[:USEFULL]->(r:Recipe)\n");
            requestEnd.append(" ").append(tool).append(".name = '").append(tool).append("' AND");
        }
        String request = requestBegin + requestEnd.substring(0, requestEnd.length() -3) + "\nRETURN r.name\n";
        return runRequest(request);
    }

    public StatementResult getRecipesByUser(String user) {
        return runRequest("MATCH (u:User)-[:PROPOSED]->(r:Recipe) WHERE u.name = '_" + user + "' RETURN r.name;");
    }

    StatementResult getRecipeParts(String recipeId, String relation, String other) {
        return runRequest("MATCH (zeug)-[rel:" + relation + "]->(r:Recipe) WHERE r.name = '_" + recipeId +
                "' RETURN zeug.name" + other);
    }

    public StatementResult getSimilarUsers(String recipeId){
        StringBuilder request = new StringBuilder("MATCH(r:Recipe) WHERE r.name = '_" + recipeId + "' \n" +
                "RETURN labels(r)");
        StatementResult labels = runRequest(request.toString( ));
        request = new StringBuilder("MATCH(r:Recipe) WHERE r.name = '_" + recipeId + "' WITH r\n" +
                "MATCH (i:Ingredient)-[:IN]->(r)\n" +
                "WITH i, r\n" +
                "MATCH (i)-[:IN]->(r2:Recipe) WHERE ");
        while (labels.hasNext()) {
            Record record = labels.next();
            Iterable<Value> allLabels = record.values().get(0).values();
            for(Value label : allLabels){
                if(!label.toString().replaceAll("\"", "").equals("Recipe")){
                    request.append("r2:").append(label.toString( ).replaceAll("\"", "")).append(" OR ");
                }
            }
        }
        request = new StringBuilder(request.substring(0, request.length( ) - 3));
        request.append(" \nWITH r2 \n" + "MATCH (u:User)-[:PROPOSED]->(r2) \n" + "RETURN DISTINCT u.name");
        return runRequest(request.toString( ));
    }

    public StatementResult getRecommandation(long userId){
        String request = "MATCH(usr:User) WHERE usr.name = '_" + userId + "' \n" +
                "WITH usr\n" +
                "MATCH(usr)-[:LIKE]->(u:User) \n" +
                "WITH usr, u \n" +
                "MATCH (u)-[:PROPOSED]->(result:Recipe) WHERE NOT (usr)-[:LIKE]->(result)\n" +
                "AND NOT (usr) = (u)\n" +
                "RETURN DISTINCT result.name\n" +
                "UNION MATCH(usr:User) WHERE usr.name = '_" + userId + "' \n" +
                "WITH usr\n" +
                "MATCH(usr)-[:LIKE]->(mitm:User)-[:LIKE]->(u:User) \n" +
                "WITH usr, u \n" +
                "MATCH (usr)-[:LIKE]->(i:Ingredient) \n" +
                "WITH usr, u, i \n" +
                "MATCH (u)-[:PROPOSED]->(result:Recipe)<-[:IN]-(i) WHERE  NOT (usr)-[:LIKE]->(result)\n" +
                "AND NOT (usr) = (u)\n" +
                "RETURN DISTINCT result.name\n";
        return runRequest(request);
    }
}