package com.side.hhplusecommerce.support;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseCleaner {

    private final List<String> tableNames = new ArrayList<>();

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    @PostConstruct
    private void findDatabaseTableNames() {
        List<Object> tableInfos = entityManager.createNativeQuery("SHOW TABLES").getResultList();
        for (Object tableInfo : tableInfos) {
            String tableName = (String) tableInfo;
            tableNames.add(tableName);
        }
    }

    private void truncate() {
        entityManager.createNativeQuery("SET foreign_key_checks = 0").executeUpdate();
        for (String tableName : tableNames) {
            entityManager
                    .createNativeQuery(String.format("TRUNCATE TABLE %s", tableName))
                    .executeUpdate();
        }
        entityManager.createNativeQuery("SET foreign_key_checks = 1").executeUpdate();
    }

    @Transactional
    public void clear() {
        entityManager.clear();
        truncate();
    }
}