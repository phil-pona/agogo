/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.property.control;

import ch.puzzle.itc.mobiliar.business.database.control.QueryUtils;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty.Origin;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 *  This class provides the interface to the native queries for property loading
 * 
 * @author oschmid
 */
public class PropertyEditingQueries {

    @Inject
    EntityManager entityManager;

    private static final String CONSUMEDRESRELTABLE = QueryUtils.getTable(ConsumedResourceRelationEntity.class);
    private static final String CONSUMEDRESRELFK = "CONSUMEDRESOURCERELATION_ID";
    private static final String PROVIDEDRESRELTABLE = QueryUtils.getTable(ProvidedResourceRelationEntity.class);
    private static final String PROVIDEDRESRELFK = "PROVIDEDRESOURCERELATION_ID";

    public Query getPropertyValueForResource(int resourceId, List<Integer> resourceTypeIds, List<Integer> contextIds) {
        String template = loadPropertyDescriptorQuery();
        Query query = entityManager.createNativeQuery(template);
        query.setParameter("loadedFor", Origin.INSTANCE.name()).setParameter("resourceId", resourceId).setParameter("resourceTypeIds", resourceTypeIds).setParameter("contextIds", contextIds).setParameter("resourceRelationId", -1).setParameter("masterResourceTypeIds", Arrays.asList(-1));
        return query;
    }

    public Query getPropertyValueForResourceType(List<Integer> resourceTypeIds, List<Integer> contextIds) {
        String template = loadSQLFile("loadPropertyDescriptorsForResourceType.sql");
        template = String.format(template, loadPropertyTypeValueQuery());
        Query query = entityManager.createNativeQuery(template);
        query.setParameter("loadedFor", Origin.TYPE.name()).setParameter("resourceTypeIds", resourceTypeIds).setParameter("contextIds", contextIds)
                  .setParameter("masterResourceTypeIds", Arrays.asList(-1))
        ;
        return query;
    }

    public Query getPropertyValueForConsumedRelationQuery(int resourceRelationId, int relatedResourceId, List<Integer> masterResourceTypeIds, List<Integer> relatedResourceTypeIds, List<Integer> contextIds) {
        String template = loadRelationPropertyDescriptorQuery(CONSUMEDRESRELTABLE, CONSUMEDRESRELFK);
        Query query = entityManager.createNativeQuery(template);
        query.setParameter("loadedFor", Origin.RELATION.name()).setParameter("resourceRelationId",
                  resourceRelationId).setParameter("resourceId", relatedResourceId).setParameter(
                  "resourceTypeIds", relatedResourceTypeIds).setParameter("contextIds", contextIds).setParameter("masterResourceTypeIds", masterResourceTypeIds);
        return query;
    }

    public Query getPropertyValueForProvidedRelationQuery(int resourceRelationId, int relatedResourceId, List<Integer> masterResourceTypeIds, List<Integer> relatedResourceTypeIds, List<Integer> contextIds) {
        String template = loadRelationPropertyDescriptorQuery(PROVIDEDRESRELTABLE, PROVIDEDRESRELFK);
        Query query = entityManager.createNativeQuery(template);
        query.setParameter("loadedFor", Origin.RELATION.name()).setParameter("resourceRelationId", resourceRelationId).setParameter("resourceId", relatedResourceId).setParameter("resourceTypeIds", relatedResourceTypeIds).setParameter("contextIds", contextIds).setParameter("masterResourceTypeIds", masterResourceTypeIds);
        return query;
    }

    public Query getPropertyValueForResourceTypeRelationQuery(List<Integer> masterResourceTypeIds, List<Integer> relatedResourceTypeIds, List<Integer> contextIds) {
        String template = loadSQLFile("loadPropertyDescriptorsForResourceType.sql");
        template = String.format(template, loadPropertyTypeValuesForRelation());
        Query query = entityManager.createNativeQuery(template);
        query.setParameter("loadedFor", Origin.TYPE_REL.name()).setParameter("resourceTypeIds",
                  relatedResourceTypeIds).setParameter("contextIds", contextIds).setParameter("masterResourceTypeIds", masterResourceTypeIds);
        return query;
    }

    private String loadPropertyValuesForRelation(String resourceRelationTableName, String resourceRelationFKName) {
        String propertyValues = loadPropertyValueQuery();
        String relationPropertyValues = loadRelationPropertyValueQuery(resourceRelationTableName,
                  resourceRelationFKName);
        return "(" + propertyValues + ") UNION ALL (" + relationPropertyValues + ")";
    }

    private String loadPropertyTypeValuesForRelation() {
        String propertyValues = loadPropertyTypeValueQuery();
        String relationPropertyValues = loadRelationPropertyTypeValueQuery();
        return "(" + propertyValues + ") UNION ALL (" + relationPropertyValues + ")";
    }

    private String loadPropertyValueQuery() {
        return loadSQLFile("loadPropertyValuesForResource.sql");
    }

    private String loadPropertyTypeValueQuery() {return loadSQLFile("loadPropertyValuesForResourceType.sql");
    }

    private String loadPropertyDescriptorQuery() {
        String template = loadSQLFile("loadPropertyDescriptorsForResource.sql");
        //We use consumed rel table as a default (for this case, no relation ids will be provided, therefore property descriptors on relations are not considered)
        template = String.format(template, CONSUMEDRESRELTABLE, CONSUMEDRESRELFK, loadPropertyValueQuery());
        return template;
    }


    private String loadRelationPropertyDescriptorQuery(String resourceRelationTableName, String resourceRelationFKName) {
        String template = loadSQLFile("loadPropertyDescriptorsForResource.sql");
        template = String.format(template, resourceRelationTableName, resourceRelationFKName,  loadPropertyValuesForRelation(resourceRelationTableName, resourceRelationFKName));
        return template;
    }

    private String loadRelationPropertyValueQuery(String resourceRelationTableName, String resourceRelationFKName) {
        String template = loadSQLFile("loadPropertyValuesForResourceRelation.sql");
        template = String.format(template, resourceRelationTableName, resourceRelationFKName);
        return template;
    }

    private String loadRelationPropertyTypeValueQuery() {
        return loadSQLFile("loadPropertyValuesForResourceTypeRelation.sql");
    }

    private String loadSQLFile(String file) {
        try {
            StringWriter sw = new StringWriter();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(file)));
                String line;
                while ((line = reader.readLine()) != null) {
                    //exclude comments
                    if (!line.trim().startsWith("--")) {
                        sw.append(line).append('\n');
                    }
                }
            } finally {
                sw.close();
                if (reader != null) {
                    reader.close();
                }
            }
			return sw.toString().trim();
        } catch (IOException e) {
            //if we have issues of reading the sql files of the current classpath, there is something completely wrong - we better throw a runtime exception
            throw new RuntimeException(e);
        }
    }

}
