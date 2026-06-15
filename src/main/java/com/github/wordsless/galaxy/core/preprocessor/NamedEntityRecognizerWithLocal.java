package com.github.wordsless.galaxy.core.preprocessor;

import com.github.wordsless.galaxy.core.exception.NamedEntityRecognizeException;
import com.github.wordsless.galaxy.core.entity.Context;
import com.github.wordsless.galaxy.core.entity.Query;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedEntityRecognizerWithLocal implements NamedEntityRecognizer {

    private static final Map<String, String> ENTITY_TYPE_MAPPING;

    static {
        ENTITY_TYPE_MAPPING = new HashMap<>();
        // 人名
        ENTITY_TYPE_MAPPING.put("nr", "PER");
        ENTITY_TYPE_MAPPING.put("nrf", "PER");
        ENTITY_TYPE_MAPPING.put("nrj", "PER");
        ENTITY_TYPE_MAPPING.put("nrp", "PER");

        // 地名
        ENTITY_TYPE_MAPPING.put("ns", "LOC");
        ENTITY_TYPE_MAPPING.put("nsf", "LOC");
        ENTITY_TYPE_MAPPING.put("nsi", "LOC");

        // 机构、公司、组织
        ENTITY_TYPE_MAPPING.put("nt", "ORG");
        ENTITY_TYPE_MAPPING.put("ntc", "ORG");
        ENTITY_TYPE_MAPPING.put("ntd", "ORG");
        ENTITY_TYPE_MAPPING.put("ntg", "ORG");
        ENTITY_TYPE_MAPPING.put("nz", "ORG");
        ENTITY_TYPE_MAPPING.put("nw", "ORG");
    }

    @Override
    public void process(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context上下文不能为空");
        }

        Query rawQuery = context.getQuery();
        if (rawQuery == null || StringUtils.isBlank(rawQuery.getText())) {
            throw new IllegalArgumentException("查询语句不能为空");
        }

        String queryText = rawQuery.getText();

        try {
            // 分词
            List<Term> termList = StandardTokenizer.segment(queryText);

            // 解析实体
            List<Query.Entity> nerEntities = parseEntities(termList, queryText);

            // 回填
            rawQuery.setNERs(nerEntities);

        } catch (Exception e) {
            throw new NamedEntityRecognizeException("本地实体识别失败", e, 0, "");
        }
    }

    /**
     * 【完美修复】
     * 直接遍历每个词，判断词性，不依赖连续相同词性！
     */
    private List<Query.Entity> parseEntities(List<Term> terms, String queryText) {
        List<Query.Entity> entityList = new ArrayList<>();

        for (Term term : terms) {
            String word = term.word;
            String nature = term.nature.toString();

            if (!ENTITY_TYPE_MAPPING.containsKey(nature)) {
                continue;
            }

            // 正确计算位置
            int start = queryText.indexOf(word);
            if (start < 0) continue;

            int end = start + word.length() - 1;

            Query.Entity entity = new Query.Entity();
            entity.setText(word);
            entity.setType(ENTITY_TYPE_MAPPING.get(nature));
            entity.setStart(start);
            entity.setEnd(end);

            entityList.add(entity);
        }

        return entityList;
    }
}