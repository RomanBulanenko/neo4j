/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.rest;

import org.junit.Test;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AutoIndexerFunctionalTest extends AbstractRestFunctionalTestBase {

    /**
     * Get current status for autoindexing on nodes.
     */
    @Test
    @Documented
    public void getCurrentStatusForNodes() {
        checkAndAssertAutoIndexerIsEnabled("node", false);
    }

    /**
     * Enable node autoindexing.
     */
    @Test
    @Documented
    public void enableNodeAutoIndexing() {
        setEnabledAutoIndexingForType("node", true);
    }

    /**
     * Add a property for autoindexing on nodes.
     */
    @Test
    @Documented
    public void addAutoIndexingPropertyForNodes() {
        gen.get().expectedStatus(204).payload("myProperty1").post(getDataUri() + "autoindex/node/properties");
    }

    /**
     * Lookup list of properties being autoindexed.
     */
    @Test
    @Documented
    public void listAutoIndexingPropertiesForNodes() throws JsonParseException {
        List<String> properties = getAutoIndexedPropertiesForType("node");
        assertEquals(1, properties.size());
    }

    /**
     * Remove a property for autoindexing on nodes.
     */
    @Test
    @Documented
    public void removeAutoIndexingPropertyForNodes() {
        gen.get().expectedStatus(204).delete(getDataUri() + "autoindex/node/properties/myProperty1");
    }

    @Test
    public void switchOnOffAutoIndexingForNodes() {
        switchOnOffAutoIndexingForType("node");
    }

    @Test
    public void switchOnOffAutoIndexingForRelationships() {
        switchOnOffAutoIndexingForType("relationship");
    }

    @Test
    public void addRemoveAutoIndexedPropertyForNodes() throws JsonParseException {
        addRemoveAutoIndexedPropertyForType("node");
    }

    @Test
    public void addRemoveAutoIndexedPropertyForRelationships() throws JsonParseException {
        addRemoveAutoIndexedPropertyForType("relationship");
    }

    private void addRemoveAutoIndexedPropertyForType(String uriPartForType) throws JsonParseException {
//        List<String> properties = getAutoIndexedPropertiesForType(uriPartForType);
//        assertTrue(properties.isEmpty());

        gen.get().expectedStatus(204).payload("myProperty1").post(getDataUri() + "autoindex/" + uriPartForType + "/properties");
        gen.get().expectedStatus(204).payload("myProperty2").post(getDataUri() + "autoindex/" + uriPartForType + "/properties");

        List<String> properties = getAutoIndexedPropertiesForType(uriPartForType);
        assertEquals(2, properties.size());
        assertTrue(properties.contains("myProperty1"));
        assertTrue(properties.contains("myProperty2"));

        gen.get().expectedStatus(204).payload(null).delete(getDataUri() + "autoindex/" + uriPartForType + "/properties/myProperty2");

        properties = getAutoIndexedPropertiesForType(uriPartForType);
        assertEquals(1, properties.size());
        assertTrue(properties.contains("myProperty1"));
    }

    private List<String> getAutoIndexedPropertiesForType(String uriPartForType) throws JsonParseException {
        StringBuilder sb = new StringBuilder();
        sb.append(getDataUri())
                .append("autoindex/")
                .append(uriPartForType)
                .append("/properties");
        String result = gen.get().expectedStatus(200).get(sb.toString()).entity();
        return (List<String>) JsonHelper.readJson(result);
    }

    private void switchOnOffAutoIndexingForType(String uriPartForType) {
        setEnabledAutoIndexingForType(uriPartForType, true);
        checkAndAssertAutoIndexerIsEnabled(uriPartForType, true);
        setEnabledAutoIndexingForType(uriPartForType, false);
        checkAndAssertAutoIndexerIsEnabled(uriPartForType, false);
    }

    private void setEnabledAutoIndexingForType(String uriPartForType, boolean enabled) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDataUri())
                .append("autoindex/")
                .append(uriPartForType)
                .append("/status");
        gen.get().expectedStatus(204).payload(Boolean.toString(enabled)).put(sb.toString());
    }

    private void checkAndAssertAutoIndexerIsEnabled(String uriPartForType, boolean enabled) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDataUri())
                .append("autoindex/")
                .append(uriPartForType)
                .append("/status");
        String result = gen.get().expectedStatus(200).get(sb.toString()).entity();
        assertEquals(enabled, Boolean.parseBoolean(result));
    }

}