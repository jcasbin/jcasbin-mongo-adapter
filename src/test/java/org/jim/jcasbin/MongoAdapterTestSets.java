package org.jim.jcasbin;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 *
 * @author JimZhang
 * @since 2021/3/24
 * Description:
 */
public class MongoAdapterTestSets {
    private static void testEnforce(Enforcer e, String sub, Object obj, String act, boolean res) {
        assertEquals(res, e.enforce(sub, obj, act));
    }

    public static void testGetPolicy(Enforcer e, List<List<String>> res) {
        List<List<String>> myRes = e.getPolicy();
        Util.logPrint("Policy: " + myRes);
        res.sort(Comparator.comparing(l -> String.join("", l)));
        myRes.sort(Comparator.comparing(l -> String.join("", l)));
        if (!Util.array2DEquals(res, myRes)) {
            fail("Policy: " + myRes + ", supposed to be " + res);
        }
    }

    public static void testHasPolicy(Enforcer e, List<String> policy, boolean res) {
        boolean myRes = e.hasPolicy(policy);
        Util.logPrint("Has policy " + Util.arrayToString(policy) + ": " + myRes);

        if (res != myRes) {
            fail("Has policy " + Util.arrayToString(policy) + ": " + myRes + ", supposed to be " + res);
        }
    }

    static void testAdapter(Adapter a) {
        // Because the DB is empty at first,
        // so we need to load the policy from the file adapter (.CSV) first.
        Enforcer e = new Enforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");

        // This is a trick to save the current policy to the DB.
        // We can't call e.savePolicy() because the adapter in the enforcer is still the file adapter.
        // The current policy means the policy in the jCasbin enforcer (aka in memory).
        a.savePolicy(e.getModel());

        // Clear the current policy.
        e.clearPolicy();
        testGetPolicy(e, Collections.emptyList());

        // Load the policy from DB.
        a.loadPolicy(e.getModel());
        testGetPolicy(e, asList(
                asList("alice", "data1", "read"),
                asList("bob", "data2", "write"),
                asList("data2_admin", "data2", "read"),
                asList("data2_admin", "data2", "write")));

        // Note: you don't need to look at the above code
        // if you already have a working DB with policy inside.
        e = new Enforcer("examples/rbac_model.conf", a);
        testGetPolicy(e, asList(
                asList("alice", "data1", "read"),
                asList("bob", "data2", "write"),
                asList("data2_admin", "data2", "read"),
                asList("data2_admin", "data2", "write")));
    }

    static void testAddAndRemovePolicy(Adapter a) {
        Enforcer e = new Enforcer("examples/rbac_model.conf", a);
        testEnforce(e, "cathy", "data1", "read", false);

        // AutoSave is enabled by default.
        // It can be disabled by:
        // e.enableAutoSave(false);

        // Because AutoSave is enabled, the policy change not only affects the policy in Casbin enforcer,
        // but also affects the policy in the storage.
        assertTrue(e.addPolicy("cathy", "data1", "read"));
        testEnforce(e, "cathy", "data1", "read", true);

        // Reload the policy from the storage to see the effect.
        e.clearPolicy();
        a.loadPolicy(e.getModel());
        // The policy has a new rule: {"cathy", "data1", "read"}.
        testEnforce(e, "cathy", "data1", "read", true);

        // Remove the added rule.
        assertTrue(e.removePolicy("cathy", "data1", "read"));
        testEnforce(e, "cathy", "data1", "read", false);

        // Reload the policy from the storage to see the effect.
        e.clearPolicy();
        a.loadPolicy(e.getModel());
        testEnforce(e, "cathy", "data1", "read", false);
    }

    static void testBatchAddAndRemovePolicies(MongoAdapter a) {
        Enforcer e = new Enforcer("examples/rbac_model.conf", a);
        testEnforce(e, "cathy", "data1", "read", false);
        testEnforce(e, "jane", "data2", "read", false);

        // AutoSave is enabled by default.
        // It can be disabled by:
        // e.enableAutoSave(false);

        // Because AutoSave is enabled, the policy change not only affects the policy in Casbin enforcer,
        // but also affects the policy in the storage.
        List<String> policy1 = new ArrayList<String>() {{
            add("cathy");
            add("data1");
            add("read");
        }};
        List<String> policy2 = new ArrayList<String>() {{
            add("jane");
            add("data2");
            add("read");
        }};
        List<List<String>> policies = new ArrayList<List<String>>() {{
            add(policy1);
            add(policy2);
        }};

        assertTrue(e.addPolicies(policies));
        testEnforce(e, "cathy", "data1", "read", true);
        testEnforce(e, "jane", "data2", "read", true);

        // Reload the policy from the storage to see the effect.
        e.clearPolicy();
        a.loadPolicy(e.getModel());
        // The policy has a new rule: {"cathy", "data1", "read"}.
        testEnforce(e, "cathy", "data1", "read", true);
        testEnforce(e, "jane", "data2", "read", true);

        // Remove the added rule.
        assertTrue(e.removePolicies(policies));
        testEnforce(e, "cathy", "data1", "read", false);
        testEnforce(e, "jane", "data2", "read", false);

        // Reload the policy from the storage to see the effect.
        e.clearPolicy();
        a.loadPolicy(e.getModel());
        testEnforce(e, "cathy", "data1", "read", false);
        testEnforce(e, "jane", "data2", "read", false);
    }
}
