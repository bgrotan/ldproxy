/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.admin.rest;

import de.ii.ldproxy.codelists.CodelistData;
import de.ii.xtraplatform.api.exceptions.BadRequest;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataStore;
import de.ii.xtraplatform.web.api.Endpoint;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Path("/admin/codelists/")
@Produces(MediaType.APPLICATION_JSON)
public class CodelistEndpoint implements Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistEndpoint.class);

    private final EntityDataStore<CodelistData> codelistRepository;

    CodelistEndpoint(@Requires EntityDataStore<EntityData> entityRepository) {
        this.codelistRepository = entityRepository.forType(CodelistData.class);
    }

    @GET
    @CacheControl(noCache = true)
    public List<String> getCodelists(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user*/) {
        return codelistRepository.ids();
    }

    @GET
    @Path("/{id}")
    @CacheControl(noCache = true)
    public CodelistData getCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/
            @PathParam("id") String id) {
        return codelistRepository.get(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @CacheControl(noCache = true)
    public CodelistData addCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/
            Map<String, Object> request) {

        if (!request.containsKey("id")) {
            throw new BadRequest("No id given");
        }

        String id = (String) request.get("id");

        if (codelistRepository.has(id)) {
            throw new BadRequest("A codelist with id '" + id + "' already exists");
        }

        try {
            //TODO: codelist generator
            CodelistData codelistData = null;// codelistRepository.generateEntity(request);
            //throw BadRequest

            CodelistData added = codelistRepository.put(id, codelistData)
                                                   .get();

            return added;
        } catch (InterruptedException | ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    @DELETE
    @Path("/{id}")
    @CacheControl(noCache = true)
    public void deleteCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/
            @PathParam("id") String id) throws IOException {
        codelistRepository.delete(id);
    }

}