package org.jim.jcasbin;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.DeleteOneModel;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.BatchAdapter;
import org.jim.jcasbin.domain.CasbinRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * MongoAdapter is the Mongodb adapter for jCasbin.
 * It can load policy from MongoDB or save policy to it.
 *
 * @author JimZhang
 * @since 2021/3/23
 * Description:
 */

public class MongoAdapter implements BatchAdapter {
    private static final String DEFAULT_DB_NAME = "casbin";
    private static final String DEFAULT_COL_NAME = "casbin_rule";
    private static final Logger log = LoggerFactory.getLogger(MongoAdapter.class);
    private static final CodecRegistry POJO_CODEC_REGISTRY = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    private static String orDefault(String str, String defaultStr) {
        return str == null || str.trim().isEmpty() ? defaultStr : str;
    }

    private static Optional<CasbinRule> fromListRule(List<String> rules) {
        if (rules.size() != 7) {
            log.warn("list rules size [{}] do not match pojo fields", rules.size());
            return Optional.empty();
        }
        CasbinRule casbinRule = new CasbinRule();
        for (int i = 0; i < rules.size(); i++) {
            casbinRule.setByIndex(i, rules.get(i));
        }
        return Optional.of(casbinRule);
    }

    private final MongoClient mongoClient;
    private final String dbName;
    private final String colName;


    public MongoAdapter(MongoClient mongoClient, String dbName) {
        this(mongoClient, dbName, null);
    }

    public MongoAdapter(MongoClient mongoClient, String dbName, String colName) {
        this.mongoClient = mongoClient;

        this.dbName = orDefault(dbName, DEFAULT_DB_NAME);
        this.colName = orDefault(colName, DEFAULT_COL_NAME);
    }

    protected void clearCollection() {
        this.mongoClient.getDatabase(this.dbName).getCollection(this.colName).drop();
    }

    private MongoCollection<CasbinRule> getCollection() {
        return this.mongoClient
                .getDatabase(this.dbName)
                .withCodecRegistry(POJO_CODEC_REGISTRY)
                .getCollection(this.colName, CasbinRule.class);
    }

    /**
     * 从存储加载所有策略规则
     * 加载时会合并重复数据
     *
     * @param model the model.
     */
    @Override
    public void loadPolicy(Model model) {
        Map<String, ArrayList<ArrayList<String>>> policies = this.loading();
        policies.keySet()
                .forEach(k -> {
                    Assertion assertion = model.model.get(k.substring(0, 1)).get(k);
                    for (ArrayList<String> policy : policies.get(k)) {
                        assertion.policy.add(policy);
                        assertion.policyIndex.put(policy.toString(), assertion.policy.size() - 1);
                    }
        });
    }

    Map<String, ArrayList<ArrayList<String>>> loading() {
        FindIterable<CasbinRule> findAll = this.getCollection()
                .find();
        return StreamSupport.stream(findAll.spliterator(), false)
                .distinct()
                .map(CasbinRule::toPolicy)
                .collect(Collectors.toMap(
                        x -> x.get(0), y -> {
                            ArrayList<ArrayList<String>> lists = new ArrayList<>();
                            // 去除list第一项策略类型
                            y.remove(0);
                            lists.add(y);
                            return lists;
                        }, (oldValue, newValue) -> {
                            oldValue.addAll(newValue);
                            return oldValue;
                        })
                );
    }

    /**
     * 将所有策略规则保存到存储
     * 保存时会合并重复数据
     *
     * @param model the model.
     */
    @Override
    public void savePolicy(Model model) {
        this.clearCollection();
        List<CasbinRule> casbinRules = CasbinRule.transformToCasbinRule(model);
        this.getCollection().insertMany(casbinRules);
    }

    /**
     * 将策略规则添加到存储
     *
     * @param sec   the section, "p" or "g".
     * @param ptype the policy type, "p", "p2", .. or "g", "g2", ..
     * @param rule  the rule, like (sub, obj, act).
     */
    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        this.adding(sec, ptype, rule);
    }


    void adding(String sec, String ptype, List<String> rule) {
        ArrayList<String> rules = new ArrayList<>(rule);
        rules.add(0, ptype);
        for (int i = 0; i < 6 - rule.size(); i++) {
            rules.add("");
        }
        Optional<CasbinRule> casbinRule = fromListRule(rules);
        casbinRule.ifPresent(r -> this.getCollection().insertOne(r));
    }

    /**
     * 从存储中删除策略规则
     *
     * @param sec   the section, "p" or "g".
     * @param ptype the policy type, "p", "p2", .. or "g", "g2", ..
     * @param rule  the rule, like (sub, obj, act).
     */
    @Override
    public void removePolicy(String sec, String ptype, List<String> rule) {
        if (rule.isEmpty()) return;
        removeFilteredPolicy(sec, ptype, 0, rule.toArray(new String[0]));

    }

    void removing(String sec, String ptype, int fieldIndex, String... fieldValues) {
        if (fieldValues.length == 0) return;
        Document filter = new Document("ptype", ptype);
        int columnIndex = fieldIndex;
        for (String fieldValue : fieldValues) {
            if (CasbinRule.hasText(fieldValue)) filter.put("v" + columnIndex, fieldValue);
            columnIndex++;
        }
        this.getCollection().deleteOne(filter);

    }

    /**
     * 从存储中删除当前策略指定索引后匹配的数据
     *
     * @param sec         the section, "p" or "g".
     * @param ptype       the policy type, "p", "p2", .. or "g", "g2", ..
     * @param fieldIndex  the policy rule's start index to be matched.
     * @param fieldValues the field values to be matched, value ""
     */
    @Override
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        this.removing(sec, ptype, fieldIndex, fieldValues);
    }

    /**
     * addPolicies adds authorization rules to the current policy.
     *
     * @param sec         the section, "p" or "g".
     * @param ptype       the policy type, "p", "p2", .. or "g", "g2", ..
     * @param rules       the rules, like ((sub, obj, act), (sub, obj, act), ...).
     */
    @Override
    public void addPolicies(String sec, String ptype, List<List<String>> rules) {
        ArrayList<CasbinRule> rulesOfRules = new ArrayList<>(rules.size());
        for (List<String> rule : rules) {
            ArrayList<String> ruleClone = new ArrayList<>(rule);
            ruleClone.add(0, ptype);
            for (int i = 0; i < 6 - rule.size(); i++) {
                ruleClone.add("");
            }
            Optional<CasbinRule> casbinRule = fromListRule(ruleClone);
            casbinRule.ifPresent(rulesOfRules::add);
        }

        if(!rulesOfRules.isEmpty()) {
            this.getCollection().insertMany(rulesOfRules);
        }
    }

    /**
     * removePolicies removes authorization rules from the current policy.
     *
     * @param sec         the section, "p" or "g".
     * @param ptype       the policy type, "p", "p2", .. or "g", "g2", ..
     * @param rules       the rules, like ((sub, obj, act), (sub, obj, act), ...).
     */
    @Override
    public void removePolicies(String sec, String ptype, List<List<String>> rules) {
        ArrayList<DeleteOneModel<CasbinRule>> deleteRequests = new ArrayList<>();
        for (List<String> rule : rules) {
            if (rule.isEmpty()) continue;
            Document filter = new Document("ptype", ptype);
            int columnIndex = 0;
            for (String fieldValue : rule) {
                if (CasbinRule.hasText(fieldValue)) filter.put("v" + columnIndex, fieldValue);
                columnIndex++;
            }
            deleteRequests.add(new DeleteOneModel<>(filter));
        }

        if(!deleteRequests.isEmpty()) {
            this.getCollection().bulkWrite(deleteRequests);
        }
    }
}
