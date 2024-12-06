package org.jim.jcasbin;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author JimZhang
 * @since 2021/3/24
 * Description:
 */
public class MongoAdapterTest {
    private void testAdapter(List<MongoAdapter> adapters) {
        for (MongoAdapter a : adapters) {
            MongoAdapterTestSets.testAdapter(a);
            MongoAdapterTestSets.testAddAndRemovePolicy(a);
            MongoAdapterTestSets.testBatchAddAndRemovePolicies(a);
        }
    }

    @Test
    public void testMongoAdapter() {
        List<MongoAdapter> adapters = new ArrayList<>();

        try (AdapterCreator.MongoAdapterCreator creator = new AdapterCreator.MongoAdapterCreator()) {
            adapters.add(creator.create());
            testAdapter(adapters);
        }
    }
}