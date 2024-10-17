import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DataLoaderExample {

    public static void main(String[] args) {
        // Step 1: Define your GraphQL schema
        String schemaDefinition = "type Query { user(id: ID!): User } type User { id: ID!, name: String!, email: String! }";

        // Step 2: Parse the schema and create a GraphQLSchema
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schemaDefinition);
        RuntimeWiring wiring = buildRuntimeWiring();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);

        // Step 3: Create a GraphQL instance
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();

        // Step 4: Execute GraphQL queries
        String query = "{ user(id: \"1\") { id, name, email } }";
        System.out.println(graphQL.execute(query).toSpecification());
    }

    private static RuntimeWiring buildRuntimeWiring() {
        // Step 5: Create a DataLoader for users
        LoadingCache<String, User> userDataLoader = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public User load(String key) throws Exception {
                        // Simulate database query to fetch user data
                        return fetchUserFromDatabase(key);
                    }
                });

        // Step 6: Define the DataFetcher for the 'user' field
        DataFetcher userDataFetcher = environment -> {
            String userId = environment.getArgument("id");
            try {
                // Use the DataLoader to fetch user data efficiently
                return userDataLoader.get(userId);
            } catch (ExecutionException e) {
                throw new RuntimeException("Error fetching user data", e);
            }
        };

        // Step 7: Wire the DataFetcher to the 'user' field in the schema
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("user", userDataFetcher))
                .build();
    }

    private static User fetchUserFromDatabase(String userId) {
        // Simulate database query to fetch user data
        // In a real application, use an ORM or database client to perform the query
        switch (userId) {
            case "1":
                return new User("1", "Alice", "alice@example.com");
            case "2":
                return new User("2", "Bob", "bob@example.com");
            default:
                return null;
        }
    }

    static class User {
        private final String id;
        private final String name;
        private final String email;

        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
