package org.cch;

import java.util.Map;

import org.cch.dto.ClientDTO;
import org.cch.service.ClinetCertificate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class CertificateResource {

    @Inject
    ClinetCertificate service;

    @ConfigProperty(name = "greeting.message") 
    String message;


    @POST
    public Response multipart( @RestForm("client") FileUpload file) {
        var clientDTO = new ClientDTO(file);
        boolean validateCertificate = service.validateCertificate(clientDTO);;
        if (validateCertificate) {
            return Response.ok().build();
        }

        return Response.status(Status.BAD_REQUEST).build();
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response into() {
        return Response.ok(Map.of("message", this.message)).build();
    }
}
