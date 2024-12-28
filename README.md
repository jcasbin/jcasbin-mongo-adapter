jcasbin-mongodb-adapter
===
[![codebeat badge](https://codebeat.co/badges/df265133-60a0-4ee7-b57d-25fd27273905)](https://codebeat.co/projects/github-com-jcasbin-mongo-adapter-master)
[![build](https://github.com/jcasbin/jcasbin-mongo-adapter/actions/workflows/maven-ci.yml/badge.svg)](https://github.com/jcasbin/jcasbin-mongo-adapter/actions/workflows/maven-ci.yml)
[![codecov](https://codecov.io/gh/casbin/jcasbin-mongo-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/casbin/jcasbin-mongo-adapter)
[![javadoc](https://javadoc.io/badge2/org.casbin/jcasbin-mongo-adapter/javadoc.svg)](https://javadoc.io/doc/org.casbin/jcasbin-mongo-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/org.casbin/jcasbin-mongo-adapter.svg)](https://mvnrepository.com/artifact/org.casbin/jcasbin-mongo-adapter/latest)
[![Discord](https://img.shields.io/discord/1022748306096537660?logo=discord&label=discord&color=5865F2)](https://discord.gg/S5UjpzGZjN)

jcasbin-mongodb-adapter is the [MongoDB](https://github.com/mongodb/mongo) adapter for [jCasbin](https://github.com/casbin/jcasbin). With this library, jCasbin can load policy from a MongoDB database or save policy to it.

## Installation

For Maven:

```xml
<dependency>
    <groupId>org.casbin</groupId>
    <artifactId>jcasbin-mongo-adapter</artifactId>
    <version>LATEST</version>
</dependency>
```

optional: If your project already has a mongodb driver, please ignore it.
```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>${mongodb.version}</version>
</dependency>
```

## Simple Example

```java
package org.casbin.adapter;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.Adapter;
import org.jim.jcasbin.MongoAdapter;

import java.util.ArrayList;

public class Test {
    public static void main(String[] args) {
        // Use MongoDB connection string and database name to initialize a MongoAdapter.
        String connectionString = "mongodb://localhost:27017/";
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase("casbin");

        // Check and create a collection
        if (!database.listCollectionNames().into(new ArrayList<>()).contains("casbin_rule")) {
            database.createCollection("casbin_rule");
        }

        Adapter adapter = new MongoAdapter(mongoClient, "casbin");
        Enforcer enforcer = new Enforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");

        // Check the permission.
        enforcer.enforce("alice", "data1", "read");

        // Modify the policy.
        // enforcer.addPolicy(...);
        // enforcer.removePolicy(...);

        // Save the policy back to DB.
        adapter.savePolicy(enforcer.getModel());

        // Close the MongoDB client.
        mongoClient.close();
    }
}

```

## Getting Help

- [jCasbin](https://github.com/casbin/jcasbin)

## License

This project is under Apache 2.0 License. See the [LICENSE](LICENSE) file for the full license text.