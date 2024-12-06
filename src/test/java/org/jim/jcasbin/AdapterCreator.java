package org.jim.jcasbin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.V6_0;

/**
 * Created with IntelliJ IDEA.
 *
 * @author JimZhang
 * @since 2021/3/24
 * Description:
 */
public interface AdapterCreator {
    MongoAdapter create() throws Exception;

    void close();

    class MongoAdapterCreator implements AdapterCreator, AutoCloseable {
        MongoClient mongoClient = null;

        @Override
        public MongoAdapter create() {
            TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().start(V6_0);
            ServerAddress serverAddress = running.current().getServerAddress();
            MongoClient mongoClient = MongoClients.create("mongodb://" + serverAddress);
            return new MongoAdapter(mongoClient, "zhangji");
        }

        @Override
        public void close() {
            if (mongoClient != null) {
                mongoClient.close();
            }
        }
    }
}
