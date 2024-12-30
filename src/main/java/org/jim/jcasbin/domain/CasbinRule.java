package org.jim.jcasbin.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.casbin.jcasbin.model.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 *
 * @author JimZhang
 * @since 2021/3/23
 * Description:
 */
@Getter
@Setter
public class CasbinRule {
    private ObjectId id;
    private String ptype;
    private String v0;
    private String v1;
    private String v2;
    private String v3;
    private String v4;
    private String v5;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CasbinRule that = (CasbinRule) o;

        if (ptype != null ? !ptype.equals(that.ptype) : that.ptype != null) return false;
        if (v0 != null ? !v0.equals(that.v0) : that.v0 != null) return false;
        if (v1 != null ? !v1.equals(that.v1) : that.v1 != null) return false;
        if (v2 != null ? !v2.equals(that.v2) : that.v2 != null) return false;
        if (v3 != null ? !v3.equals(that.v3) : that.v3 != null) return false;
        if (v4 != null ? !v4.equals(that.v4) : that.v4 != null) return false;
        return v5 != null ? v5.equals(that.v5) : that.v5 == null;
    }

    @Override
    public int hashCode() {
        int result = ptype != null ? ptype.hashCode() : 0;
        result = 31 * result + (v0 != null ? v0.hashCode() : 0);
        result = 31 * result + (v1 != null ? v1.hashCode() : 0);
        result = 31 * result + (v2 != null ? v2.hashCode() : 0);
        result = 31 * result + (v3 != null ? v3.hashCode() : 0);
        result = 31 * result + (v4 != null ? v4.hashCode() : 0);
        result = 31 * result + (v5 != null ? v5.hashCode() : 0);
        return result;
    }

    public static boolean hasText(String str) {
        return str != null && !str.isEmpty();
    }

    public void setByIndex(int i, String data) {
        switch (i) {
            case 0:
                this.ptype = data;
                break;
            case 1:
                this.v0 = data;
                break;
            case 2:
                this.v1 = data;
                break;
            case 3:
                this.v2 = data;
                break;
            case 4:
                this.v3 = data;
                break;
            case 5:
                this.v4 = data;
                break;
            case 6:
                this.v5 = data;
                break;
            default:

        }
    }

    public ArrayList<String> toPolicy() {
        ArrayList<String> policy = new ArrayList<>();
        policy.add(ptype);
        if (hasText(v0)) {
            policy.add(v0);
        }
        if (hasText(v1)) {
            policy.add(v1);
        }
        if (hasText(v2)) {
            policy.add(v2);
        }
        if (hasText(v3)) {
            policy.add(v3);
        }
        if (hasText(v4)) {
            policy.add(v4);
        }
        if (hasText(v5)) {
            policy.add(v5);
        }
        return policy;
    }

    /**
     * Converts the model into CasbinRule.
     * The conversion process will merge duplicate data.
     */
    public static List<CasbinRule> transformToCasbinRule(Model model) {
        Set<CasbinRule> casbinRules = new HashSet<>();
        model.model.values().forEach(x -> x.values().forEach(y -> y.policy.forEach(z -> {
            if (z.isEmpty()) return;
            int size = z.size();
            CasbinRule casbinRule = new CasbinRule();
            casbinRule.setPtype(y.key);
            casbinRule.setV0(z.get(0));
            if (size >= 2) {
                casbinRule.setV1(z.get(1));
            }
            if (size >= 3) {
                casbinRule.setV2(z.get(2));
            }
            if (size >= 4) {
                casbinRule.setV3(z.get(3));
            }
            if (size >= 5) {
                casbinRule.setV4(z.get(4));
            }
            if (size >= 6) {
                casbinRule.setV5(z.get(5));
            }
            casbinRules.add(casbinRule);
        })));
        return new ArrayList<>(casbinRules);
    }
}
