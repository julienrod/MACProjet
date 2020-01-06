import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;

import java.sql.ResultSet;
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

    public void addReceipe(long user_id, String receipe_id, List<String> ingredients, List<String> ustenciles,
                           List<String> subCategories) {
        List<String> collections = new LinkedList<>();
        collections.add("Receipe");
        for(String sub : subCategories) {
            collections.add(sub);
        }
        addNode("_" + receipe_id, collections);
        for(String ingredient : ingredients) {
            String[] ingredientXquantite = ingredient.split("/");
            addNode(ingredientXquantite[0], Arrays.asList("Ingredient"));
            runRequest("MATCH (ing: Ingredient{name: '" + ingredientXquantite[0] + "' })," +
                    "(rcp:Receipe{ name:'_" + receipe_id +"'})\n" +
                    "CREATE (ing)-[:IN{quantite:'"+
                    ingredientXquantite[1] + "'}]->(rcp)");
        }

        for(String ustencile : ustenciles) {
            addNode(ustencile, Arrays.asList("Ustencile"));
            runRequest("MATCH (ust: Ustencile{name: '" + ustencile + "' })," +
                    "(rcp:Receipe{ name:'_" + receipe_id +"'})\n" +
                    "CREATE (ust)-[:USEFULL]->(rcp)");
        }
        runRequest("MATCH (usr: User{name: '_" + user_id + "' })," +
                "(rcp:Receipe{ name:'_" + receipe_id +"'})\n" +
                "CREATE (usr)-[:PROPOSED{date:datetime()}]->(rcp)");
    }
}