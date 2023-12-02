package org.cch;

import org.cch.dto.ClientDTO;
import org.cch.service.ClinetCertificate;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class CertificateResource {

    @Inject
    ClinetCertificate service;

    @POST
    public Response multipart( @RestForm("client") FileUpload file) {
        var clientDTO = new ClientDTO(file);
        boolean validateCertificate = service.validateCertificate(clientDTO);;
        if (validateCertificate) {
            return Response.ok().build();
        }

        return Response.status(Status.BAD_REQUEST).build();
    }
}
